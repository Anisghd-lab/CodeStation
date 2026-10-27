package com.ghdinteractivestudio.codestation.data.remote

import android.content.Context
import com.ghdinteractivestudio.codestation.data.local.ApprovalDao
import com.ghdinteractivestudio.codestation.data.local.ApprovalEntity
import com.ghdinteractivestudio.codestation.data.local.BotDao
import com.ghdinteractivestudio.codestation.data.local.ChatMessageEntity
import com.ghdinteractivestudio.codestation.data.local.MessageDao
import com.ghdinteractivestudio.codestation.domain.model.ApprovalActionType
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager
import com.ghdinteractivestudio.codestation.notification.TaskNotificationPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class CodeStationEventStreamClient(
    private val context: Context,
    private val serverRepository: ServerRepository,
    private val botDao: BotDao,
    private val messageDao: MessageDao,
    private val approvalDao: ApprovalDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var streamJob: Job? = null
    private var lastRev: Long = 0

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE stream illimité
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    fun start() {
        if (streamJob?.isActive == true) return
        streamJob = scope.launch {
            runEventLoop()
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
    }

    private suspend fun runEventLoop() {
        var backoffMs = 1000L
        while (scope.isActive) {
            try {
                val config = serverRepository.getServerConfig()
                val baseUrl = config.url.trimEnd('/')
                val token = config.token

                if (token.isNullOrBlank()) {
                    delay(3000)
                    continue
                }

                val request = Request.Builder()
                    .url("$baseUrl/events?since=$lastRev")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "text/event-stream")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(15000L)
                        return@use
                    }

                    // Connexion établie, réinitialisation du backoff
                    backoffMs = 1000L
                    val source = response.body?.source() ?: return@use
                    val reader = BufferedReader(source.inputStream().reader())

                    var line: String?
                    while (scope.isActive && reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.startsWith("data:")) {
                            val dataPayload = currentLine.removePrefix("data:").trim()
                            if (dataPayload.isNotEmpty()) {
                                handleServerEvent(dataPayload)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                break
            } catch (_: Exception) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(15000L)
            }
        }
    }

    private suspend fun handleServerEvent(payload: String) {
        try {
            val root = json.parseToJsonElement(payload).jsonObject
            val type = root["type"]?.jsonPrimitive?.content ?: return

            root["rev"]?.jsonPrimitive?.longOrNull?.let { rev ->
                if (rev > lastRev) lastRev = rev
            }

            when (type) {
                "bot" -> {
                    val botObj = root["bot"]?.jsonObject ?: return
                    val botId = botObj["id"]?.jsonPrimitive?.content ?: return
                    val name = botObj["name"]?.jsonPrimitive?.content ?: "Bot"
                    val backend = botObj["backend"]?.jsonPrimitive?.content ?: "agy"
                    val rawStatus = botObj["status"]?.jsonPrimitive?.content ?: "idle"
                    val activity = botObj["activity"]?.jsonPrimitive?.content ?: ""
                    val startedAt = botObj["startedAt"]?.jsonPrimitive?.longOrNull

                    val mappedStatus = when (rawStatus) {
                        "working" -> BotStatus.WORKING
                        "needs_input" -> BotStatus.WORKING // En attente d'approbation ou saisie
                        "error" -> BotStatus.ERROR
                        else -> BotStatus.IDLE
                    }

                    botDao.updateStatus(botId, mappedStatus.name)

                    val notificationPhase = when (rawStatus) {
                        "working" -> TaskNotificationPhase.WORKING
                        "needs_input" -> TaskNotificationPhase.NEEDS_APPROVAL
                        "error" -> TaskNotificationPhase.FAILED
                        "idle" -> TaskNotificationPhase.COMPLETED
                        else -> TaskNotificationPhase.QUEUED
                    }

                    val summary = if (activity.isNotBlank()) activity else when (notificationPhase) {
                        TaskNotificationPhase.WORKING -> "Exécution d'instructions en cours..."
                        TaskNotificationPhase.NEEDS_APPROVAL -> "Une approbation est requise pour continuer"
                        TaskNotificationPhase.COMPLETED -> "Tâche exécutée avec succès"
                        TaskNotificationPhase.FAILED -> "Échec du processus"
                        else -> "Prêt"
                    }

                    CodeStationNotificationManager.updateTaskNotification(
                        context = context,
                        botId = botId,
                        botName = name,
                        agentBackend = backend,
                        phase = notificationPhase,
                        summary = summary,
                        startedAtMs = startedAt
                    )
                }

                "entry" -> {
                    val entryObj = root["entry"]?.jsonObject ?: return
                    val entryId = entryObj["id"]?.jsonPrimitive?.content ?: return
                    val botId = entryObj["bot_id"]?.jsonPrimitive?.content ?: return
                    val kind = entryObj["kind"]?.jsonPrimitive?.content ?: return
                    val dataObj = entryObj["data"]?.jsonObject ?: return
                    val text = dataObj["text"]?.jsonPrimitive?.content ?: ""

                    // Mise à jour de l'historique dans Room
                    if (text.isNotBlank() && (kind == "assistant" || kind == "notice" || kind == "user")) {
                        val authorType = if (kind == "user") MessageAuthorType.USER else MessageAuthorType.BOT
                        val entity = ChatMessageEntity(
                            id = entryId,
                            botId = botId,
                            authorType = authorType.name,
                            authorId = botId,
                            authorName = if (authorType == MessageAuthorType.USER) "Moi" else "CodeStation Agent",
                            text = text,
                            timestamp = System.currentTimeMillis(),
                            isFinal = true,
                            replyToId = null
                        )
                        messageDao.insertMessage(entity)
                        botDao.updateLastMessage(botId, text, entity.timestamp)
                    }

                    // Détection des demandes d'approbation (cartes de permission)
                    val isApproval = dataObj["approval"]?.jsonPrimitive?.booleanOrNull ?: (kind == "approval")
                    val command = dataObj["command"]?.jsonPrimitive?.content
                    if (isApproval || command != null) {
                        val approvalEntity = ApprovalEntity(
                            id = entryId,
                            botId = botId,
                            botName = "CodeStation Agent",
                            actionType = ApprovalActionType.SHELL_COMMAND.name,
                            commandOrPath = command ?: text,
                            diffPreview = dataObj["diff"]?.jsonPrimitive?.content,
                            timestamp = System.currentTimeMillis(),
                            status = ApprovalStatus.PENDING.name,
                            alwaysApprove = false
                        )
                        approvalDao.insertApproval(approvalEntity)

                        CodeStationNotificationManager.updateTaskNotification(
                            context = context,
                            botId = botId,
                            botName = "CodeStation",
                            agentBackend = "acp",
                            phase = TaskNotificationPhase.NEEDS_APPROVAL,
                            summary = "Autorisation requise : ${command ?: text}",
                            approvalId = entryId
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Ignorer les fragments malformés sans casser le flux
        }
    }
}
