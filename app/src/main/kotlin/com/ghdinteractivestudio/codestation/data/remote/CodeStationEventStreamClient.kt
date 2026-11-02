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
import com.ghdinteractivestudio.codestation.domain.model.ExecutionStep
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.model.StepType
import com.ghdinteractivestudio.codestation.domain.repository.ServerRepository
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager
import com.ghdinteractivestudio.codestation.notification.TaskNotificationPhase
import com.ghdinteractivestudio.codestation.util.RequestTracer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.ghdinteractivestudio.codestation.data.local.FileAttachmentDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
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

data class StreamingEvent(
    val botId: String,
    val isStreaming: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class CodeStationEventStreamClient(
    private val context: Context,
    private val serverRepository: ServerRepository,
    private val botDao: BotDao,
    private val messageDao: MessageDao,
    private val approvalDao: ApprovalDao,
    private val conversationDao: com.ghdinteractivestudio.codestation.data.local.ConversationDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "CodeStationSSE"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }
    private val ssePrefs by lazy { context.getSharedPreferences("codestation_sse_prefs", Context.MODE_PRIVATE) }
    private var streamJob: Job? = null
    private var lastRev: Long = 0
    private var initialSyncCompleted = false
    private val botPreviousStatus = ConcurrentHashMap<String, String>()
    private val _streamingFlow = MutableSharedFlow<StreamingEvent>(extraBufferCapacity = 64)
    val streamingFlow: SharedFlow<StreamingEvent> = _streamingFlow.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .readTimeout(35, TimeUnit.SECONDS) // Auto-détection de socket orphelin (KeepAlive hôte = 15s)
        .connectTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun start() {
        if (streamJob?.isActive == true) return
        if (lastRev == 0L) {
            lastRev = ssePrefs.getLong("last_rev", 0L)
        }
        scope.launch {
            try {
                messageDao.purgePollutedSessionNotices()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Notice cleanup failed: ${e.message}")
            }
        }
        android.util.Log.i(TAG, "Starting CodeStationEventStreamClient SSE loop (since=$lastRev)")
        streamJob = scope.launch {
            runEventLoop()
        }
    }

    fun reconnectNow() {
        android.util.Log.i(TAG, "Force reconnecting CodeStationEventStreamClient SSE loop (since=$lastRev)")
        streamJob?.cancel()
        streamJob = scope.launch {
            runEventLoop()
        }
    }

    fun stop() {
        android.util.Log.i(TAG, "Stopping CodeStationEventStreamClient SSE loop")
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
                    android.util.Log.w(TAG, "Token is missing in server config, waiting 3s...")
                    delay(3000)
                    continue
                }

                android.util.Log.i(TAG, "Connecting SSE to $baseUrl/events?since=$lastRev")
                val request = Request.Builder()
                    .url("$baseUrl/events?since=$lastRev")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "text/event-stream")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Accept-Encoding", "identity")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e(TAG, "SSE connection failed with HTTP ${response.code}, retrying in ${backoffMs}ms")
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(15000L)
                        return@use
                    }

                    android.util.Log.i(TAG, "SSE connection established successfully to $baseUrl/events")
                    backoffMs = 1000L
                    val source = response.body?.source() ?: return@use
                    val payloadBuilder = StringBuilder()

                    while (scope.isActive && !source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()

                        if (trimmed.startsWith("data:")) {
                            val dataChunk = trimmed.removePrefix("data:").trim()
                            if (payloadBuilder.isNotEmpty()) {
                                payloadBuilder.append("\n")
                            }
                            payloadBuilder.append(dataChunk)
                        } else if (trimmed.isEmpty() && payloadBuilder.isNotEmpty()) {
                            val fullPayload = payloadBuilder.toString()
                            payloadBuilder.clear()
                            val shouldContinue = handleServerEvent(fullPayload)
                            if (!shouldContinue) {
                                break
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                android.util.Log.i(TAG, "SSE loop cancelled")
                break
            } catch (e: Exception) {
                android.util.Log.e(TAG, "SSE loop network error (${e.javaClass.simpleName}: ${e.message}), retrying in ${backoffMs}ms")
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(15000L)
            }
        }
    }

    private suspend fun handleServerEvent(payload: String): Boolean {
        try {
            val root = json.parseToJsonElement(payload).jsonObject
            val type = root["type"]?.jsonPrimitive?.content ?: return true

            val rev = root["rev"]?.jsonPrimitive?.longOrNull
            if (rev != null && type != "hello" && rev > lastRev) {
                lastRev = rev
                ssePrefs.edit().putLong("last_rev", rev).apply()
            }

            when (type) {
                "resync" -> {
                    android.util.Log.w(TAG, "Server requested resync (SSE lagged). Reconnecting from since=$lastRev")
                    return false
                }
                "hello" -> {
                    android.util.Log.i(TAG, "Handshake hello received: rev=$rev")
                    initialSyncCompleted = true
                }
                "bot" -> {
                    val botObj = root["bot"]?.jsonObject ?: return true
                    val botId = botObj["id"]?.jsonPrimitive?.content ?: return true
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
                    com.ghdinteractivestudio.codestation.widget.CodeStationWidgetManager.updateAllWidgets(context)
                }

                "entry" -> {
                    val entryObj = root["entry"]?.jsonObject ?: return true
                    val entryId = entryObj["id"]?.jsonPrimitive?.content ?: return true
                    val botId = entryObj["botId"]?.jsonPrimitive?.content
                        ?: entryObj["bot_id"]?.jsonPrimitive?.content
                        ?: return true
                    val kind = entryObj["kind"]?.jsonPrimitive?.content ?: return true
                    val dataObj = entryObj["data"]?.jsonObject ?: return true
                    val text = dataObj["text"]?.jsonPrimitive?.content ?: ""
                    val isFinal = dataObj["final"]?.jsonPrimitive?.booleanOrNull ?: false
                    val createdAt = entryObj["createdAt"]?.jsonPrimitive?.longOrNull
                        ?: entryObj["created_at"]?.jsonPrimitive?.longOrNull
                        ?: System.currentTimeMillis()

                    val isAgent = (kind == "agent" || kind == "assistant")
                    val isUser = (kind == "user")
                    val isNotice = (kind == "notice")
                    val isThought = (kind == "thought" || kind == "thinking")
                    val isTool = (kind == "tool" || kind == "tool_call")
                    val isPlan = (kind == "plan" || kind == "planning")

                    // Traçage d'exécution : étapes de raisonnement (Thought / Thinking)
                    if (isThought) {
                        val thoughtText = dataObj["text"]?.jsonPrimitive?.content ?: text
                        if (thoughtText.isNotBlank()) {
                            val step = ExecutionStep(
                                id = entryId.ifBlank { "thought-${System.currentTimeMillis()}" },
                                type = StepType.THINKING,
                                title = "Raisonnement de l'agent",
                                content = thoughtText,
                                isError = false,
                                timestamp = createdAt
                            )
                            RequestTracer.addStep(botId, step)
                            android.util.Log.d(TAG, "Recorded thought trace step for botId=$botId (len=${thoughtText.length})")
                        }
                    }

                    // Traçage d'exécution : appels d'outils et commandes CLI
                    if (isTool) {
                        val toolTitle = dataObj["title"]?.jsonPrimitive?.content ?: "Appel d'outil"
                        val toolStatus = dataObj["status"]?.jsonPrimitive?.content ?: "pending"
                        val toolKind = dataObj["toolKind"]?.jsonPrimitive?.content ?: dataObj["kind"]?.jsonPrimitive?.content ?: ""
                        val toolOutput = dataObj["output"]?.jsonPrimitive?.content ?: ""
                        val toolLocations = dataObj["locations"]?.toString() ?: ""
                        val toolDiffs = dataObj["diffs"]?.toString() ?: ""

                        val toolContent = buildString {
                            if (toolKind.isNotBlank() && toolKind != "other") append("Type: $toolKind\n")
                            append("Statut: $toolStatus")
                            if (toolOutput.isNotBlank()) {
                                append("\n\nSortie:\n$toolOutput")
                            }
                            if (toolLocations.isNotBlank() && toolLocations != "null" && toolLocations != "[]") {
                                append("\n\nEmplacements: $toolLocations")
                            }
                            if (toolDiffs.isNotBlank() && toolDiffs != "null" && toolDiffs != "[]") {
                                append("\n\nDiffs:\n$toolDiffs")
                            }
                        }

                        val step = ExecutionStep(
                            id = entryId.ifBlank { "tool-${System.currentTimeMillis()}" },
                            type = StepType.TOOL_CALL,
                            title = "$toolTitle ($toolStatus)",
                            content = toolContent.ifBlank { "Appel d'outil sans détails supplémentaires" },
                            isError = (toolStatus == "error" || toolStatus == "failed"),
                            timestamp = createdAt
                        )
                        RequestTracer.addStep(botId, step)
                        android.util.Log.d(TAG, "Recorded tool trace step for botId=$botId: $toolTitle ($toolStatus)")
                    }

                    // Traçage d'exécution : plan d'action
                    if (isPlan) {
                        val planTitle = dataObj["title"]?.jsonPrimitive?.content ?: "Plan d'action"
                        val planText = dataObj["text"]?.jsonPrimitive?.content ?: dataObj["plan"]?.toString() ?: text
                        if (planText.isNotBlank()) {
                            val step = ExecutionStep(
                                id = entryId.ifBlank { "plan-${System.currentTimeMillis()}" },
                                type = StepType.PLANNING,
                                title = planTitle,
                                content = planText,
                                isError = false,
                                timestamp = createdAt
                            )
                            RequestTracer.addStep(botId, step)
                            android.util.Log.d(TAG, "Recorded plan trace step for botId=$botId: $planTitle")
                        }
                    }

                    // Extraction exhaustive des pièces jointes, fichiers générés et artefacts
                    val parsedAttachments = mutableListOf<FileAttachmentDto>()

                    // 1. Pièces jointes utilisateur
                    dataObj["attachments"]?.jsonArray?.forEach { item ->
                        val obj = item.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: ""
                        val name = obj["name"]?.jsonPrimitive?.content ?: "fichier"
                        val size = obj["size"]?.jsonPrimitive?.longOrNull ?: 0L
                        if (id.isNotBlank()) {
                            parsedAttachments.add(FileAttachmentDto(id, name, size, detectMimeType(name)))
                        }
                    }

                    // 2. Fichiers générés par l'agent (send_file ou détection auto workspace)
                    dataObj["files"]?.jsonArray?.forEach { item ->
                        val obj = item.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: ""
                        val name = obj["name"]?.jsonPrimitive?.content ?: "fichier"
                        val size = obj["size"]?.jsonPrimitive?.longOrNull ?: 0L
                        if (id.isNotBlank()) {
                            parsedAttachments.add(FileAttachmentDto(id, name, size, detectMimeType(name)))
                        }
                    }

                    // 3. Artefacts interactifs publiés
                    dataObj["artifact"]?.jsonObject?.let { artObj ->
                        val artId = artObj["id"]?.jsonPrimitive?.content ?: ""
                        val title = artObj["title"]?.jsonPrimitive?.content ?: "Artifact"
                        if (artId.isNotBlank()) {
                            parsedAttachments.add(
                                FileAttachmentDto(
                                    id = artId,
                                    name = "$title.html",
                                    sizeBytes = 0L,
                                    mimeType = "text/html",
                                    isArtifact = true,
                                    artifactTitle = title
                                )
                            )
                        }
                    }

                    val attachmentsJsonStr = if (parsedAttachments.isNotEmpty()) {
                        try {
                            Json.encodeToString(parsedAttachments)
                        } catch (_: Exception) { null }
                    } else null

                    // Filtrage des événements de cycle de vie et de session internes
                    val isInternalNotice = isNotice && (
                        dataObj.containsKey("handoff") ||
                        text.startsWith("Started a fresh conversation", ignoreCase = true) ||
                        text.startsWith("Kept this conversation", ignoreCase = true) ||
                        text.contains("carrying this summary", ignoreCase = true) ||
                        text.contains("This conversation is long and has gone quiet", ignoreCase = true) ||
                        text.contains("You've been away a while", ignoreCase = true) ||
                        text.contains("Summarizing this conversation no longer frees enough room", ignoreCase = true) ||
                        text.trim() == "RECONNECT_OK" ||
                        text.trim() == "RECOVERY_SUCCESS"
                    )

                    val isSyntheticHarnessEcho = (isUser || isAgent) && (
                        text.trim() == "Reponds uniquement: RECONNECT_OK" ||
                        text.trim() == "Reponds exactement: RECOVERY_SUCCESS" ||
                        text.trim() == "RECONNECT_OK" ||
                        text.trim() == "RECOVERY_SUCCESS"
                    )

                    if (isInternalNotice || isSyntheticHarnessEcho) {
                        android.util.Log.i(TAG, "Internal session/recovery event (diagnostic only, not saved to chat): botId=$botId text=$text")
                        RequestTracer.logEvent(
                            requestId = entryId,
                            event = "internal_session_event",
                            botId = botId,
                            details = mapOf("text" to text, "kind" to kind, "isInternalNotice" to isInternalNotice)
                        )
                        return true
                    }

                    // Mise à jour de l'historique dans Room (inclut "agent", "assistant", notices utilisateur, "user")
                    // On filtre les messages de complétion silencieuse interne comme "(pass)"
                    if ((text.isNotBlank() || parsedAttachments.isNotEmpty()) && text.trim() != "(pass)" && (isAgent || isNotice || isUser)) {
                        val authorType = when {
                            isUser -> MessageAuthorType.USER
                            isNotice -> MessageAuthorType.SYSTEM
                            else -> MessageAuthorType.BOT
                        }
                        val effectiveText = if (text.isBlank() && parsedAttachments.isNotEmpty()) {
                            parsedAttachments.first().name
                        } else text
                        val displayText = if (isNotice && (effectiveText.contains("Couldn't select model", ignoreCase = true) || effectiveText.contains("Insufficient credits", ignoreCase = true))) {
                            "$effectiveText\n\n💡 Modèles gratuits OpenCode disponibles sans crédit :\n• opencode/glm-5.3-flash-free (GLM-5.3-Flash Free)\n• opencode/deepseek-v4.1-flash-free\n• opencode/beam-free"
                        } else effectiveText

                        val botName = botDao.getBotById(botId)?.name ?: if (isUser) "Moi" else "CodeStation Agent"
                        val authorName = when {
                            isUser -> "Moi"
                            isNotice -> "Système"
                            else -> botName
                        }
                        val authorId = when {
                            isUser -> "user"
                            isNotice -> "system"
                            else -> botId
                        }

                        val conversationId = dataObj["conversationId"]?.jsonPrimitive?.content.orEmpty()
                        val entity = ChatMessageEntity(
                            id = entryId,
                            botId = botId,
                            authorType = authorType.name,
                            authorId = authorId,
                            authorName = authorName,
                            text = displayText,
                            timestamp = createdAt,
                            isFinal = isFinal || isNotice || isUser,
                            replyToMessageId = null,
                            replyToAuthor = null,
                            replyToText = null,
                            attachmentsJson = attachmentsJsonStr,
                            conversationId = conversationId
                        )
                        if (isUser) {
                            val clientNonce = dataObj["clientNonce"]?.jsonPrimitive?.content
                                ?: dataObj["client_nonce"]?.jsonPrimitive?.content
                            messageDao.reconcileUserMessage(clientNonce, entity)
                        } else {
                            messageDao.insertMessage(entity)
                        }
                        // Ne mettre à jour le dernier message que pour les vraies réponses d'agent ou de l'utilisateur
                        if ((isFinal && isAgent) || isUser) {
                            botDao.updateLastMessage(botId, displayText, entity.timestamp)
                            if (conversationId.isNotBlank()) {
                                conversationDao?.updateLastMessage(conversationId, displayText, entity.timestamp)
                            }
                        }
                        android.util.Log.i(TAG, "Saved chat message id=$entryId botId=$botId isFinal=$isFinal author=$authorType textLen=${text.length}")

                        val formattedBotName = CodeStationNotificationManager.formatAgentDisplayName(botId, botName)

                        if (isAgent && isFinal) {
                            RequestTracer.logEvent(
                                requestId = entryId,
                                event = RequestTracer.EVENT_RESPONSE_COMPLETED,
                                botId = botId,
                                details = mapOf("botId" to botId, "textLength" to text.length)
                            )
                            _streamingFlow.tryEmit(StreamingEvent(botId, isStreaming = false))
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
                            RequestTracer.logEvent(
                                requestId = entryId,
                                event = RequestTracer.EVENT_FIRST_OUTPUT_RECEIVED,
                                botId = botId,
                                details = mapOf("botId" to botId, "chunkLength" to text.length)
                            )
                            _streamingFlow.tryEmit(StreamingEvent(botId, isStreaming = true))
                            botDao.updateStatus(botId, BotStatus.WORKING.name)
                            botPreviousStatus[botId] = "working"
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
                        com.ghdinteractivestudio.codestation.widget.CodeStationWidgetManager.updateAllWidgets(context)

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
            return true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse/process event payload: ${e.message}", e)
            return true
        }
    }

    private fun detectMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }
}
