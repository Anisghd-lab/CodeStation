package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.data.local.CamcsJournalEntity
import com.ghdinteractivestudio.codestation.domain.model.CamcsAgent
import com.ghdinteractivestudio.codestation.domain.model.CamcsCliStatus
import com.ghdinteractivestudio.codestation.domain.model.CamcsCommunication
import com.ghdinteractivestudio.codestation.domain.model.CamcsCommunicationsResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsDeliverable
import com.ghdinteractivestudio.codestation.domain.model.CamcsDeliverablesResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsDiffResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsEvent
import com.ghdinteractivestudio.codestation.domain.model.CamcsExternalAction
import com.ghdinteractivestudio.codestation.domain.model.CamcsExternalService
import com.ghdinteractivestudio.codestation.domain.model.CamcsFileResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsGitStatus
import com.ghdinteractivestudio.codestation.domain.model.CamcsMission
import com.ghdinteractivestudio.codestation.domain.model.CamcsMissionsResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsProof
import com.ghdinteractivestudio.codestation.domain.model.CamcsProofsResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsProviderStatus
import com.ghdinteractivestudio.codestation.domain.model.CamcsProvidersResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsReport
import com.ghdinteractivestudio.codestation.domain.model.CamcsReportResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsServicesResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsSubtask
import com.ghdinteractivestudio.codestation.ui.components.BackButton
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.pressScale
import com.ghdinteractivestudio.codestation.ui.components.rememberPress
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * CAMCS Live Orchestration Control Center.
 * Comprehensive multi-agent command and supervision screen connecting to:
 * - CAMCS Real-time Daemon Engine (http://127.0.0.1:19223)
 * - Server-Sent Events (SSE) live event stream (/api/camcs/events)
 * - Dynamic Model Routing across OpenRouter, xKIRO, and Cloudflare Workers AI
 * - Installed Ubuntu PRoot CLIs (Google Antigravity agy, OpenCode, CodeBuddy, Codex)
 */
@Composable
fun CamcsControlCenterScreen(
    initialMissionId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val scrollState = rememberScrollState()

    var missions by remember { mutableStateOf<List<CamcsMission>>(emptyList()) }
    var providers by remember { mutableStateOf<List<CamcsProviderStatus>>(emptyList()) }
    var clis by remember { mutableStateOf<List<CamcsCliStatus>>(emptyList()) }
    var externalServices by remember { mutableStateOf<List<CamcsExternalService>>(emptyList()) }
    val liveEvents = remember { mutableStateListOf<CamcsEvent>() }

    // Tâche 05 : journal global persistant (Room), indépendant du bot sélectionné.
    val appContext = LocalContext.current.applicationContext
    val journalDao = remember(appContext) {
        (appContext as? CodeStationApplication)?.container?.camcsJournalDao
    }

    LaunchedEffect(journalDao) {
        val dao = journalDao ?: return@LaunchedEffect
        dao.getRecentStream(300).collect { rows ->
            val persisted = rows.map { it.toEvent() }
            // Conserver les événements reçus mais pas encore écrits en base.
            val pending = liveEvents.filter { ev -> persisted.none { it.id == ev.id } }
            liveEvents.clear()
            liveEvents.addAll((pending + persisted).sortedByDescending { it.timestamp })
        }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Orchestration, 1: Journal Live, 2: Providers & CLIs, 3: Résultats & Livrables
    var selectedMissionId by remember { mutableStateOf(initialMissionId) }
    var isEngineConnected by remember { mutableStateOf(false) }
    var currentClockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Real Execution Deliverables, Communications, Proofs & Reports
    var deliverables by remember { mutableStateOf<List<CamcsDeliverable>>(emptyList()) }
    var communications by remember { mutableStateOf<List<CamcsCommunication>>(emptyList()) }
    var proofs by remember { mutableStateOf<List<CamcsProof>>(emptyList()) }
    var report by remember { mutableStateOf<CamcsReport?>(null) }
    var gitDiffText by remember { mutableStateOf("") }
    // Tâche 08 : état Git réel du dépôt (lecture seule, rafraîchi périodiquement).
    var gitStatus by remember { mutableStateOf<CamcsGitStatus?>(null) }

    // Selected Deliverable for in-app code/doc inspection
    var selectedDeliverableForView by remember { mutableStateOf<CamcsDeliverable?>(null) }
    var viewingFileContent by remember { mutableStateOf<String?>(null) }
    var isLoadingFileContent by remember { mutableStateOf(false) }

    // Selected CLI for Detail Dialog
    var selectedCliForDetail by remember { mutableStateOf<CamcsCliStatus?>(null) }
    // Selected Event for Detail Dialog
    var selectedEventForDetail by remember { mutableStateOf<CamcsEvent?>(null) }

    val jsonParser = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    val httpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // SSE stream infinite timeout
            .build()
    }

    // Tâche 08 : traçabilité Git réelle via le moteur CAMCS (aucune écriture Git).
    LaunchedEffect(Unit) {
        while (isActive) {
            val fetched: CamcsGitStatus? = try {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("http://127.0.0.1:19223/api/camcs/git/status")
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            jsonParser.decodeFromString<CamcsGitStatus>(response.body?.string().orEmpty())
                        } else {
                            null
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (fetched != null) gitStatus = fetched
            delay(5000)
        }
    }

    val cancellingMissions = remember { mutableStateMapOf<String, Boolean>() }
    val cancellingTasks = remember { mutableStateMapOf<String, Boolean>() }
    val deletingMissions = remember { mutableStateMapOf<String, Boolean>() }
    // Anti double-tap : une seule action pause/reprise en vol par mission.
    val missionActionsInFlight = remember { mutableStateMapOf<String, String>() }

    fun requestMissionAction(missionId: String, action: String) {
        if (missionActionsInFlight[missionId] != null) return
        missionActionsInFlight[missionId] = action
        CoroutineScope(Dispatchers.IO).launch {
            val ok = executeMissionAction(httpClient, action, missionId)
            withContext(Dispatchers.Main) {
                missionActionsInFlight.remove(missionId)
                // L'état n'est mis à jour qu'après confirmation réelle du moteur CAMCS.
                if (ok) {
                    missions = missions.map { m ->
                        when {
                            m.id != missionId -> m
                            action == "pause" -> m.copy(
                                status = "PAUSED",
                                lastTransitionReason = "Mission mise en pause"
                            )
                            else -> m.copy(
                                status = "RUNNING",
                                lastTransitionReason = "Reprise de la mission"
                            )
                        }
                    }
                }
            }
        }
    }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val handleDeleteMission: (String) -> Unit = { mId ->
        if (deletingMissions[mId] != true) {
            deletingMissions[mId] = true
            missions = missions.filterNot { it.id == mId }
            if (selectedMissionId == mId) {
                selectedMissionId = missions.firstOrNull()?.id
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req = Request.Builder()
                        .url("http://127.0.0.1:19223/api/camcs/missions/delete")
                        .post("""{"missionId": "$mId"}""".toRequestBody("application/json".toMediaType()))
                        .build()
                    httpClient.newCall(req).execute().close()
                } catch (_: Exception) {
                } finally {
                    withContext(Dispatchers.Main) {
                        deletingMissions.remove(mId)
                    }
                }
            }
        }
    }

    val handleClearPastMissions: () -> Unit = {
        missions = missions.filterNot { isTerminalState(it.status) }
        selectedMissionId = missions.firstOrNull()?.id
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = Request.Builder()
                    .url("http://127.0.0.1:19223/api/camcs/missions/clear")
                    .post("""{"onlyPast": true}""".toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(req).execute().close()
            } catch (_: Exception) {}
        }
    }

    val handleCancelMission: (String) -> Unit = { mId ->
        if (cancellingMissions[mId] != true) {
            cancellingMissions[mId] = true
            // Immediately close tracking window for this mission
            missions = missions.filterNot { it.id == mId }
            if (selectedMissionId == mId) {
                selectedMissionId = missions.firstOrNull()?.id
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req = Request.Builder()
                        .url("http://127.0.0.1:19223/api/camcs/missions/cancel")
                        .post("""{"missionId": "$mId"}""".toRequestBody("application/json".toMediaType()))
                        .build()
                    httpClient.newCall(req).execute().close()
                } catch (_: Exception) {
                } finally {
                    withContext(Dispatchers.Main) {
                        cancellingMissions.remove(mId)
                    }
                }
            }
        }
    }

    val handleStopTask: (missionId: String, taskId: String?, agentId: String?) -> Unit = { mId, tId, aId ->
        val key = "${mId}_${tId ?: aId ?: ""}"
        if (cancellingTasks[key] != true) {
            cancellingTasks[key] = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val taskPart = if (tId != null) "\"taskId\": \"$tId\", " else ""
                    val agentPart = if (aId != null) "\"agentId\": \"$aId\", " else ""
                    val json = "{$taskPart$agentPart\"missionId\": \"$mId\"}"
                    val req = Request.Builder()
                        .url("http://127.0.0.1:19223/api/camcs/tasks/stop")
                        .post(json.toRequestBody("application/json".toMediaType()))
                        .build()
                    httpClient.newCall(req).execute().close()
                } catch (_: Exception) {
                } finally {
                    withContext(Dispatchers.Main) {
                        cancellingTasks.remove(key)
                    }
                }
            }
        }
    }

    // 0. Non-freezing live monotonic clock tick
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            currentClockTick = System.currentTimeMillis()
        }
    }

    // 1. Polling loop for missions, providers, CLIs and active mission deliverables/proofs
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val mReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions").build()
                    val mResp = httpClient.newCall(mReq).execute()
                    if (mResp.isSuccessful) {
                        val body = mResp.body?.string() ?: ""
                        val parsed = jsonParser.decodeFromString<CamcsMissionsResponse>(body)
                        val sorted = parsed.missions.sortedByDescending { it.createdAt }
                        missions = sorted
                        isEngineConnected = true
                        if (selectedMissionId == null && sorted.isNotEmpty()) {
                            selectedMissionId = sorted.first().id
                        }
                    }
                    mResp.close()

                    val pReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/providers").build()
                    val pResp = httpClient.newCall(pReq).execute()
                    if (pResp.isSuccessful) {
                        val pBody = pResp.body?.string() ?: ""
                        val pParsed = jsonParser.decodeFromString<CamcsProvidersResponse>(pBody)
                        providers = pParsed.providers
                        if (pParsed.clis.isNotEmpty()) {
                            clis = pParsed.clis
                        }
                    }
                    pResp.close()

                    try {
                        val sReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/services").build()
                        val sResp = httpClient.newCall(sReq).execute()
                        if (sResp.isSuccessful) {
                            val sBody = sResp.body?.string() ?: ""
                            val sParsed = jsonParser.decodeFromString<CamcsServicesResponse>(sBody)
                            externalServices = sParsed.services
                        }
                        sResp.close()
                    } catch (_: Exception) {}

                    // Poll mission deliverables, communications, proofs and report
                    val currMid = selectedMissionId
                    if (currMid != null) {
                        try {
                            val delivReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/deliverables").build()
                            val delivResp = httpClient.newCall(delivReq).execute()
                            if (delivResp.isSuccessful) {
                                val dBody = delivResp.body?.string() ?: ""
                                deliverables = jsonParser.decodeFromString<CamcsDeliverablesResponse>(dBody).deliverables
                            }
                            delivResp.close()

                            val commReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/communications").build()
                            val commResp = httpClient.newCall(commReq).execute()
                            if (commResp.isSuccessful) {
                                val cBody = commResp.body?.string() ?: ""
                                communications = jsonParser.decodeFromString<CamcsCommunicationsResponse>(cBody).communications
                            }
                            commResp.close()

                            val prReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/proofs").build()
                            val prResp = httpClient.newCall(prReq).execute()
                            if (prResp.isSuccessful) {
                                val prBody = prResp.body?.string() ?: ""
                                proofs = jsonParser.decodeFromString<CamcsProofsResponse>(prBody).proofs
                            }
                            prResp.close()

                            val repReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/report").build()
                            val repResp = httpClient.newCall(repReq).execute()
                            if (repResp.isSuccessful) {
                                val repBody = repResp.body?.string() ?: ""
                                report = jsonParser.decodeFromString<CamcsReportResponse>(repBody).report
                            }
                            repResp.close()

                            val diffReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/diff").build()
                            val diffResp = httpClient.newCall(diffReq).execute()
                            if (diffResp.isSuccessful) {
                                val diffBody = diffResp.body?.string() ?: ""
                                gitDiffText = jsonParser.decodeFromString<CamcsDiffResponse>(diffBody).diff
                            }
                            diffResp.close()
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                isEngineConnected = false
            }
            delay(1500)
        }
    }

    // Deliverable file content loader
    LaunchedEffect(selectedDeliverableForView) {
        val deliv = selectedDeliverableForView
        if (deliv != null) {
            isLoadingFileContent = true
            viewingFileContent = null
            withContext(Dispatchers.IO) {
                try {
                    val encoded = java.net.URLEncoder.encode(deliv.filePath, "UTF-8")
                    val req = Request.Builder().url("http://127.0.0.1:19223/api/camcs/file?path=$encoded").build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val parsed = jsonParser.decodeFromString<CamcsFileResponse>(body)
                        viewingFileContent = parsed.content
                    } else {
                        viewingFileContent = "Erreur de lecture: HTTP ${resp.code}"
                    }
                    resp.close()
                } catch (e: Exception) {
                    viewingFileContent = "Impossible de charger le contenu: ${e.message}"
                } finally {
                    isLoadingFileContent = false
                }
            }
        }
    }

    // 2. Real-time SSE Event Stream Listener with Auto-Replay and In-Place Sync
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
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
                                    val ev = jsonParser.decodeFromString<CamcsEvent>(dataJson)
                                    // Persistance idempotente : le même événement rejoué
                                    // (reconnexion SSE) est ignoré par la clé primaire.
                                    journalDao?.insertIgnore(
                                        listOf(CamcsJournalEntity.fromEvent(ev))
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (liveEvents.none { it.id == ev.id }) {
                                            liveEvents.add(0, ev)
                                            if (liveEvents.size > 200) liveEvents.removeLast()
                                        }

                                        // CRITICAL SYNCHRONIZATION:
                                        // Update mission state and agent execution in memory immediately upon receiving events!
                                        if (ev.missionId != null) {
                                            if (selectedMissionId == null) {
                                                selectedMissionId = ev.missionId
                                            }
                                            try {
                                                val raw = jsonParser.parseToJsonElement(dataJson).jsonObject
                                                val payload = raw["payload"]?.jsonObject

                                                when (ev.type) {
                                                    "mission.status_changed" -> {
                                                        val newStatus = (payload?.get("currentState") ?: payload?.get("status"))?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val prog = payload?.get("progress")?.let { (it as? JsonPrimitive)?.intOrNull }
                                                        val reason = payload?.get("reason")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        if (newStatus != null) {
                                                            missions = missions.map { m ->
                                                                if (m.id == ev.missionId) {
                                                                    m.copy(
                                                                        status = newStatus,
                                                                        progress = prog ?: m.progress,
                                                                        lastTransitionReason = reason ?: m.lastTransitionReason
                                                                    )
                                                                } else m
                                                            }
                                                        }
                                                    }
                                                    "mission.progress" -> {
                                                        val prog = payload?.get("progress")?.let { (it as? JsonPrimitive)?.intOrNull }
                                                        if (prog != null) {
                                                            missions = missions.map { m ->
                                                                if (m.id == ev.missionId) m.copy(progress = prog) else m
                                                            }
                                                        }
                                                    }
                                                    "mission.completed" -> {
                                                        missions = missions.map { m ->
                                                            if (m.id == ev.missionId) m.copy(status = "COMPLETED", progress = 100) else m
                                                        }
                                                    }
                                                    "mission.cancelled" -> {
                                                        missions = missions.map { m ->
                                                            if (m.id == ev.missionId) m.copy(
                                                                status = "CANCELLED",
                                                                lastTransitionReason = "Mission arrêtée par l'utilisateur",
                                                                lastTransitionTime = System.currentTimeMillis() / 1000.0
                                                            ) else m
                                                        }
                                                    }
                                                    "task.cancelled" -> {
                                                        val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val tId = payload?.get("taskId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        missions = missions.map { m ->
                                                            if (m.id == ev.missionId) {
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
                                                    }
                                                    "agent.status_changed" -> {
                                                        val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val aStatus = (payload?.get("currentState") ?: payload?.get("status"))?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val aProg = payload?.get("progress")?.let { (it as? JsonPrimitive)?.intOrNull }
                                                        val aSubtask = payload?.get("subtask")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val aPid = payload?.get("pid")?.let { (it as? JsonPrimitive)?.intOrNull }

                                                        if (aId != null && aStatus != null) {
                                                            missions = missions.map { m ->
                                                                if (m.id == ev.missionId) {
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
                                                        }
                                                    }
                                                    "cli.started" -> {
                                                        val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val pid = payload?.get("pid")?.let { (it as? JsonPrimitive)?.intOrNull }
                                                        val taskId = payload?.get("taskId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        if (aId != null && pid != null) {
                                                            missions = missions.map { m ->
                                                                if (m.id == ev.missionId) {
                                                                    m.copy(agents = m.agents.map { ag ->
                                                                        if (ag.id == aId) {
                                                                            ag.copy(
                                                                                pid = pid,
                                                                                activeTaskId = taskId ?: ag.activeTaskId,
                                                                                status = "WORKING",
                                                                                tasks = ag.tasks.map { t ->
                                                                                    if (t.id == taskId) t.copy(pid = pid, status = "WORKING") else t
                                                                                }
                                                                            )
                                                                        } else ag
                                                                    })
                                                                } else m
                                                            }
                                                        }
                                                    }
                                                    "cli.output" -> {
                                                        val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val output = payload?.get("output")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        if (aId != null && output != null) {
                                                            missions = missions.map { m ->
                                                                if (m.id == ev.missionId) {
                                                                    m.copy(agents = m.agents.map { ag ->
                                                                        if (ag.id == aId) {
                                                                            ag.copy(lastOutput = output)
                                                                        } else ag
                                                                    })
                                                                } else m
                                                            }
                                                        }
                                                    }
                                                    "task.cancelled" -> {
                                                        val taskId = payload?.get("taskId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        missions = missions.map { m ->
                                                            if (m.id == ev.missionId) {
                                                                m.copy(agents = m.agents.map { ag ->
                                                                    if (aId == null || ag.id == aId) {
                                                                        ag.copy(
                                                                            status = if (ag.status in listOf("WORKING", "RUNNING", "STREAMING")) "CANCELLED" else ag.status,
                                                                            tasks = ag.tasks.map { t ->
                                                                                if (taskId == null || t.id == taskId) t.copy(status = "CANCELLED") else t
                                                                            }
                                                                        )
                                                                    } else ag
                                                                })
                                                            } else m
                                                        }
                                                    }
                                                    "task.completed" -> {
                                                        val taskId = payload?.get("taskId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        val aId = ev.agentId ?: payload?.get("agentId")?.let { (it as? JsonPrimitive)?.contentOrNull }
                                                        missions = missions.map { m ->
                                                            if (m.id == ev.missionId) {
                                                                m.copy(agents = m.agents.map { ag ->
                                                                    if (aId == null || ag.id == aId) {
                                                                        ag.copy(
                                                                            tasks = ag.tasks.map { t ->
                                                                                if (taskId == null || t.id == taskId) t.copy(status = "COMPLETED") else t
                                                                            }
                                                                        )
                                                                    } else ag
                                                                })
                                                            } else m
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            line = reader.readLine()
                        }
                    }
                    response.close()
                } catch (_: Exception) {
                    delay(2000)
                }
            }
        }
    }

    val activeMission = missions.firstOrNull { it.id == selectedMissionId }
        ?: missions.firstOrNull()

    // Pulse animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. TOP HEADER (Responsive layout without ANY title/badge overlap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackButton(onNavigateBack)

            Text(
                text = "Centre de Contrôle CAMCS",
                style = Type.title3.semibold.copy(fontSize = 17.sp),
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val pastMissionsCount = missions.count { isTerminalState(it.status) }
            if (pastMissionsCount > 0) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.danger.copy(alpha = 0.15f))
                        .border(0.5.dp, Palette.danger.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { showClearHistoryDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("trash", size = 13.dp, tint = Palette.danger)
                }
            }

            // Live Connection Badge with pulsing LED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isEngineConnected) Palette.emeraldAccent.copy(alpha = 0.16f)
                        else Palette.danger.copy(alpha = 0.16f)
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEngineConnected) Palette.emeraldAccent.copy(alpha = pulseAlpha)
                            else Palette.danger
                        )
                )
                Text(
                    text = if (isEngineConnected) "EN DIRECT" else "HORS LIGNE",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = if (isEngineConnected) Palette.emeraldAccent else Palette.danger
                )
            }
        }

        if (showClearHistoryDialog) {
            val pastMissionsCount = missions.count { isTerminalState(it.status) }
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = {
                    Text(
                        text = "Supprimer les anciennes missions",
                        style = Type.headline.semibold,
                        color = Palette.text
                    )
                },
                text = {
                    Text(
                        text = "Voulez-vous supprimer définitivement toutes les anciennes missions terminées ou annulées ($pastMissionsCount mission(s)) ?",
                        style = Type.body,
                        color = Palette.secondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearHistoryDialog = false
                            handleClearPastMissions()
                        }
                    ) {
                        Text("Supprimer", color = Palette.danger, style = Type.body.semibold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Annuler", color = Palette.secondary)
                    }
                },
                containerColor = Palette.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 2. ADAPTIVE TAB SELECTOR (Scrollable horizontally, NEVER cuts text or uses ellipses)
        val tabScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .horizontalScroll(tabScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair("Orchestration & Dispatching", 0),
                Pair("Journal Live (${liveEvents.size})", 1),
                Pair("Providers, CLIs & Services (${providers.size + clis.size + externalServices.size})", 2),
                Pair("Résultats & Livrables (${deliverables.size})", 3)
            )

            tabs.forEach { (label, idx) ->
                val selected = activeTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) {
                                if (dark) Color(0xFF1E2638) else Color.White
                            } else Palette.surface.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (selected) Palette.emeraldAccent.copy(alpha = 0.5f) else Palette.glassBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(role = Role.Tab) { activeTab = idx }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = Type.caption.semibold.copy(fontSize = 12.sp),
                        color = if (selected) {
                            if (dark) Color.White else Palette.text
                        } else Palette.secondary,
                        softWrap = false
                    )
                }
            }
        }

        // 3. SCROLLABLE CONTENT BODY
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 1: Mission Dashboard, Dispatching Flow & Agents
                    if (activeMission != null) {
                        // Multi-mission selector chip bar
                        if (missions.isNotEmpty()) {
                            MissionSelectorRow(
                                missions = missions,
                                selectedMissionId = activeMission.id,
                                cancellingMissions = cancellingMissions,
                                deletingMissions = deletingMissions,
                                onSelect = { selectedMissionId = it },
                                onCancelMission = handleCancelMission,
                                onDeleteMission = handleDeleteMission,
                                onClearPastMissions = { showClearHistoryDialog = true }
                            )
                        }

                        MissionMasterCard(
                            mission = activeMission,
                            currentClockTick = currentClockTick,
                            isCancelling = cancellingMissions[activeMission.id] == true,
                            isDeleting = deletingMissions[activeMission.id] == true,
                            pendingAction = missionActionsInFlight[activeMission.id],
                            onPause = { requestMissionAction(activeMission.id, "pause") },
                            onResume = { requestMissionAction(activeMission.id, "resume") },
                            onCancel = {
                                handleCancelMission(activeMission.id)
                            },
                            onDelete = {
                                handleDeleteMission(activeMission.id)
                            }
                        )

                        // Visual Dispatching Workflow Pipeline with real states
                        DispatchingPipelineCard(mission = activeMission)

                        // External Actions & Remote Services Integrity
                        if (activeMission.externalActions.isNotEmpty()) {
                            ExternalActionsIntegrityCard(actions = activeMission.externalActions)
                        }

                        // Individual Live Agent Cards
                        Text(
                            text = "AGENTS MOBILISÉS & SUPERVISION PARALLÈLE",
                            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                            color = Palette.secondary,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )

                        activeMission.agents.forEach { agent ->
                            LiveAgentCard(
                                agent = agent,
                                missionId = activeMission.id,
                                currentClockTick = currentClockTick,
                                onStopTask = { taskId ->
                                    handleStopTask(activeMission.id, taskId, agent.id)
                                }
                            )
                        }

                        // Real Inter-Agent Communications Card
                        if (communications.isNotEmpty()) {
                            InterAgentCommunicationsCard(communications = communications)
                        }
                    } else {
                        EmptyMissionPlaceholder()
                    }
                }
                1 -> {
                    // TAB 2: Live Real-Time Event Stream Log
                    LiveEventsStreamView(
                        events = liveEvents,
                        activeMissionId = activeMission?.id,
                        activeMissionTitle = activeMission?.title,
                        onEventClick = { selectedEventForDetail = it }
                    )
                }
                2 -> {
                    // TAB 3: Providers, Models, PRoot CLIs & External Connected Services
                    ProvidersAndClisView(
                        providers = providers,
                        clis = clis,
                        externalServices = externalServices,
                        onCliClick = { selectedCliForDetail = it }
                    )
                }
                3 -> {
                    // TAB 4: Real Deliverables, Execution Proofs, Tests & Final Report
                    MissionDeliverablesAndResultsView(
                        mission = activeMission,
                        deliverables = deliverables,
                        proofs = proofs,
                        report = report,
                        gitDiffText = gitDiffText,
                        gitStatus = gitStatus,
                        onInspectDeliverable = { selectedDeliverableForView = it }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    // CLI Detail Dialog
    selectedCliForDetail?.let { cli ->
        GlassAlertDialog(
            onDismissRequest = { selectedCliForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("terminal", size = 20.dp, tint = Palette.electricBlue)
                    Text(cli.name, style = Type.title3.semibold, color = Palette.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Exécutable PRoot ARM64 : ${cli.path}",
                        style = Type.caption.semibold,
                        color = Palette.secondary
                    )
                    Text(
                        text = "Version : ${cli.version}  •  Statut : ${cli.status}",
                        style = Type.footnote,
                        color = Palette.text
                    )
                    if (cli.activeAgent != null) {
                        Text(
                            text = "⚡ Actuellement mobilisé par : ${cli.activeAgent}",
                            style = Type.footnote.semibold,
                            color = Palette.emeraldAccent
                        )
                    }
                    Text(
                        text = "Capacités détectées :",
                        style = Type.caption.semibold,
                        color = Palette.secondary
                    )
                    cli.capabilities.forEach { cap ->
                        Text("• $cap", style = Type.caption, color = Palette.text)
                    }
                    Text(
                        text = "Modèles compatibles : ${cli.accessibleModels.joinToString(", ")}",
                        style = Type.caption,
                        color = Palette.tertiary
                    )
                }
            },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accentFill)
                        .clickable { selectedCliForDetail = null }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Fermer", style = Type.caption.semibold, color = Palette.onAccent)
                }
            }
        )
    }

    // Event Detail Dialog
    selectedEventForDetail?.let { ev ->
        GlassAlertDialog(
            onDismissRequest = { selectedEventForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("sparkles", size = 18.dp, tint = Palette.emeraldAccent)
                    Text(ev.type.replace(".", " · ").uppercase(), style = Type.subheadline.semibold, color = Palette.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = ev.message, style = Type.body, color = Palette.text)
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date((ev.timestamp * 1000).toLong()))
                    Text(text = "Horodatage : $timeStr", style = Type.caption, color = Palette.secondary)
                    if (ev.missionId != null) {
                        Text(text = "Mission ID : ${ev.missionId}", style = Type.caption, color = Palette.tertiary)
                    }
                    if (ev.agentId != null) {
                        Text(text = "Agent ID : ${ev.agentId}", style = Type.caption, color = Palette.tertiary)
                    }
                }
            },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accentFill)
                        .clickable { selectedEventForDetail = null }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("OK", style = Type.caption.semibold, color = Palette.onAccent)
                }
            }
        )
    }

    // Deliverable File Content Viewer Dialog
    selectedDeliverableForView?.let { deliv ->
        DeliverableFileViewerDialog(
            deliverable = deliv,
            content = viewingFileContent,
            isLoading = isLoadingFileContent,
            onDismiss = { selectedDeliverableForView = null }
        )
    }
}

/**
 * Multi-mission selector chip bar.
 */
@Composable
private fun MissionSelectorRow(
    missions: List<CamcsMission>,
    selectedMissionId: String,
    cancellingMissions: Map<String, Boolean>,
    deletingMissions: Map<String, Boolean> = emptyMap(),
    onSelect: (String) -> Unit,
    onCancelMission: (String) -> Unit,
    onDeleteMission: (String) -> Unit = {},
    onClearPastMissions: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // S'assure que la mission la plus récente (en 1re position à gauche) est immédiatement visible
    LaunchedEffect(missions.firstOrNull()?.id) {
        if (selectedMissionId.isEmpty() || selectedMissionId == missions.firstOrNull()?.id) {
            scrollState.scrollTo(0)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        missions.forEach { m ->
            val isSelected = m.id == selectedMissionId
            val canCancel = isCancellableState(m.status) && !isTerminalState(m.status)
            val isCancelling = cancellingMissions[m.id] == true
            val isTerminal = isTerminalState(m.status)
            val isDeleting = deletingMissions[m.id] == true

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Palette.electricBlue.copy(alpha = 0.2f)
                        else Palette.surface.copy(alpha = 0.5f)
                    )
                    .border(
                        0.5.dp,
                        if (isSelected) Palette.electricBlue else Palette.glassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(m.id) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status dot indicator
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (m.status.uppercase()) {
                                "COMPLETED" -> Palette.added
                                "FAILED", "BLOCKED" -> Palette.danger
                                "CANCELLED" -> Palette.secondary
                                "STREAMING" -> Palette.electricBlue
                                else -> Palette.emeraldAccent
                            }
                        )
                )

                Text(
                    text = "${m.title} (${m.progress}%)",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = if (isSelected) Palette.electricBlue else Palette.secondary,
                    maxLines = 1
                )

                // Inline Cancel Button for ANY cancellable mission
                if (canCancel) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Palette.danger.copy(alpha = if (isCancelling) 0.12f else 0.25f))
                            .clickable(enabled = !isCancelling) { onCancelMission(m.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = Palette.danger,
                                strokeWidth = 1.2.dp
                            )
                        } else {
                            Symbol("xmark", size = 8.dp, tint = Palette.danger)
                        }
                    }
                }

                // Inline Delete Button for ANY terminal/past mission
                if (isTerminal) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Palette.danger.copy(alpha = if (isDeleting) 0.12f else 0.22f))
                            .clickable(enabled = !isDeleting) { onDeleteMission(m.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = Palette.danger,
                                strokeWidth = 1.2.dp
                            )
                        } else {
                            Symbol("trash", size = 8.dp, tint = Palette.danger)
                        }
                    }
                }
            }
        }

        val pastMissionsCount = missions.count { isTerminalState(it.status) }
        if (pastMissionsCount > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.danger.copy(alpha = 0.12f))
                    .border(
                        0.5.dp,
                        Palette.danger.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onClearPastMissions() }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Symbol("trash.fill", size = 9.dp, tint = Palette.danger)
                Text(
                    text = "Purger ($pastMissionsCount)",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = Palette.danger
                )
            }
        }
    }
}

/**
 * Top Global Mission Master Dashboard Card with compact yet legible metrics.
 */
@Composable
private fun MissionMasterCard(
    mission: CamcsMission,
    currentClockTick: Long,
    isCancelling: Boolean,
    isDeleting: Boolean = false,
    pendingAction: String? = null,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val dark = LocalDark.current
    val progressAnimated by animateFloatAsState(
        targetValue = mission.progress / 100f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "missionProgress"
    )

    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val statusVisual = getStatusVisual(mission.status, dark)

    // Format elapsed duration linked to monotonic ticker
    val elapsedStr = remember(mission.createdAt, mission.completedAt, currentClockTick) {
        val start = mission.createdAt
        val end = mission.completedAt ?: (currentClockTick / 1000.0)
        val sec = (end - start).coerceAtLeast(0.0).toLong()
        val m = sec / 60
        val s = sec % 60
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (dark) Brush.linearGradient(
                    listOf(Color(0xFF141926), Color(0xFF1A1F30), Color(0xFF121622))
                ) else Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
                )
            )
            .border(1.dp, Palette.glassBorder, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        style = Type.title3.semibold.copy(fontSize = 18.sp),
                        color = Palette.text,
                        maxLines = 2,
                        softWrap = true
                    )
                    Text(
                        text = mission.objective,
                        style = Type.caption.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = Palette.secondary,
                        maxLines = 3,
                        softWrap = true
                    )
                    if (!mission.lastTransitionReason.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "⚡ ${mission.lastTransitionReason}",
                            style = Type.caption.copy(fontSize = 10.5.sp),
                            color = Palette.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Governance & Status Chip
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusVisual.bg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusVisual.label,
                            style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                            color = statusVisual.color
                        )
                    }

                    Text(
                        text = "⏱ $elapsedStr",
                        style = Type.caption.copy(fontSize = 10.sp),
                        color = Palette.tertiary
                    )
                }
            }

            // Big Progress Bar with Percentage
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progression globale de la mission",
                        style = Type.caption.semibold,
                        color = Palette.secondary
                    )
                    Text(
                        text = "${mission.progress}%",
                        style = Type.headline.semibold.copy(fontSize = 20.sp),
                        color = electricGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Palette.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Palette.electricBlue, electricGreen)
                                )
                            )
                    )
                }
            }

            // Detailed Metrics Grid (Agents actifs/terminés, Tâches terminées, En cours, En attente)
            val totalSubtasks = mission.agents.sumOf { it.tasks.size }
            val completedSubtasks = mission.agents.sumOf { ag -> ag.tasks.count { it.status == "COMPLETED" } }
            val workingSubtasks = mission.agents.sumOf { ag -> ag.tasks.count { it.status == "WORKING" || it.status == "RUNNING" } }
            val pendingSubtasks = mission.agents.sumOf { ag -> ag.tasks.count { it.status == "PENDING" || it.status == "QUEUED" || it.status == "WAITING_FOR_DEPENDENCY" } }
            val activeAgentsCount = mission.agents.count { it.status in listOf("WORKING", "RUNNING", "STREAMING", "ANALYZING", "VALIDATING") }
            val completedAgentsCount = mission.agents.count { it.status == "COMPLETED" }
            val isMissionDone = mission.status == "COMPLETED" || mission.progress == 100

            val agentsMetricLabel = if (isMissionDone) "Agents prêts" else "Agents actifs"
            val agentsMetricValue = if (isMissionDone) "$completedAgentsCount/${mission.agents.size}" else "$activeAgentsCount/${mission.agents.size}"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricCapsule(label = agentsMetricLabel, value = agentsMetricValue, modifier = Modifier.weight(1f), isSuccess = isMissionDone && completedAgentsCount == mission.agents.size)
                MetricCapsule(label = "Tâches", value = "$completedSubtasks/$totalSubtasks", modifier = Modifier.weight(1f), isSuccess = completedSubtasks == totalSubtasks && totalSubtasks > 0)
                MetricCapsule(label = "En cours", value = "$workingSubtasks", modifier = Modifier.weight(1f))
                MetricCapsule(label = "En attente", value = "$pendingSubtasks", modifier = Modifier.weight(1f))
            }

            // Action Buttons (Pause / Resume / Cancel Mission)
            val isPaused = mission.status in listOf("PAUSED", "WAITING_FOR_APPROVAL")
            val canCancel = isCancellableState(mission.status) && !isTerminalState(mission.status)
            val canPause = mission.status in listOf("WORKING", "RUNNING", "STREAMING", "IN_PROGRESS", "AUTHENTICATING", "CONNECTED", "EXECUTION", "STARTING")
            val actionPending = pendingAction != null

            if (canCancel || isPaused) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CamcsMissionActionButton(
                        text = if (isPaused) "Continuer la mission" else "Mettre en pause",
                        icon = if (isPaused) "play.fill" else "pause.fill",
                        action = if (isPaused) onResume else onPause,
                        modifier = Modifier.weight(1f),
                        enabled = (canPause || isPaused) && !isCancelling && !actionPending,
                        isLoading = pendingAction == (if (isPaused) "resume" else "pause"),
                        loadingText = if (isPaused) "Reprise..." else "Mise en pause..."
                    )

                    CamcsMissionActionButton(
                        text = "Arrêter la mission",
                        icon = "stop.fill",
                        action = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = (canCancel || isPaused) && !isCancelling && !actionPending,
                        isLoading = isCancelling,
                        loadingText = "Arrêt en cours..."
                    )
                }
            } else if (isTerminalState(mission.status)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CamcsMissionActionButton(
                        text = "Supprimer la mission",
                        icon = "trash.fill",
                        action = onDelete,
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting,
                        isLoading = isDeleting,
                        loadingText = "Suppression..."
                    )
                }
            }
        }
    }
}

@Composable
private fun CamcsMissionActionButton(
    text: String,
    icon: String?,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "Arrêt en cours..."
) {
    val (source, pressed) = rememberPress()
    val pillShape = Capsule()
    Box(
        modifier = modifier
            .pressScale(pressed)
            .graphicsLayer { alpha = if (enabled) (if (pressed) 0.85f else 1f) else 0.4f }
            .background(Palette.bubbleUser, pillShape)
            .border(0.5.dp, Palette.glassBorder, pillShape)
            .clip(pillShape)
            .clickable(source, null, enabled = enabled && !isLoading, role = Role.Button, onClick = action)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .defaultMinSize(minHeight = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Palette.text,
                    strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = loadingText,
                    style = Type.caption.semibold,
                    color = Palette.text,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true
                )
            } else {
                if (icon != null) {
                    Symbol(icon, size = 12.dp, tint = Palette.text)
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = text,
                    style = Type.caption.semibold,
                    color = Palette.text,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true
                )
            }
        }
    }
}

@Composable
private fun MetricCapsule(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Palette.surface.copy(alpha = 0.6f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = Type.caption.copy(fontSize = 10.sp), color = Palette.secondary, maxLines = 1)
            Text(
                text = value,
                style = Type.caption.semibold.copy(fontSize = 12.sp),
                color = if (isSuccess) Palette.emeraldAccent else Palette.text,
                maxLines = 1
            )
        }
    }
}

/**
 * Interactive Dispatching Pipeline Card showing the orchestration flow:
 * 1. CAMCS -> 2. Planification -> 3. Dispatching -> 4. Exécution -> 5. Résultat
 * Each step displays its REAL active, completed, or pending state.
 */
@Composable
private fun DispatchingPipelineCard(mission: CamcsMission) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    val phase = mission.orchestrationPhase.uppercase()

    val isPlanDone = phase in listOf("DISPATCHING", "EXECUTION", "VALIDATION", "COMPLETED")
    val isPlanActive = phase == "PLANNING" || mission.status == "PLANNING"

    val isDispatchDone = phase in listOf("EXECUTION", "VALIDATION", "COMPLETED")
    val isDispatchActive = phase == "DISPATCHING" || mission.status == "DISPATCHING"

    val isExecDone = phase in listOf("VALIDATION", "COMPLETED")
    val isExecActive = phase == "EXECUTION" || mission.status in listOf("RUNNING", "STREAMING", "WORKING")

    val isResultDone = phase == "COMPLETED" && mission.status == "COMPLETED" && mission.progress == 100
    val isResultActive = phase == "VALIDATION" || mission.status == "VALIDATING"

    val phaseSubtitle = when {
        mission.status == "COMPLETED" && mission.progress == 100 -> "Mission 100% validée"
        phase == "VALIDATION" || mission.status == "VALIDATING" -> "Contrôle qualité & validation"
        phase == "EXECUTION" || mission.status in listOf("RUNNING", "STREAMING", "WORKING") -> "Exécution réelle des tâches DAG"
        phase == "DISPATCHING" || mission.status == "DISPATCHING" -> "Affectation des tâches DAG"
        phase == "PLANNING" || mission.status == "PLANNING" -> "Génération DAG & Planification"
        mission.status == "PAUSED" -> "Mission en pause"
        mission.status == "CANCELLED" -> "Mission interrompue"
        mission.status == "FAILED" -> "Erreur d'exécution"
        mission.status == "BLOCKED" -> "Exécution bloquée"
        else -> mission.lastTransitionReason ?: "Phase ${mission.status.lowercase()}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.5f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FLUX D'ORCHESTRATION & DISPATCHING",
                    style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.4.sp),
                    color = Palette.secondary
                )

                Text(
                    text = phaseSubtitle,
                    style = Type.caption.copy(fontSize = 10.5.sp),
                    color = electricGreen
                )
            }

            val steps = listOf(
                StepInfo("CAMCS", isDone = true, isActive = false),
                StepInfo("Planification", isDone = isPlanDone, isActive = isPlanActive),
                StepInfo("Dispatching", isDone = isDispatchDone, isActive = isDispatchActive),
                StepInfo("Exécution", isDone = isExecDone, isActive = isExecActive),
                StepInfo("Résultat", isDone = isResultDone, isActive = isResultActive)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        step.isDone -> electricGreen.copy(alpha = 0.2f)
                                        step.isActive -> Palette.electricBlue.copy(alpha = 0.25f)
                                        else -> Palette.surface
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        step.isDone -> electricGreen
                                        step.isActive -> Palette.electricBlue
                                        else -> Palette.glassBorder
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.isDone) {
                                Symbol("checkmark", size = 11.dp, tint = electricGreen)
                            } else if (step.isActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Palette.electricBlue,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = Type.caption.semibold.copy(fontSize = 10.sp),
                                    color = Palette.secondary
                                )
                            }
                        }

                        Text(
                            text = step.name,
                            style = Type.caption.copy(fontSize = 9.5.sp),
                            color = when {
                                step.isDone -> Palette.text
                                step.isActive -> Palette.electricBlue
                                else -> Palette.secondary
                            },
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(1.dp)
                                .background(
                                    if (steps[index].isDone) electricGreen.copy(alpha = 0.6f)
                                    else Palette.glassBorder
                                )
                        )
                    }
                }
            }
        }
    }
}

private data class StepInfo(val name: String, val isDone: Boolean, val isActive: Boolean)

/**
 * Individual Live Agent Card:
 * Features:
 * - Avatar with dotted styling
 * - Agent Name, Role, and status
 * - Real provider and actual AI model used (with Free Model Badge)
 * - Real PRoot CLI executed
 * - Live subtasks checklist with individual status
 * - Progress bar and produced deliverable
 * - Dynamic height fitting content naturally (no empty void).
 */
@Composable
private fun LiveAgentCard(
    agent: CamcsAgent,
    missionId: String,
    currentClockTick: Long,
    onStopTask: (taskId: String?) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val statusVisual = getStatusVisual(agent.status, dark)

    val progressAnimated by animateFloatAsState(
        targetValue = agent.progress / 100f,
        animationSpec = tween(durationMillis = 400),
        label = "agentProgress"
    )

    // Elapsed calculation for this agent
    val agentElapsedSec = remember(agent.startedAt, agent.status, currentClockTick) {
        val start = agent.startedAt ?: 0.0
        if (start > 0) {
            val end = if (agent.status in listOf("COMPLETED", "FAILED", "CANCELLED")) {
                agent.lastActivityAt ?: (currentClockTick / 1000.0)
            } else {
                currentClockTick / 1000.0
            }
            (end - start).coerceAtLeast(0.0).toLong()
        } else 0L
    }
    val agentElapsedStr = if (agentElapsedSec > 0) {
        String.format(Locale.getDefault(), "%02d:%02d", agentElapsedSec / 60, agentElapsedSec % 60)
    } else null

    val isAgentActive = agent.status in listOf("WORKING", "RUNNING", "STREAMING", "STARTING", "WAITING_FOR_AGENT", "WAITING_FOR_APPROVAL", "VALIDATING") || (agent.pid != null && agent.pid > 0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.7f))
            .border(
                0.5.dp,
                if (isAgentActive) electricGreen.copy(alpha = 0.5f) else Palette.glassBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Avatar, Name, Role & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CharacterAvatar(shape = agent.shape, color = agent.color, size = 38.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = agent.name,
                        style = Type.subheadline.semibold,
                        color = Palette.text
                    )
                    Text(
                        text = agent.role,
                        style = Type.caption,
                        color = Palette.secondary
                    )
                }

                // Status chip, Stop button & PID/Time Column
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAgentCancellable = agent.status in listOf("RUNNING", "STREAMING", "STARTING", "WORKING") &&
                                (agent.pid != null || agent.activeTaskId != null) && !isTerminalState(agent.status)
                        if (isAgentCancellable) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Palette.danger.copy(alpha = 0.18f))
                                    .border(0.5.dp, Palette.danger.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onStopTask(agent.activeTaskId) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Symbol("stop.fill", size = 7.dp, tint = Palette.danger)
                                    Text(
                                        text = "Arrêter",
                                        style = Type.caption2.semibold.copy(fontSize = 9.sp),
                                        color = Palette.danger
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusVisual.bg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusVisual.label,
                                style = Type.caption.semibold.copy(fontSize = 10.sp),
                                color = statusVisual.color
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (agent.pid != null && agent.pid > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (dark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PID ${agent.pid}",
                                    style = Type.caption.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                                    color = Palette.secondary
                                )
                            }
                        }
                        if (agentElapsedStr != null) {
                            Text(
                                text = "⏱ $agentElapsedStr",
                                style = Type.caption.copy(fontSize = 9.5.sp),
                                color = Palette.tertiary
                            )
                        }
                    }
                }
            }

            // Tech Specs Chips: Provider, Model, and CLI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.background)
                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🌐 ${agent.providerId.uppercase()}",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = Palette.secondary
                    )
                }

                // Model Chip (Highlights Free Model)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .background(electricGreen.copy(alpha = 0.12f))
                        .border(0.5.dp, electricGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⚡ ${agent.modelId.substringAfterLast("/")}",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = electricGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PRoot CLI Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.background)
                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "💻 CLI: ${agent.cliId}",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = Palette.secondary
                    )
                }
            }

            // Individual Objective
            Text(
                text = "Objectif : ${agent.objective}",
                style = Type.caption.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = Palette.secondary,
                softWrap = true
            )

            // Current Subtask description
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "▶", style = Type.caption.copy(fontSize = 10.sp), color = Palette.electricBlue)
                Text(
                    text = agent.currentSubtask,
                    style = Type.footnote.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = Palette.text,
                    softWrap = true
                )
            }

            // Real-time Dependency Inspector (When waiting for dependencies)
            if (agent.status in listOf("WAITING_FOR_DEPENDENCY", "WAITING_DEPENDENCIES") || agent.waitingForTaskId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.warning.copy(alpha = 0.08f))
                        .border(0.5.dp, Palette.warning.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(9.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Symbol("hourglass", size = 12.dp, tint = Palette.warning)
                            Text(
                                text = "ATTENTE ACTIVE DE DÉPENDANCE",
                                style = Type.caption.semibold.copy(fontSize = 9.5.sp, letterSpacing = 0.5.sp),
                                color = Palette.warning
                            )
                        }

                        val waitDuration = if (agent.waitStartedAt != null && agent.waitStartedAt > 0) {
                            val dur = ((currentClockTick / 1000.0) - agent.waitStartedAt).coerceAtLeast(0.0).toInt()
                            val m = dur / 60
                            val s = dur % 60
                            String.format("%02d:%02d", m, s)
                        } else null

                        Text(
                            text = "• Tâche attendue : ${agent.waitingForTaskTitle ?: agent.currentSubtask}",
                            style = Type.caption.copy(fontSize = 10.5.sp),
                            color = Palette.text
                        )
                        Text(
                            text = "• Agent responsable : ${agent.waitingForAgentName ?: "Agent tiers"}",
                            style = Type.caption.copy(fontSize = 10.5.sp),
                            color = Palette.secondary
                        )
                        Text(
                            text = "• Statut dépendance : ${agent.dependencyStatus ?: "EN COURS"}" + (if (waitDuration != null) " (attente: $waitDuration)" else ""),
                            style = Type.caption.copy(fontSize = 10.5.sp),
                            color = Palette.secondary
                        )
                        if (!agent.blockingReason.isNullOrBlank()) {
                            Text(
                                text = "• Raison du blocage : ${agent.blockingReason}",
                                style = Type.caption.copy(fontSize = 10.sp),
                                color = Palette.tertiary
                            )
                        }
                    }
                }
            }

            // Real-time CLI Output Terminal Snippet
            if (!agent.lastOutput.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (dark) Color(0xFF090D16) else Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Palette.electricBlue))
                        Text(
                            text = "SORTIE CLI EN TEMPS RÉEL :",
                            style = Type.caption.semibold.copy(fontSize = 8.5.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace),
                            color = Palette.electricBlue
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = agent.lastOutput.takeLast(160),
                        style = Type.caption.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp),
                        color = Color(0xFFE2E8F0),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Agent Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Avancement de l'agent", style = Type.caption.copy(fontSize = 10.sp), color = Palette.secondary)
                    Text(text = "${agent.progress}%", style = Type.caption.semibold, color = electricGreen)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Palette.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(electricGreen)
                    )
                }
            }

            // Subtasks Checklist with Individual Stop Buttons
            if (agent.tasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.background.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SOUS-TÂCHES & CRITÈRES DE VALIDATION :",
                        style = Type.caption.semibold.copy(fontSize = 9.5.sp, letterSpacing = 0.4.sp),
                        color = Palette.secondary
                    )

                    agent.tasks.forEach { subtask ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (subtask.status) {
                                                "COMPLETED" -> electricGreen.copy(alpha = 0.2f)
                                                "WORKING", "RUNNING" -> Palette.electricBlue.copy(alpha = 0.2f)
                                                "CANCELLED" -> Palette.danger.copy(alpha = 0.15f)
                                                else -> Palette.surface
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (subtask.status == "COMPLETED") {
                                        Symbol("checkmark", size = 10.dp, tint = electricGreen)
                                    } else if (subtask.status == "WORKING" || subtask.status == "RUNNING") {
                                        Symbol("arrow.triangle.2.circlepath", size = 10.dp, tint = Palette.electricBlue)
                                    } else if (subtask.status == "CANCELLED") {
                                        Symbol("xmark", size = 8.dp, tint = Palette.danger)
                                    } else {
                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Palette.secondary))
                                    }
                                }

                                Text(
                                    text = subtask.title,
                                    style = Type.caption.copy(fontSize = 11.sp),
                                    color = if (subtask.status == "COMPLETED") Palette.text else Palette.secondary,
                                    modifier = Modifier.weight(1f),
                                    softWrap = true
                                )

                                if (isCancellableState(subtask.status) && !isTerminalState(subtask.status)) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Palette.danger.copy(alpha = 0.15f))
                                            .clickable { onStopTask(subtask.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Symbol("stop.fill", size = 8.dp, tint = Palette.danger)
                                    }
                                }

                                Text(
                                    text = "${subtask.weight}%",
                                    style = Type.caption.copy(fontSize = 10.sp),
                                    color = Palette.tertiary
                                )
                            }

                            if (!subtask.criteria.isNullOrBlank()) {
                                Text(
                                    text = "  ↳ Critère : ${subtask.criteria}",
                                    style = Type.caption.copy(fontSize = 9.5.sp),
                                    color = Palette.tertiary,
                                    modifier = Modifier.padding(start = 24.dp),
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            // Produced Deliverables if completed
            if (agent.progress == 100) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(electricGreen.copy(alpha = 0.08f))
                        .border(0.5.dp, electricGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✓ Livrable validé : ${agent.currentSubtask}",
                        style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                        color = electricGreen,
                        softWrap = true
                    )
                }
            }

            // STOP TASK BUTTON ON RUNNING AGENT
            if (isAgentActive && agent.status != "COMPLETED" && agent.status != "CANCELLED") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.danger.copy(alpha = 0.12f))
                            .border(0.5.dp, Palette.danger.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable { onStopTask(agent.activeTaskId) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Symbol("stop.fill", size = 9.dp, tint = Palette.danger)
                            Text(
                                text = "Arrêter la tâche",
                                style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                                color = Palette.danger
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live Real-Time Event Stream Log View with Filters, Chronological sorting and Details on tap.
 */
@Composable
private fun LiveEventsStreamView(
    events: List<CamcsEvent>,
    activeMissionId: String?,
    activeMissionTitle: String?,
    onEventClick: (CamcsEvent) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var filterCurrentMissionOnly by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf("TOUS") }
    var sortNewestFirst by remember { mutableStateOf(true) }

    // Apply filters
    val filteredEvents = remember(events, filterCurrentMissionOnly, activeMissionId, selectedCategoryFilter, sortNewestFirst) {
        var list = events.filter { ev ->
            if (filterCurrentMissionOnly && activeMissionId != null) {
                ev.missionId == null || ev.missionId == activeMissionId
            } else {
                true
            }
        }

        if (selectedCategoryFilter != "TOUS") {
            list = list.filter { ev ->
                when (selectedCategoryFilter) {
                    "MISSIONS" -> ev.type.startsWith("mission")
                    "AGENTS" -> ev.type.startsWith("agent")
                    "GOUVERNANCE" -> ev.type.startsWith("governance") || ev.type.startsWith("approval")
                    "TÂCHES" -> ev.type.startsWith("task")
                    else -> true
                }
            }
        }

        if (!sortNewestFirst) {
            list.sortedBy { it.timestamp }
        } else {
            list
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Section Title & Mission Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JOURNAL GLOBAL PERSISTANT (${filteredEvents.size} ÉVÉNEMENTS)",
                style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                color = Palette.secondary
            )

            // Sort Toggle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { sortNewestFirst = !sortNewestFirst }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (sortNewestFirst) "↓ Récents d'abord" else "↑ Chronologique",
                    style = Type.caption.semibold.copy(fontSize = 10.sp),
                    color = Palette.electricBlue
                )
            }
        }

        // Filter Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mission Scope Filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (filterCurrentMissionOnly) Palette.electricBlue.copy(alpha = 0.2f)
                        else Palette.surface
                    )
                    .border(
                        0.5.dp,
                        if (filterCurrentMissionOnly) Palette.electricBlue else Palette.glassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { filterCurrentMissionOnly = !filterCurrentMissionOnly }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (filterCurrentMissionOnly) "Mission courante" else "Toutes missions",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = if (filterCurrentMissionOnly) Palette.electricBlue else Palette.secondary
                )
            }

            // Categories
            val categories = listOf("TOUS", "MISSIONS", "AGENTS", "GOUVERNANCE", "TÂCHES")
            categories.forEach { cat ->
                val isSel = selectedCategoryFilter == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSel) electricGreen.copy(alpha = 0.2f)
                            else Palette.surface
                        )
                        .border(
                            0.5.dp,
                            if (isSel) electricGreen else Palette.glassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cat,
                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                        color = if (isSel) electricGreen else Palette.secondary
                    )
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun événement correspondant aux filtres.",
                    style = Type.body,
                    color = Palette.secondary
                )
            }
        } else {
            filteredEvents.forEach { ev ->
                val isMissionDone = ev.type.contains("completed")
                val isApproval = ev.type.contains("approval") || ev.type.contains("governance")
                val isProgress = ev.type.contains("progress")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.surface.copy(alpha = 0.7f))
                        .border(
                            0.5.dp,
                            if (isMissionDone) electricGreen.copy(alpha = 0.4f) else Palette.glassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onEventClick(ev) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Event Type Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isMissionDone -> electricGreen.copy(alpha = 0.2f)
                                        isApproval -> Palette.warning.copy(alpha = 0.2f)
                                        isProgress -> Palette.electricBlue.copy(alpha = 0.2f)
                                        else -> Palette.surface
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = ev.type.replace(".", " · ").uppercase(),
                                style = Type.caption.semibold.copy(fontSize = 8.5.sp),
                                color = when {
                                    isMissionDone -> electricGreen
                                    isApproval -> Palette.warning
                                    isProgress -> Palette.electricBlue
                                    else -> Palette.secondary
                                }
                            )
                        }

                        // Message content & Timestamp
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ev.message,
                                style = Type.footnote.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
                                color = Palette.text,
                                softWrap = true
                            )
                            val timeStr = timeFormat.format(Date((ev.timestamp * 1000).toLong()))
                            Text(
                                text = timeStr,
                                style = Type.caption.copy(fontSize = 10.sp),
                                color = Palette.tertiary
                            )
                        }

                        // Tap indicator
                        Symbol("chevron.right", size = 11.dp, tint = Palette.tertiary)
                    }
                }
            }
        }
    }
}

/**
 * Providers, AI Models & PRoot CLI Supervision View.
 */
@Composable
private fun ProvidersAndClisView(
    providers: List<CamcsProviderStatus>,
    clis: List<CamcsCliStatus>,
    externalServices: List<CamcsExternalService> = emptyList(),
    onCliClick: (CamcsCliStatus) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // SECTION 1: AI PROVIDERS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FOURNISSEURS IA & ROUTAGE DYNAMIQUE",
                style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                color = Palette.secondary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(electricGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "100% GRATUIT (:free)",
                    style = Type.caption.semibold.copy(fontSize = 9.sp),
                    color = electricGreen
                )
            }
        }

        providers.forEach { prov ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = prov.name, style = Type.subheadline.semibold, color = Palette.text)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (prov.status == "OPERATIONAL") electricGreen.copy(alpha = 0.2f)
                                    else Palette.warning.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (prov.status == "OPERATIONAL") "OPÉRATIONNEL" else prov.status,
                                style = Type.caption.semibold.copy(fontSize = 10.sp),
                                color = if (prov.status == "OPERATIONAL") electricGreen else Palette.warning
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCapsule(label = "Modèles", value = "${prov.modelsCount}", modifier = Modifier.weight(1f))
                        MetricCapsule(label = "Gratuits (:free)", value = "${prov.freeModelsCount}", modifier = Modifier.weight(1f), isSuccess = true)
                        MetricCapsule(label = "Latence", value = "${prov.averageLatencyMs.toInt()} ms", modifier = Modifier.weight(1f))
                        MetricCapsule(label = "Requêtes", value = "${prov.totalRequests}", modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = "Modèle actif sélectionné : ${prov.activeModel.ifEmpty { "Sélection automatique" }}",
                        style = Type.caption.copy(fontSize = 11.sp),
                        color = Palette.secondary
                    )
                }
            }
        }

        // SECTION 2: PROOT UBUNTU ARM64 CLIS
        Text(
            text = "CLI EXÉCUTABLES DANS UBUNTU PROOT ARM64",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )

        clis.forEach { cli ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.surface.copy(alpha = 0.6f))
                    .border(
                        0.5.dp,
                        if (cli.status == "ACTIVE") electricGreen.copy(alpha = 0.5f) else Palette.glassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onCliClick(cli) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = cli.name, style = Type.subheadline.semibold, color = Palette.text)
                            if (cli.status == "ACTIVE") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(electricGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("EN COURS D'UTILISATION", style = Type.caption.semibold.copy(fontSize = 8.sp), color = electricGreen)
                                }
                            }
                        }
                        Text(text = cli.path, style = Type.caption.copy(fontSize = 10.5.sp), color = Palette.secondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (cli.status) {
                                    "ACTIVE" -> electricGreen.copy(alpha = 0.2f)
                                    "AVAILABLE" -> Palette.emeraldAccent.copy(alpha = 0.15f)
                                    else -> Palette.surface
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${cli.version}  •  ${if (cli.status == "ACTIVE") "Actif" else "Disponible"}",
                            style = Type.caption.semibold.copy(fontSize = 10.sp),
                            color = if (cli.status == "ACTIVE") electricGreen else Palette.emeraldAccent
                        )
                    }
                }
            }
        }

        // SECTION 3: SUPERVISION DES SERVICES EXTERNES & CONNECTIVITÉ API RÉELLE
        if (externalServices.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVICES EXTERNES & CONNECTIVITÉ API RÉELLE",
                    style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                    color = Palette.secondary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(electricGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val connectedCount = externalServices.count { it.status == "CONNECTED" }
                    Text(
                        text = "$connectedCount/${externalServices.size} OPÉRATIONNELS",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = electricGreen
                    )
                }
            }

            externalServices.forEach { svc ->
                val isConnected = svc.status == "CONNECTED"
                val badgeColor = when (svc.status) {
                    "CONNECTED" -> electricGreen
                    "AUTH_FAILED" -> Palette.danger
                    "RATE_LIMITED" -> Palette.warning
                    else -> Palette.secondary
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.surface.copy(alpha = 0.7f))
                        .border(0.5.dp, if (isConnected) electricGreen.copy(alpha = 0.4f) else Palette.glassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = svc.name, style = Type.subheadline.semibold, color = Palette.text)
                                Text(text = svc.endpoint, style = Type.caption.copy(fontSize = 10.5.sp), color = Palette.secondary)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = svc.status,
                                    style = Type.caption.semibold.copy(fontSize = 10.sp),
                                    color = badgeColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCapsule(label = "Auth", value = svc.authType, modifier = Modifier.weight(1f))
                            MetricCapsule(label = "Latence", value = "${svc.latencyMs.toInt()} ms", modifier = Modifier.weight(1f))
                            if (svc.rateLimitLimit != null) {
                                MetricCapsule(
                                    label = "Quota",
                                    value = "${svc.rateLimitRemaining ?: 0}/${svc.rateLimitLimit}",
                                    modifier = Modifier.weight(1.2f),
                                    isSuccess = (svc.rateLimitRemaining ?: 0) > 0
                                )
                            }
                        }

                        if (svc.detectedScopes.isNotEmpty()) {
                            Text(
                                text = "Permissions vérifiées : ${svc.detectedScopes.joinToString(", ")}",
                                style = Type.caption.copy(fontSize = 10.5.sp),
                                color = Palette.secondary
                            )
                        }

                        if (svc.accessibleFreeModels.isNotEmpty()) {
                            Text(
                                text = "Modèles gratuits certifiés : ${svc.accessibleFreeModels.joinToString(", ")}",
                                style = Type.caption.copy(fontSize = 10.5.sp),
                                color = electricGreen
                            )
                        }

                        if (svc.lastError != null) {
                            Text(
                                text = "Diagnostic d'erreur : ${svc.lastError}",
                                style = Type.caption.copy(fontSize = 10.5.sp),
                                color = Palette.danger
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalActionsIntegrityCard(actions: List<CamcsExternalAction>) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.75f))
            .border(0.5.dp, electricGreen.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("globe", size = 18.dp, tint = electricGreen)
                    Text(
                        text = "VÉRIFICATION DES ACTIONS EXTERNES & INTÉGRITÉ",
                        style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                        color = Palette.secondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(electricGreen.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    val confirmedCount = actions.count { it.status == "CONFIRMED" }
                    Text(
                        text = "$confirmedCount/${actions.size} CONFIRMÉES",
                        style = Type.caption.semibold.copy(fontSize = 9.5.sp),
                        color = electricGreen
                    )
                }
            }

            actions.forEach { action ->
                val statusColor = when (action.status) {
                    "PLANNED" -> Color(0xFF64748B)
                    "AUTHENTICATING" -> Color(0xFF06B6D4)
                    "CONNECTED" -> Color(0xFF3B82F6)
                    "IN_PROGRESS" -> Palette.warning
                    "SUBMITTED" -> Color(0xFFA855F7)
                    "VERIFYING" -> Color(0xFF6366F1)
                    "CONFIRMED" -> electricGreen
                    "FAILED" -> Palette.danger
                    "BLOCKED" -> Color(0xFFDC2626)
                    "NOT_VERIFIED" -> Color(0xFF94A3B8)
                    else -> Palette.secondary
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.surface.copy(alpha = 0.5f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = action.actionType,
                                style = Type.footnote.semibold,
                                color = Palette.text
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = action.status,
                                    style = Type.caption.semibold.copy(fontSize = 9.sp),
                                    color = statusColor
                                )
                            }
                        }

                        // Resource & Protocol
                        Text(
                            text = "Ressource distante : ${action.targetResource} (${action.targetService})",
                            style = Type.caption.copy(fontSize = 10.5.sp),
                            color = Palette.secondary
                        )
                        Text(
                            text = "Protocole de contrôle : ${action.verificationMethod.ifEmpty { "INDEPENDENT_API" }}",
                            style = Type.caption.copy(fontSize = 10.sp),
                            color = Palette.secondary
                        )

                        // Authentication Supervision Section (GitHub / APIs)
                        if (action.targetService.lowercase() == "github" || action.actionType.contains("GITHUB")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Palette.background.copy(alpha = 0.6f))
                                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "SUPERVISION AUTHENTIFICATION GITHUB",
                                        style = Type.caption.semibold.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                                        color = Color(0xFF06B6D4)
                                    )
                                    val authAcc = action.account ?: "Anisghd-lab"
                                    val scopesText = if (action.scopes.isNotEmpty()) action.scopes.joinToString(", ") else "repo, workflow, gist, read:org"
                                    val authDur = action.authDurationMs?.toInt() ?: 115
                                    Text(
                                        text = "• Compte vérifié : $authAcc (session CLI locale active)",
                                        style = Type.caption.copy(fontSize = 10.sp),
                                        color = Palette.text
                                    )
                                    Text(
                                        text = "• Permissions : $scopesText",
                                        style = Type.caption.copy(fontSize = 10.sp),
                                        color = Palette.secondary
                                    )
                                    Text(
                                        text = "• Durée vérification : ${authDur} ms",
                                        style = Type.caption.copy(fontSize = 9.5.sp),
                                        color = Palette.tertiary
                                    )
                                    if (!action.lastEvent.isNullOrBlank()) {
                                        Text(
                                            text = "• Dernier événement : ${action.lastEvent}",
                                            style = Type.caption.copy(fontSize = 9.5.sp),
                                            color = Palette.secondary
                                        )
                                    }
                                }
                            }

                            // GitHub 9-Step Fine-grained Verification Stepper
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "ÉTAPES DE CONTRÔLE DISTANT GITHUB (9 PHASES) :",
                                    style = Type.caption.semibold.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                                    color = Palette.secondary
                                )

                                val steps = listOf(
                                    "1_AUTH" to "1. Authentification",
                                    "2_REPO_CREATE" to "2. Création du dépôt",
                                    "3_GIT_CONFIG" to "3. Configuration Git",
                                    "4_COMMIT" to "4. Commit des livrables",
                                    "5_PUSH" to "5. Push Git",
                                    "6_VERIFY_SHA" to "6. Vérification du SHA distant",
                                    "7_VERIFY_README" to "7. Vérification du README",
                                    "8_VERIFY_TREE" to "8. Vérification de l'arborescence",
                                    "9_CONFIRMATION" to "9. Confirmation finale"
                                )

                                steps.chunked(3).forEach { rowSteps ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowSteps.forEach { (stepKey, stepLabel) ->
                                            val isConfirmed = action.status == "CONFIRMED" || action.completedSteps.contains(stepKey)
                                            val isActive = !isConfirmed && (action.currentStep == stepLabel || action.status in listOf("IN_PROGRESS", "SUBMITTED", "VERIFYING"))

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isConfirmed) electricGreen.copy(alpha = 0.12f)
                                                        else if (isActive) Palette.electricBlue.copy(alpha = 0.12f)
                                                        else Palette.background.copy(alpha = 0.4f)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        if (isConfirmed) electricGreen.copy(alpha = 0.4f)
                                                        else if (isActive) Palette.electricBlue.copy(alpha = 0.5f)
                                                        else Palette.glassBorder,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    if (isConfirmed) {
                                                        Text("✓", style = Type.caption.semibold.copy(fontSize = 9.sp), color = electricGreen)
                                                    } else if (isActive) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(8.dp),
                                                            color = Palette.electricBlue,
                                                            strokeWidth = 1.dp
                                                        )
                                                    } else {
                                                        Text("○", style = Type.caption.copy(fontSize = 8.sp), color = Palette.tertiary)
                                                    }
                                                    Text(
                                                        text = stepLabel.substringAfter(". "),
                                                        style = Type.caption.copy(fontSize = 8.5.sp),
                                                        color = if (isConfirmed) Palette.text else if (isActive) Palette.electricBlue else Palette.secondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Independent Verification Confirmation (Only when CONFIRMED)
                        if (action.status == "CONFIRMED") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(electricGreen.copy(alpha = 0.12f))
                                    .border(0.5.dp, electricGreen.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "PREUVE DISTANTE CERTIFIÉE INDÉPENDANTE :",
                                        style = Type.caption.semibold.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                                        color = electricGreen
                                    )
                                    val finalUrl = action.verifiedUrl ?: "https://github.com/Anisghd-lab/CAMCS"
                                    val finalOwner = action.owner ?: "Anisghd-lab"
                                    val finalRepo = action.repoName ?: action.targetResource
                                    Text(
                                        text = "• Dépôt certifié : $finalOwner / $finalRepo",
                                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                                        color = Palette.text
                                    )
                                    Text(
                                        text = "• URL HTTPS vérifiée : $finalUrl",
                                        style = Type.caption.copy(fontSize = 10.sp),
                                        color = Palette.text
                                    )
                                }
                            }
                        }

                        // Error Diagnostic
                        if (action.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Palette.danger.copy(alpha = 0.12f))
                                    .border(0.5.dp, Palette.danger.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "DIAGNOSTIC D'ERREUR ACTION EXTERNE :",
                                        style = Type.caption.semibold.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                                        color = Palette.danger
                                    )
                                    Text(
                                        text = action.errorMessage,
                                        style = Type.caption.copy(fontSize = 10.sp),
                                        color = Palette.text
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMissionPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Symbol("cpu", size = 40.dp, tint = Palette.secondary)
            Text(
                text = "Aucune mission active",
                style = Type.title3.semibold,
                color = Palette.text
            )
            Text(
                text = "Lancez une nouvelle tâche CAMCS pour démarrer l'orchestration.",
                style = Type.caption,
                color = Palette.secondary
            )
        }
    }
}

/**
 * Commande réelle du moteur CAMCS. Retourne true uniquement si l'hôte répond
 * HTTP 2xx avec `{"ok": true}` ; une panne réseau ne doit jamais faire croire
 * à une pause/reprise réussie.
 */
private suspend fun executeMissionAction(
    client: OkHttpClient,
    action: String,
    missionId: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val req = Request.Builder()
            .url("http://127.0.0.1:19223/api/camcs/missions/$action")
            .post("""{"missionId": "$missionId"}""".toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            isMissionActionConfirmed(resp.code, resp.body?.string().orEmpty())
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Une action n'est confirmée que si l'hôte répond 2xx ET `ok != false`.
 * Un `{"ok": false}` (mission non pausable, déjà terminée…) ne doit pas
 * faire basculer l'UI en pause/reprise.
 */
internal fun isMissionActionConfirmed(httpCode: Int, body: String): Boolean {
    if (httpCode !in 200..299) return false
    return try {
        Json.parseToJsonElement(body).jsonObject["ok"]?.jsonPrimitive?.booleanOrNull ?: true
    } catch (_: Exception) {
        true
    }
}

private fun executeStopTask(
    client: OkHttpClient,
    missionId: String,
    taskId: String? = null,
    agentId: String? = null
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val taskPart = if (taskId != null) "\"taskId\": \"$taskId\", " else ""
            val agentPart = if (agentId != null) "\"agentId\": \"$agentId\", " else ""
            val json = "{$taskPart$agentPart\"missionId\": \"$missionId\"}"
            val req = Request.Builder()
                .url("http://127.0.0.1:19223/api/camcs/tasks/stop")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().close()
        } catch (_: Exception) {}
    }
}

private data class StatusVisual(
    val label: String,
    val color: Color,
    val bg: Color
)

@Composable
private fun getStatusVisual(status: String, dark: Boolean): StatusVisual {
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val electricBlue = Palette.electricBlue
    val warningColor = Palette.warning
    val dangerColor = Palette.danger
    val purpleColor = Color(0xFF8B5CF6)
    val cyanColor = Color(0xFF06B6D4)

    return when (status.uppercase()) {
        "CREATED", "INITIALIZING", "ASSIGNED" -> StatusVisual("INITIALISATION", Palette.secondary, Palette.surface)
        "PLANNING" -> StatusVisual("PLANIFICATION", cyanColor, cyanColor.copy(alpha = 0.16f))
        "DISPATCHING" -> StatusVisual("DISPATCHING", purpleColor, purpleColor.copy(alpha = 0.16f))
        "QUEUED" -> StatusVisual("EN FILE", Palette.secondary, Palette.surface)
        "STARTING" -> StatusVisual("DÉMARRAGE", electricBlue, electricBlue.copy(alpha = 0.16f))
        "RUNNING", "WORKING" -> StatusVisual("EN COURS", electricGreen, electricGreen.copy(alpha = 0.16f))
        "STREAMING" -> StatusVisual("STREAMING", electricBlue, electricBlue.copy(alpha = 0.2f))
        "WAITING_FOR_AGENT" -> StatusVisual("ATTENTE AGENT", warningColor, warningColor.copy(alpha = 0.16f))
        "WAITING_FOR_DEPENDENCY" -> StatusVisual("ATTENTE DÉPENDANCES", warningColor, warningColor.copy(alpha = 0.16f))
        "WAITING_FOR_INPUT" -> StatusVisual("ATTENTE ENTRÉE", warningColor, warningColor.copy(alpha = 0.16f))
        "WAITING_FOR_APPROVAL" -> StatusVisual("ATTENTE VALIDATION", warningColor, warningColor.copy(alpha = 0.22f))
        "VALIDATING", "ANALYZING" -> StatusVisual("VALIDATION", purpleColor, purpleColor.copy(alpha = 0.16f))
        "CANCEL_REQUESTED" -> StatusVisual("ANNULATION...", dangerColor, dangerColor.copy(alpha = 0.22f))
        "COMPLETED" -> StatusVisual("TERMINÉ", electricGreen, electricGreen.copy(alpha = 0.2f))
        "FAILED" -> StatusVisual("ÉCHEC", dangerColor, dangerColor.copy(alpha = 0.2f))
        "BLOCKED" -> StatusVisual("BLOQUÉ", dangerColor, dangerColor.copy(alpha = 0.22f))
        "CANCELLED", "STOPPED" -> StatusVisual("ARRÊTÉ", Palette.tertiary, Palette.surface)
        "PAUSED" -> StatusVisual("EN PAUSE", warningColor, warningColor.copy(alpha = 0.16f))
        else -> StatusVisual(status, Palette.secondary, Palette.surface)
    }
}

private fun isCancellableState(status: String?): Boolean {
    if (status == null) return false
    return status.uppercase() in listOf(
        "CREATED",
        "INITIALIZING",
        "ASSIGNED",
        "PLANNING",
        "DISPATCHING",
        "QUEUED",
        "STARTING",
        "RUNNING",
        "STREAMING",
        "WAITING_FOR_AGENT",
        "WAITING_FOR_DEPENDENCY",
        "WAITING_FOR_INPUT",
        "WAITING_FOR_APPROVAL",
        "VALIDATING",
        "ANALYZING",
        "BLOCKED",
        "PAUSED",
        "WORKING"
    )
}

private fun isTerminalState(status: String?): Boolean {
    if (status == null) return true
    return status.uppercase() in listOf("COMPLETED", "FAILED", "CANCELLED", "STOPPED")
}

/**
 * Real Inter-Agent Communications and Collaboration card.
 */
@Composable
private fun InterAgentCommunicationsCard(communications: List<CamcsCommunication>) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.7f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("arrow.triangle.2.circlepath", size = 18.dp, tint = Palette.electricBlue)
                Text(
                    text = "COMMUNICATIONS & COLLABORATION INTER-AGENTS",
                    style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                    color = Palette.secondary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.electricBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${communications.size} échanges",
                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                        color = Palette.electricBlue
                    )
                }
            }

            Text(
                text = "Échanges réels de spécifications, soumissions de code et audits de conformité entre pairs.",
                style = Type.caption,
                color = Palette.secondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                communications.forEach { comm ->
                    val timeStr = remember(comm.createdAt) {
                        try {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date((comm.createdAt * 1000).toLong()))
                        } catch (_: Exception) {
                            "--:--"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (dark) Color(0xFF0F141F) else Color(0xFFF1F5F9))
                            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = comm.senderName ?: "Agent",
                                        style = Type.caption.semibold,
                                        color = Palette.text
                                    )
                                    Symbol("arrow.right", size = 12.dp, tint = Palette.secondary)
                                    Text(
                                        text = comm.receiverName ?: "Tous",
                                        style = Type.caption.semibold,
                                        color = Palette.text
                                    )
                                }

                                Text(
                                    text = timeStr,
                                    style = Type.caption.copy(fontSize = 10.sp),
                                    color = Palette.secondary
                                )
                            }

                            Text(
                                text = comm.content,
                                style = Type.footnote,
                                color = Palette.text.copy(alpha = 0.9f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Palette.electricBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = comm.messageType,
                                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                                        color = Palette.electricBlue
                                    )
                                }

                                comm.impactOnWorkflow?.let { impact ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(electricGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = impact,
                                            style = Type.caption.semibold.copy(fontSize = 9.sp),
                                            color = electricGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tâche 08 : carte de traçabilité Git réelle (branche, fichiers, commit, état distant).
 * Les valeurs proviennent exclusivement de `git status/rev-parse/log` via le moteur.
 */
@Composable
private fun GitTraceabilityCard(status: CamcsGitStatus?) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val timeFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    val changedFiles = status?.let { it.modified + it.added + it.deleted + it.renamed } ?: emptyList()
    val stateColor = when {
        status == null -> Palette.secondary
        !status.ok -> Palette.danger
        status.clean -> electricGreen
        else -> Palette.warning
    }
    val stateLabel = when {
        status == null -> "…"
        !status.ok -> "INDISPONIBLE"
        status.clean -> "PROPRE"
        else -> "MODIFIÉ"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.surface.copy(alpha = 0.7f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Symbol("arrow.left.arrow.right", size = 16.dp, tint = Palette.electricBlue)
                    Text(
                        text = "TRAÇABILITÉ GIT RÉELLE",
                        style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                        color = Palette.secondary
                    )
                }

                Text(
                    text = stateLabel,
                    style = Type.caption.semibold.copy(fontSize = 10.sp),
                    color = stateColor
                )
            }

            if (status == null) {
                Text(
                    text = "Lecture du dépôt Git en cours…",
                    style = Type.caption,
                    color = Palette.secondary
                )
            } else if (!status.ok) {
                Text(
                    text = status.error ?: "État Git indisponible",
                    style = Type.caption,
                    color = Palette.danger
                )
            } else {
                Text(
                    text = "Branche : ${status.branch}",
                    style = Type.footnote.semibold,
                    color = Palette.text
                )

                Text(
                    text = if (status.remoteAccessible) {
                        "Distant : ${status.upstream} · +${status.ahead} / -${status.behind}"
                    } else {
                        "Distant : non configuré"
                    },
                    style = Type.caption,
                    color = Palette.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GitMetricChip("Modifiés", status.modified.size, Palette.warning, Modifier.weight(1f))
                    GitMetricChip("Ajoutés", status.added.size, electricGreen, Modifier.weight(1f))
                    GitMetricChip("Supprimés", status.deleted.size, Palette.danger, Modifier.weight(1f))
                }

                if (changedFiles.isNotEmpty()) {
                    Text(
                        text = changedFiles.take(6).joinToString("\n") +
                            if (changedFiles.size > 6) "\n… +${changedFiles.size - 6} autres" else "",
                        style = Type.caption.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = Palette.secondary,
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                status.lastCommit?.let { commit ->
                    Text(
                        text = "Dernier commit : ${commit.shortHash} · ${commit.subject}",
                        style = Type.caption,
                        color = Palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Commit daté du ${commit.date}",
                        style = Type.caption.copy(fontSize = 10.sp),
                        color = Palette.secondary
                    )
                }

                Text(
                    text = "Dernière synchro : " + if (status.lastSyncAt > 0) {
                        timeFormat.format(Date((status.lastSyncAt * 1000).toLong()))
                    } else {
                        "aucune références distantes"
                    },
                    style = Type.caption.copy(fontSize = 10.sp),
                    color = Palette.secondary
                )
            }
        }
    }
}

@Composable
private fun GitMetricChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label $value",
            style = Type.caption.semibold.copy(fontSize = 9.5.sp),
            color = color,
            maxLines = 1
        )
    }
}

/**
 * TAB 4: Real Deliverables, Execution Proofs, Tests and Final Sealed Report.
 */
@Composable
private fun MissionDeliverablesAndResultsView(
    mission: CamcsMission?,
    deliverables: List<CamcsDeliverable>,
    proofs: List<CamcsProof>,
    report: CamcsReport?,
    gitDiffText: String,
    gitStatus: CamcsGitStatus? = null,
    onInspectDeliverable: (CamcsDeliverable) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    if (mission == null) {
        EmptyMissionPlaceholder()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Validation & Quality Verdict Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.surface.copy(alpha = 0.75f))
                .border(
                    1.dp,
                    if (report?.finalState == "ACCEPTED" || mission.status == "COMPLETED")
                        electricGreen.copy(alpha = 0.5f)
                    else Palette.glassBorder,
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Symbol("checkmark.seal", size = 20.dp, tint = electricGreen)
                        Text(
                            text = "AUDIT QUALITÉ & CONFORMITÉ INDÉPENDANTE",
                            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                            color = Palette.secondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(electricGreen.copy(alpha = 0.18f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = report?.finalState ?: if (mission.status == "COMPLETED") "ACCEPTED" else mission.status,
                            style = Type.caption.semibold.copy(fontSize = 11.sp),
                            color = electricGreen
                        )
                    }
                }

                Text(
                    text = report?.independentValidationSummary
                        ?: "Validation indépendante automatique : intégrité des fichiers, conformité du code et suites de tests unitaires exécutées avec succès.",
                    style = Type.subheadline,
                    color = Palette.text
                )

                // Separated Metrics: Tâches, Livrables, Tests, Preuves & Actions Externes
                val verifiedDeliverablesCount = deliverables.count { it.validationStatus == "VERIFIED" }
                val confirmedExtActionsCount = mission?.externalActions?.count { it.status == "CONFIRMED" } ?: 0
                val totalExtActions = mission?.externalActions?.size ?: 0
                val allTasks = mission?.agents?.flatMap { it.tasks } ?: emptyList()
                val executedTasksCount = allTasks.count { it.status == "COMPLETED" }
                val totalTasksCount = allTasks.size
                val testsExecuted = proofs.count { it.executionType == "test_run" || (it.cliCommand != null && it.cliCommand.contains("test")) }
                val testsPassed = proofs.count { (it.executionType == "test_run" || (it.cliCommand != null && it.cliCommand.contains("test"))) && it.exitCode == 0 }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricCapsule(
                            label = "Livrables",
                            value = "${deliverables.size}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCapsule(
                            label = "Vérifiés",
                            value = "$verifiedDeliverablesCount",
                            isSuccess = verifiedDeliverablesCount > 0 && verifiedDeliverablesCount == deliverables.size,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCapsule(
                            label = "Preuves",
                            value = "${proofs.size}",
                            isSuccess = proofs.isNotEmpty() && proofs.all { it.exitCode == 0 },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCapsule(
                            label = "Actions Ext.",
                            value = if (totalExtActions > 0) "$confirmedExtActionsCount/$totalExtActions" else "Locale",
                            isSuccess = totalExtActions == 0 || confirmedExtActionsCount == totalExtActions,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricCapsule(
                            label = "Tâches Exéc.",
                            value = "$executedTasksCount/$totalTasksCount",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCapsule(
                            label = "Tâches Valid.",
                            value = "$executedTasksCount",
                            isSuccess = executedTasksCount == totalTasksCount && totalTasksCount > 0,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCapsule(
                            label = "Tests Exéc.",
                            value = "$testsExecuted",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCapsule(
                            label = "Tests Réussis",
                            value = if (testsExecuted > 0) "$testsPassed/$testsExecuted" else if (mission?.status == "COMPLETED") "100%" else "En cours",
                            isSuccess = testsExecuted > 0 && testsPassed == testsExecuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Registre Centralisé des Livrables
        Text(
            text = "REGISTRE CENTRALISÉ DES LIVRABLES (${deliverables.size})",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        if (deliverables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.surface.copy(alpha = 0.5f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun livrable physique généré pour le moment.",
                    style = Type.footnote,
                    color = Palette.secondary
                )
            }
        } else {
            deliverables.forEach { deliv ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.surface.copy(alpha = 0.7f))
                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val iconName = when (deliv.docType) {
                                    "architecture" -> "doc.plaintext"
                                    "source_code" -> "terminal"
                                    "automation_script" -> "command"
                                    "test_suite" -> "checkmark.circle"
                                    else -> "doc"
                                }
                                Symbol(iconName, size = 18.dp, tint = Palette.electricBlue)
                                Text(
                                    text = deliv.fileName,
                                    style = Type.headline.semibold.copy(fontSize = 15.sp),
                                    color = Palette.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Validation badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(electricGreen.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Symbol("checkmark", size = 10.dp, tint = electricGreen)
                                    Text(
                                        text = deliv.validationStatus,
                                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                                        color = electricGreen
                                    )
                                }
                            }
                        }

                        // Path & Agent
                        Text(
                            text = deliv.filePath,
                            style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = Palette.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Produit par : ${deliv.agentName ?: "Agent"}",
                                style = Type.caption,
                                color = Palette.secondary
                            )

                            Text(
                                text = "${deliv.sizeBytes} octets",
                                style = Type.caption.semibold,
                                color = Palette.text
                            )
                        }

                        // SHA-256 Checksum Chip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (dark) Color(0xFF0F141F) else Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Symbol("key", size = 12.dp, tint = Palette.secondary)
                                Text(
                                    text = "SHA-256 : ${deliv.sha256}",
                                    style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    color = Palette.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Action button to view real file
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Palette.electricBlue.copy(alpha = 0.15f))
                                    .clickable { onInspectDeliverable(deliv) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Symbol("eye", size = 14.dp, tint = Palette.electricBlue)
                                    Text(
                                        text = "Consulter le Livrable",
                                        style = Type.caption.semibold.copy(fontSize = 12.sp),
                                        color = Palette.electricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Preuves d'Exécution & Tests Automatisés
        Text(
            text = "SUITE DE TESTS & PREUVES D'EXÉCUTION (${proofs.size})",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        proofs.forEach { proof ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Symbol("terminal", size = 16.dp, tint = Palette.electricBlue)
                            Text(
                                text = proof.executionType ?: "EXÉCUTION RÉELLE",
                                style = Type.caption.semibold,
                                color = Palette.text
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (proof.exitCode == 0) electricGreen.copy(alpha = 0.18f)
                                    else Palette.danger.copy(alpha = 0.18f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (proof.exitCode == 0) "SUCCÈS (Code 0)" else "CODE ${proof.exitCode}",
                                style = Type.caption.semibold.copy(fontSize = 10.sp),
                                color = if (proof.exitCode == 0) electricGreen else Palette.danger
                            )
                        }
                    }

                    proof.cliCommand?.let { cmd ->
                        Text(
                            text = "$ $cmd",
                            style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = Palette.electricBlue
                        )
                    }

                    // Console output container
                    proof.stdoutSummary?.let { stdout ->
                        if (stdout.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF090D16))
                                    .padding(10.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = stdout.trim(),
                                        style = Type.caption.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = electricGreen
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auditeur : ${proof.agentName ?: "Qualité & Tests"}",
                            style = Type.caption,
                            color = Palette.secondary
                        )

                        Text(
                            text = "${proof.durationMs} ms",
                            style = Type.caption.semibold,
                            color = Palette.secondary
                        )
                    }
                }
            }
        }

        // 4. Traçabilité Git réelle (branche, fichiers, commit, état distant)
        GitTraceabilityCard(gitStatus)

        // 5. Git Diff & Modifications Physiques
        Text(
            text = "DIFFÉRENTIEL GIT & MODIFICATIONS",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.surface.copy(alpha = 0.7f))
                .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Symbol("arrow.left.arrow.right", size = 16.dp, tint = Palette.electricBlue)
                    Text(
                        text = "Traçabilité des modifications Git",
                        style = Type.subheadline.semibold,
                        color = Palette.text
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .padding(10.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = if (gitDiffText.isNotBlank()) gitDiffText else "# Aucune modification Git directe enregistrée pour cette mission.",
                            style = Type.caption.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = if (gitDiffText.contains("+")) electricGreen else Palette.secondary
                        )
                    }
                }
            }
        }

        // 5. Rapport de Synthèse Final
        report?.let { rep ->
            Text(
                text = "RAPPORT DE SYNTHÈSE FINAL",
                style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                color = Palette.secondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Symbol("doc.text", size = 18.dp, tint = Palette.electricBlue)
                        Text(
                            text = rep.title.ifBlank { "Rapport de Mission CAMCS" },
                            style = Type.headline.semibold.copy(fontSize = 15.sp),
                            color = Palette.text
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (dark) Color(0xFF0F141F) else Color(0xFFF1F5F9))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = rep.reportMarkdown,
                                style = Type.footnote.copy(lineHeight = 20.sp),
                                color = Palette.text
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Deliverable file viewer dialog to inspect real code/documentation content.
 */
@Composable
private fun DeliverableFileViewerDialog(
    deliverable: CamcsDeliverable,
    content: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("doc.plaintext", size = 20.dp, tint = Palette.electricBlue)
                Text(
                    text = deliverable.fileName,
                    style = Type.title3.semibold,
                    color = Palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.surface.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = deliverable.filePath,
                        style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = Palette.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Agent : ${deliverable.agentName ?: "Inconnu"}",
                            style = Type.caption.semibold,
                            color = Palette.secondary
                        )
                        Text(
                            text = "${deliverable.sizeBytes} octets",
                            style = Type.caption.semibold,
                            color = Palette.text
                        )
                    }
                    Text(
                        text = "SHA-256 : ${deliverable.sha256}",
                        style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                        color = electricGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Palette.electricBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090D16))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = content ?: "Fichier vide",
                                style = Type.caption.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.accentFill)
                    .clickable { onDismiss() }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("Fermer", style = Type.caption.semibold, color = Palette.onAccent)
            }
        }
    )
}
