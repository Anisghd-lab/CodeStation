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
import java.util.concurrent.ConcurrentHashMap
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
    private val botPreviousStatus = ConcurrentHashMap<String, String>()

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

                    var line: String? = ""
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

                    val prevStatus = botPreviousStatus[botId] ?: "idle"
                    botPreviousStatus[botId] = rawStatus

                    val mappedStatus = when (rawStatus) {
                        "working" -> BotStatus.WORKING
                        "needs_input" -> BotStatus.WORKING
                        "error" -> BotStatus.OFFLINE
                        else -> BotStatus.IDLE
                    }

                    botDao.updateStatus(botId, mappedStatus.name)

                    val formattedBotName = CodeStationNotificationManager.formatAgentDisplayName(botId, name)
                    val formattedBackend = CodeStationNotificationManager.formatBackendDisplayName(backend)

                    // Gestion rigoureuse des notifications et traçage : uniquement sur changement d'état réel
                    when (rawStatus) {
                        "working" -> {
                            com.ghdinteractivestudio.codestation.util.RequestTracer.logEvent(
                                requestId = "status-$botId",
                                event = com.ghdinteractivestudio.codestation.util.RequestTracer.EVENT_PROCESS_STARTED,
                                details = mapOf("botId" to botId, "backend" to backend, "activity" to activity)
                            )
                            val summary = if (activity.isNotBlank()) activity else "Exécution d'instructions en cours..."
                            CodeStationNotificationManager.updateTaskNotification(
                                context = context,
                                botId = botId,
                                botName = formattedBotName,
                                agentBackend = formattedBackend,
                                phase = TaskNotificationPhase.WORKING,
                                summary = summary,
                                startedAtMs = startedAt ?: System.currentTimeMillis()
                            )
                        }
                        "needs_input" -> {
                            CodeStationNotificationManager.updateTaskNotification(
                                context = context,
                                botId = botId,
                                botName = formattedBotName,
                                agentBackend = formattedBackend,
                                phase = TaskNotificationPhase.NEEDS_APPROVAL,
                                summary = if (activity.isNotBlank()) activity else "Autorisation requise pour continuer"
                            )
                        }
                        "error" -> {
                            com.ghdinteractivestudio.codestation.util.RequestTracer.logEvent(
                                requestId = "status-$botId",
                                event = com.ghdinteractivestudio.codestation.util.RequestTracer.EVENT_REQUEST_FAILED,
                                details = mapOf("botId" to botId, "backend" to backend, "activity" to activity)
                            )
                            CodeStationNotificationManager.updateTaskNotification(
                                context = context,
                                botId = botId,
                                botName = formattedBotName,
                                agentBackend = formattedBackend,
                                phase = TaskNotificationPhase.FAILED,
                                summary = if (activity.isNotBlank()) activity else "Erreur de processus agent"
                            )
                        }
                        "idle" -> {
                            if (prevStatus == "working" || prevStatus == "needs_input") {
                                com.ghdinteractivestudio.codestation.util.RequestTracer.logEvent(
                                    requestId = "status-$botId",
                                    event = com.ghdinteractivestudio.codestation.util.RequestTracer.EVENT_RESPONSE_COMPLETED,
                                    details = mapOf("botId" to botId)
                                )
                                CodeStationNotificationManager.updateTaskNotification(
                                    context = context,
                                    botId = botId,
                                    botName = formattedBotName,
                                    agentBackend = formattedBackend,
                                    phase = TaskNotificationPhase.COMPLETED,
                                    summary = "Réponse terminée avec succès"
                                )
                            } else {
                                // Déjà au repos : ne pas spammer de notification de succès fantôme
                                CodeStationNotificationManager.clearTaskNotification(context, botId)
                            }
                        }
                    }
                }

                "entry" -> {
                    val entryObj = root["entry"]?.jsonObject ?: return
                    val entryId = entryObj["id"]?.jsonPrimitive?.content ?: return
                    val botId = entryObj["botId"]?.jsonPrimitive?.content
                        ?: entryObj["bot_id"]?.jsonPrimitive?.content
                        ?: return
                    val kind = entryObj["kind"]?.jsonPrimitive?.content ?: return
                    val dataObj = entryObj["data"]?.jsonObject ?: return
                    val text = dataObj["text"]?.jsonPrimitive?.content ?: ""
                    val isFinal = dataObj["final"]?.jsonPrimitive?.booleanOrNull ?: false
                    val createdAt = entryObj["createdAt"]?.jsonPrimitive?.longOrNull
                        ?: entryObj["created_at"]?.jsonPrimitive?.longOrNull
                        ?: System.currentTimeMillis()

                    val isAgent = (kind == "agent" || kind == "assistant")
                    val isUser = (kind == "user")
                    val isNotice = (kind == "notice")

                    // Mise à jour de l'historique dans Room (inclut "agent", "assistant", "notice", "user")
                    // On filtre les messages de complétion silencieuse interne comme "(pass)"
                    if (text.isNotBlank() && text.trim() != "(pass)" && (isAgent || isNotice || isUser)) {
                        val authorType = if (isUser) MessageAuthorType.USER else MessageAuthorType.BOT
                        val displayText = if (isNotice && (text.contains("Couldn't select model", ignoreCase = true) || text.contains("Insufficient credits", ignoreCase = true))) {
                            "$text\n\n💡 Modèles gratuits OpenCode disponibles sans crédit :\n• opencode/glm-5.3-flash-free (GLM-5.3-Flash Free)\n• opencode/deepseek-v4.1-flash-free\n• opencode/beam-free"
                        } else text

                        val botName = botDao.getBotById(botId)?.name ?: if (isUser) "Moi" else "CodeStation Agent"

                        val entity = ChatMessageEntity(
                            id = entryId,
                            botId = botId,
                            authorType = authorType.name,
                            authorId = if (isUser) "user" else botId,
                            authorName = if (isUser) "Moi" else botName,
                            text = displayText,
                            timestamp = createdAt,
                            isFinal = isFinal || isNotice || isUser,
                            replyToMessageId = null,
                            replyToAuthor = null,
                            replyToText = null
                        )
                        messageDao.insertMessage(entity)
                        botDao.updateLastMessage(botId, displayText, entity.timestamp)

                        val formattedBotName = CodeStationNotificationManager.formatAgentDisplayName(botId, botName)

                        if (isAgent && isFinal) {
                            com.ghdinteractivestudio.codestation.util.RequestTracer.logEvent(
                                requestId = entryId,
                                event = com.ghdinteractivestudio.codestation.util.RequestTracer.EVENT_RESPONSE_COMPLETED,
                                details = mapOf("botId" to botId, "textLength" to text.length)
                            )
                            botDao.updateStatus(botId, BotStatus.IDLE.name)
                            botPreviousStatus[botId] = "idle"
                            CodeStationNotificationManager.updateTaskNotification(
                                context = context,
                                botId = botId,
                                botName = formattedBotName,
                                agentBackend = "agent",
                                phase = TaskNotificationPhase.COMPLETED,
                                summary = "Réponse terminée avec succès"
                            )
                        } else if (isAgent) {
                            com.ghdinteractivestudio.codestation.util.RequestTracer.logEvent(
                                requestId = entryId,
                                event = com.ghdinteractivestudio.codestation.util.RequestTracer.EVENT_FIRST_OUTPUT_RECEIVED,
                                details = mapOf("botId" to botId, "chunkLength" to text.length)
                            )
                            // Mise à jour de la notification persistante avec l'extrait de streaming
                            CodeStationNotificationManager.updateTaskNotification(
                                context = context,
                                botId = botId,
                                botName = formattedBotName,
                                agentBackend = "agent",
                                phase = TaskNotificationPhase.RESPONSE_RECEIVED,
                                summary = text.trim().takeLast(90).ifBlank { "Réception de la réponse..." }
                            )
                        }
                    }

                    // Détection des demandes d'approbation (cartes de permission)
                    val isApproval = dataObj["approval"]?.jsonPrimitive?.booleanOrNull ?: (kind == "approval" || kind == "permission")
                    val command = dataObj["command"]?.jsonPrimitive?.content
                    if (isApproval || command != null) {
                        val formattedBotName = CodeStationNotificationManager.formatAgentDisplayName(botId, null)
                        val approvalEntity = ApprovalEntity(
                            id = entryId,
                            botId = botId,
                            botName = formattedBotName,
                            actionType = ApprovalActionType.SHELL_COMMAND.name,
                            commandOrPath = command ?: text,
                            diffPreview = dataObj["diff"]?.jsonPrimitive?.content,
                            timestamp = System.currentTimeMillis(),
                            status = ApprovalStatus.PENDING.name,
                            isAlwaysApproved = false
                        )
                        approvalDao.insertApproval(approvalEntity)

                        CodeStationNotificationManager.updateTaskNotification(
                            context = context,
                            botId = botId,
                            botName = formattedBotName,
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
