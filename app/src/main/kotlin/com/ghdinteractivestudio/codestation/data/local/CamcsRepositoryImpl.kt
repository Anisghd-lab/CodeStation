package com.ghdinteractivestudio.codestation.data.local

import com.ghdinteractivestudio.codestation.domain.model.CamcsEvent
import com.ghdinteractivestudio.codestation.domain.model.CamcsMission
import com.ghdinteractivestudio.codestation.domain.model.CamcsMissionsResponse
import com.ghdinteractivestudio.codestation.domain.repository.CamcsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class CamcsRepositoryImpl(
    private val journalDao: CamcsJournalDao? = null
) : CamcsRepository {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE Stream infinite timeout
        .build()

    // Client REST borné (dérivé du client partagé : même pool de connexions) pour les requêtes courtes.
    private val restClient = httpClient.newBuilder()
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _allMissionsStream = MutableStateFlow<List<CamcsMission>>(emptyList())
    override val allMissionsStream: StateFlow<List<CamcsMission>> = _allMissionsStream.asStateFlow()

    private val _activeMissionStream = MutableStateFlow<CamcsMission?>(null)
    override val activeMissionStream: StateFlow<CamcsMission?> = _activeMissionStream.asStateFlow()

    private var lastCompletedMission: CamcsMission? = null
    private var retainCompletedUntilMs = 0L

    init {
        startPollingAndSse()
    }

    private fun startPollingAndSse() {
        // 1. Polling loop every 2 seconds
        scope.launch {
            while (isActive) {
                try {
                    refreshMissionsInternal()
                } catch (_: Exception) {}
                delay(2000)
            }
        }

        // 2. Continuous SSE Event stream for sub-second synchronization
        scope.launch {
            while (isActive) {
                try {
                    val sseReq = Request.Builder()
                        .url("http://127.0.0.1:19223/api/camcs/events")
                        .header("Accept", "text/event-stream")
                        .build()
                    val response = httpClient.newCall(sseReq).execute()
                    val source = response.body?.byteStream()
                    if (source != null) {
                        val reader = BufferedReader(InputStreamReader(source))
                        var line: String? = reader.readLine()
                        while (isActive && line != null) {
                            if (line.startsWith("data: ")) {
                                val dataJson = line.substring(6).trim()
                                try {
                                    val ev = json.decodeFromString<CamcsEvent>(dataJson)
                                    journalDao?.insertIgnore(listOf(CamcsJournalEntity.fromEvent(ev)))
                                    handleRealtimeEvent(ev, dataJson)
                                } catch (_: Exception) {}
                            }
                            line = reader.readLine()
                        }
                    }
                    response.close()
                } catch (_: Exception) {
                    delay(3000)
                }
            }
        }
    }

    /**
     * Requête REST de liste des missions (polling + Pull-to-Refresh du Master).
     * Le client partagé a un readTimeout infini (nécessaire au flux SSE) : sans délai borné ici,
     * un hôte qui accepte la connexion sans répondre bloquerait l'indicateur de rafraîchissement
     * et la boucle de polling indéfiniment.
     *
     * @return `true` si l'hôte a répondu 2xx et que la liste a été mise à jour.
     * @throws java.io.IOException en cas d'échec réseau (géré par les appelants).
     */
    private suspend fun refreshMissionsInternal(): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions").build()
        restClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use false
            val body = resp.body?.string() ?: ""
            val parsed = json.decodeFromString<CamcsMissionsResponse>(body)
            val sorted = parsed.missions.sortedByDescending { it.createdAt }
            _allMissionsStream.value = sorted
            updateActiveMissionFromList(sorted)
            true
        }
    }

    private fun updateActiveMissionFromList(missions: List<CamcsMission>) {
        val active = missions.firstOrNull {
            it.status in listOf(
                "WORKING", "RUNNING", "STREAMING", "PLANNING", "DISPATCHING",
                "STARTING", "QUEUED", "WAITING_FOR_AGENT", "WAITING_FOR_APPROVAL", "VALIDATING"
            )
        }

        if (active != null) {
            _activeMissionStream.value = active
            lastCompletedMission = null
            retainCompletedUntilMs = 0L
        } else {
            // Check if recently completed
            val now = System.currentTimeMillis()
            val justCompleted = missions.firstOrNull { it.status == "COMPLETED" && (now / 1000.0 - (it.completedAt ?: 0.0)) < 4.2 }
            if (justCompleted != null) {
                lastCompletedMission = justCompleted
                retainCompletedUntilMs = now + 4200L
                _activeMissionStream.value = justCompleted
            } else if (now < retainCompletedUntilMs && lastCompletedMission != null) {
                _activeMissionStream.value = lastCompletedMission
            } else {
                _activeMissionStream.value = null
            }
        }
    }

    private fun handleRealtimeEvent(ev: CamcsEvent, dataJson: String) {
        val currentList = _allMissionsStream.value
        val mid = ev.missionId ?: return

        try {
            val raw = json.parseToJsonElement(dataJson).jsonObject
            val payload = raw["payload"]?.jsonObject

            when (ev.type) {
                "mission.status_changed" -> {
                    val newStatus = (payload?.get("currentState") ?: payload?.get("status"))?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val prog = payload?.get("progress")?.let { (it as? JsonPrimitive)?.intOrNull }
                    val reason = payload?.get("reason")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    if (newStatus != null) {
                        val updated = currentList.map { m ->
                            if (m.id == mid) m.copy(
                                status = newStatus,
                                progress = prog ?: m.progress,
                                lastTransitionReason = reason ?: m.lastTransitionReason
                            ) else m
                        }
                        _allMissionsStream.value = updated
                        updateActiveMissionFromList(updated)
                    }
                }
                "mission.completed" -> {
                    val updated = currentList.map { m ->
                        if (m.id == mid) m.copy(status = "COMPLETED", progress = 100) else m
                    }
                    _allMissionsStream.value = updated
                    val completed = updated.firstOrNull { it.id == mid }
                    if (completed != null) {
                        lastCompletedMission = completed
                        retainCompletedUntilMs = System.currentTimeMillis() + 4200L
                        _activeMissionStream.value = completed
                    }
                }
                "mission.cancelled" -> {
                    val updated = currentList.map { m ->
                        if (m.id == mid) m.copy(
                            status = "CANCELLED",
                            lastTransitionReason = "Mission arrêtée par l'utilisateur",
                            lastTransitionTime = System.currentTimeMillis() / 1000.0
                        ) else m
                    }
                    _allMissionsStream.value = updated
                    updateActiveMissionFromList(updated)
                }
                "task.cancelled" -> {
                    val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val tId = payload?.get("taskId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val updated = currentList.map { m ->
                        if (m.id == mid) {
                            m.copy(agents = m.agents.map { ag ->
                                if (aId == null || ag.id == aId) {
                                    ag.copy(
                                        status = "CANCELLED",
                                        pid = null,
                                        tasks = ag.tasks.map { t ->
                                            if (tId == null || t.id == tId) t.copy(status = "CANCELLED", pid = null) else t
                                        }
                                    )
                                } else ag
                            })
                        } else m
                    }
                    _allMissionsStream.value = updated
                    updateActiveMissionFromList(updated)
                }
                "agent.status_changed" -> {
                    val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val aStatus = (payload?.get("currentState") ?: payload?.get("status"))?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val aProg = payload?.get("progress")?.let { (it as? JsonPrimitive)?.intOrNull }
                    val aSubtask = payload?.get("subtask")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val aPid = payload?.get("pid")?.let { (it as? JsonPrimitive)?.intOrNull }

                    if (aId != null && aStatus != null) {
                        val updated = currentList.map { m ->
                            if (m.id == mid) {
                                m.copy(agents = m.agents.map { ag ->
                                    if (ag.id == aId) {
                                        ag.copy(
                                            status = aStatus,
                                            progress = aProg ?: ag.progress,
                                            currentSubtask = aSubtask ?: ag.currentSubtask,
                                            pid = aPid ?: ag.pid
                                        )
                                    } else ag
                                })
                            } else m
                        }
                        _allMissionsStream.value = updated
                        updateActiveMissionFromList(updated)
                    }
                }
                "cli.started" -> {
                    val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val pid = payload?.get("pid")?.let { (it as? JsonPrimitive)?.intOrNull }
                    if (aId != null && pid != null) {
                        val updated = currentList.map { m ->
                            if (m.id == mid) {
                                m.copy(agents = m.agents.map { ag ->
                                    if (ag.id == aId) ag.copy(pid = pid, status = "WORKING") else ag
                                })
                            } else m
                        }
                        _allMissionsStream.value = updated
                        updateActiveMissionFromList(updated)
                    }
                }
                "cli.output" -> {
                    val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    val output = payload?.get("output")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    if (aId != null && output != null) {
                        val updated = currentList.map { m ->
                            if (m.id == mid) {
                                m.copy(agents = m.agents.map { ag ->
                                    if (ag.id == aId) ag.copy(lastOutput = output) else ag
                                })
                            } else m
                        }
                        _allMissionsStream.value = updated
                        updateActiveMissionFromList(updated)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override suspend fun refreshMissions(): Result<Unit> {
        return try {
            if (refreshMissionsInternal()) {
                Result.success(Unit)
            } else {
                Result.failure(java.io.IOException("Hôte CAMCS : réponse HTTP non valide"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Hôte CAMCS indisponible : le dernier état connu est conservé, l'erreur est
            // retournée à l'appelant (jamais propagée en exception : le refresh ne tue pas l'app).
            Result.failure(e)
        }
    }

    override suspend fun stopTask(missionId: String, taskId: String?, agentId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val taskPart = if (taskId != null) "\"taskId\": \"$taskId\", " else ""
            val agentPart = if (agentId != null) "\"agentId\": \"$agentId\", " else ""
            val jsonBody = "{$taskPart$agentPart\"missionId\": \"$missionId\"}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/tasks/stop")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            refreshMissionsInternal()
            success
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun pauseMission(missionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = "{\"missionId\": \"$missionId\"}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/missions/pause")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            refreshMissionsInternal()
            success
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun resumeMission(missionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = "{\"missionId\": \"$missionId\"}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/missions/resume")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            refreshMissionsInternal()
            success
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun cancelMission(missionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = "{\"missionId\": \"$missionId\"}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/missions/cancel")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            refreshMissionsInternal()
            success
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteMission(missionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = "{\"missionId\": \"$missionId\"}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/missions/delete")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            refreshMissionsInternal()
            success
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun clearPastMissions(onlyPast: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = "{\"onlyPast\": $onlyPast}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/missions/clear")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            refreshMissionsInternal()
            success
        } catch (_: Exception) {
            false
        }
    }
}
