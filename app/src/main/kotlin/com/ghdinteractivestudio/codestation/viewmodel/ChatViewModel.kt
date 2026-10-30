package com.ghdinteractivestudio.codestation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.model.ExecutionStep
import com.ghdinteractivestudio.codestation.domain.model.FileAttachment
import com.ghdinteractivestudio.codestation.domain.model.PendingAttachment
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import com.ghdinteractivestudio.codestation.domain.model.StepType
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ChatRepository
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager
import com.ghdinteractivestudio.codestation.notification.TaskNotificationPhase
import com.ghdinteractivestudio.codestation.util.RequestTracer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val bot: Bot? = null,
    val messages: List<ChatMessage> = emptyList(),
    val pendingApprovals: List<ApprovalRequest> = emptyList(),
    val isBotWorking: Boolean = false,
    val currentWorkingSteps: List<ExecutionStep> = emptyList(),
    val inputDraft: String = "",
    val quotedReply: QuotedReply? = null,
    val attachments: List<PendingAttachment> = emptyList(),
    val isVoiceRecording: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val isTranscribing: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val botId: String,
    private val botRepository: BotRepository,
    private val chatRepository: ChatRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _inputDraft = MutableStateFlow("")
    private val _quotedReply = MutableStateFlow<QuotedReply?>(null)
    private val _isBotWorking = MutableStateFlow(false)
    private val _workingSteps = MutableStateFlow<List<ExecutionStep>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _attachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    private val _isVoiceRecording = MutableStateFlow(false)
    private val _voiceDurationSeconds = MutableStateFlow(0)
    private val _isTranscribing = MutableStateFlow(false)

    private var lastSentText: String? = null
    private var lastSentAttachments: List<PendingAttachment> = emptyList()
    private var lastSentMessageId: String? = null
    private var turnTimeoutJob: Job? = null
    private var isSending = false
    private var turnStartTimeMs = 0L
    private var lastBotMsgId: String? = null
    private var lastBotMsgLength: Int = 0

    private fun resetTurnTimeout(timeoutMs: Long, isStreaming: Boolean = false) {
        turnTimeoutJob?.cancel()
        turnTimeoutJob = viewModelScope.launch {
            delay(timeoutMs)
            if (_isBotWorking.value) {
                val currentBot = botRepository.getBotById(botId)
                if (currentBot?.status == BotStatus.WORKING || currentBot?.status == BotStatus.BUSY) {
                    android.util.Log.i("ChatViewModel", "Bot $botId is still reported as ${currentBot.status} on host, extending watchdog by 60s")
                    resetTurnTimeout(60000L, isStreaming = isStreaming)
                    return@launch
                }
                _isBotWorking.value = false
                botRepository.updateBotStatus(botId, BotStatus.IDLE)
                val timeoutSec = timeoutMs / 1000
                val reason = if (isStreaming) {
                    "Délai d'inactivité dépassé pendant la génération (${timeoutSec}s)."
                } else {
                    "Délai d'attente dépassé (${timeoutSec}s). Le modèle ou le serveur n'a pas répondu à temps."
                }
                _errorMessage.value = reason
                RequestTracer.logEvent(
                    requestId = lastSentMessageId ?: "turn-$botId",
                    event = RequestTracer.EVENT_REQUEST_FAILED,
                    botId = botId,
                    details = mapOf("reason" to "turn_timeout_${timeoutSec}s", "isStreaming" to isStreaming)
                )
                val bot = botRepository.getBotById(botId)
                val displayName = CodeStationNotificationManager.formatAgentDisplayName(botId, bot?.name)
                val engine = bot?.provider?.backendId ?: "opencode"
                context?.let { ctx ->
                    CodeStationNotificationManager.updateTaskNotification(
                        context = ctx,
                        botId = botId,
                        botName = displayName,
                        agentBackend = engine,
                        phase = TaskNotificationPhase.FAILED,
                        summary = reason
                    )
                }
                (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                    botId = botId,
                    text = "⏱️ $reason Vérifiez que Termux et le CLI du bot sont actifs, puis appuyez sur Réessayer."
                )
            }
        }
    }

    private data class InputState(
        val draft: String,
        val reply: QuotedReply?,
        val working: Boolean,
        val steps: List<ExecutionStep>,
        val attachments: List<PendingAttachment>,
        val isVoiceRecording: Boolean,
        val voiceDurationSeconds: Int,
        val isTranscribing: Boolean,
        val error: String?
    )

    private val _botDataFlow = combine(
        botRepository.getBotByIdStream(botId),
        chatRepository.getMessagesStream(botId),
        chatRepository.getPendingApprovalsStream(botId)
    ) { bot, messages, approvals ->
        Triple(bot, messages, approvals)
    }

    private val _uiInputFlow = combine(
        _inputDraft,
        _quotedReply,
        _isBotWorking,
        _workingSteps,
        _attachments,
        _isVoiceRecording,
        _voiceDurationSeconds,
        _isTranscribing,
        _errorMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        InputState(
            draft = args[0] as String,
            reply = args[1] as QuotedReply?,
            working = args[2] as Boolean,
            steps = args[3] as List<ExecutionStep>,
            attachments = args[4] as List<PendingAttachment>,
            isVoiceRecording = args[5] as Boolean,
            voiceDurationSeconds = args[6] as Int,
            isTranscribing = args[7] as Boolean,
            error = args[8] as String?
        )
    }

    val uiState: StateFlow<ChatUiState> = combine(
        _botDataFlow,
        _uiInputFlow
    ) { (bot, messages, approvals), input ->
        ChatUiState(
            bot = bot,
            messages = messages,
            pendingApprovals = approvals,
            isBotWorking = input.working || bot?.status == BotStatus.WORKING,
            currentWorkingSteps = input.steps,
            inputDraft = input.draft,
            quotedReply = input.reply,
            attachments = input.attachments,
            isVoiceRecording = input.isVoiceRecording,
            voiceDurationSeconds = input.voiceDurationSeconds,
            isTranscribing = input.isTranscribing,
            errorMessage = input.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        viewModelScope.launch {
            chatRepository.markChatRead(botId)
        }

        // Observer l'état du bot : s'il passe à IDLE, libérer l'état de génération (hors transition de démarrage)
        viewModelScope.launch {
            botRepository.getBotByIdStream(botId).collect { bot ->
                if (bot?.status == BotStatus.IDLE && _isBotWorking.value && (System.currentTimeMillis() - turnStartTimeMs > 1500L)) {
                    turnTimeoutJob?.cancel()
                    turnTimeoutJob = null
                    _isBotWorking.value = false
                }
            }
        }

        // Observer les messages : si une réponse finale du bot arrive, libérer l'état de génération.
        // Pendant le streaming actif (chunks), prolonger dynamiquement le délai d'inactivité.
        viewModelScope.launch {
            chatRepository.getMessagesStream(botId).collect { msgs ->
                val lastBotMsg = msgs.filter { it.author.type == com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType.BOT }.lastOrNull()
                if (lastBotMsg != null) {
                    if (lastBotMsg.isFinal) {
                        turnTimeoutJob?.cancel()
                        turnTimeoutJob = null
                        _isBotWorking.value = false
                        lastBotMsgId = null
                        lastBotMsgLength = 0
                    } else if (_isBotWorking.value) {
                        if (lastBotMsg.id != lastBotMsgId || lastBotMsg.text.length != lastBotMsgLength) {
                            lastBotMsgId = lastBotMsg.id
                            lastBotMsgLength = lastBotMsg.text.length
                            resetTurnTimeout(180000L, isStreaming = true)
                        }
                    }
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _inputDraft.value = text
    }

    fun addAttachment(attachment: PendingAttachment) {
        _attachments.value = _attachments.value + attachment
    }

    fun removeAttachment(attachmentId: String) {
        _attachments.value = _attachments.value.filter { it.id != attachmentId }
    }

    fun clearAttachments() {
        _attachments.value = emptyList()
    }

    fun setVoiceRecording(recording: Boolean, duration: Int = 0) {
        _isVoiceRecording.value = recording
        _voiceDurationSeconds.value = duration
    }

    fun setTranscribing(transcribing: Boolean) {
        _isTranscribing.value = transcribing
    }

    fun onVoiceTranscriptionCompleted(text: String) {
        _isTranscribing.value = false
        if (text.isNotBlank()) {
            _inputDraft.value = if (_inputDraft.value.isBlank()) text else "${_inputDraft.value} $text"
        }
    }

    fun onVoiceTranscriptionFailed(error: String) {
        _isTranscribing.value = false
        _errorMessage.value = error
    }

    fun onReplyTo(message: ChatMessage) {
        _quotedReply.value = QuotedReply(
            messageId = message.id,
            authorName = message.author.displayName,
            textExcerpt = message.text.take(120)
        )
    }

    fun dismissQuotedReply() {
        _quotedReply.value = null
    }

    fun sendMessage() {
        if (isSending) return
        val text = _inputDraft.value.trim()
        val currentAttachments = _attachments.value
        if (text.isBlank() && currentAttachments.isEmpty()) return

        isSending = true
        val reply = _quotedReply.value
        val clientMsgId = java.util.UUID.randomUUID().toString()
        lastSentText = text
        lastSentAttachments = currentAttachments
        lastSentMessageId = clientMsgId
        turnStartTimeMs = System.currentTimeMillis()
        _inputDraft.value = ""
        _quotedReply.value = null
        _attachments.value = emptyList()
        _errorMessage.value = null

        val reqId = clientMsgId

        RequestTracer.logEvent(
            requestId = reqId,
            event = RequestTracer.EVENT_MESSAGE_CREATED,
            botId = botId,
            details = mapOf("length" to text.length, "attachments" to currentAttachments.size)
        )
        RequestTracer.logEvent(
            requestId = reqId,
            event = RequestTracer.EVENT_MESSAGE_QUEUED,
            botId = botId
        )

        viewModelScope.launch {
            try {
                _isBotWorking.value = true
                botRepository.updateBotStatus(botId, BotStatus.WORKING)

                RequestTracer.logEvent(
                    requestId = reqId,
                    event = RequestTracer.EVENT_REQUEST_SENT,
                    botId = botId
                )
                RequestTracer.logEvent(
                    requestId = reqId,
                    event = RequestTracer.EVENT_REQUEST_RECEIVED,
                    botId = botId
                )
                RequestTracer.logEvent(
                    requestId = reqId,
                    event = RequestTracer.EVENT_BOT_RESOLVED,
                    botId = botId
                )
                val bot = botRepository.getBotById(botId)
                val displayName = CodeStationNotificationManager.formatAgentDisplayName(botId, bot?.name)
                val engine = bot?.provider?.backendId ?: "opencode"

                // Déclenchement immédiat de la notification persistante dans la barre d'état
                context?.let { ctx ->
                    CodeStationNotificationManager.updateTaskNotification(
                        context = ctx,
                        botId = botId,
                        botName = displayName,
                        agentBackend = engine,
                        phase = TaskNotificationPhase.WORKING,
                        summary = "Envoi et réflexion en cours...",
                        startedAtMs = System.currentTimeMillis()
                    )
                }

                RequestTracer.logEvent(
                    requestId = reqId,
                    event = RequestTracer.EVENT_CLI_RESOLVED,
                    botId = botId,
                    details = mapOf("engine" to engine, "model" to (bot?.model ?: "default"))
                )
                RequestTracer.logEvent(
                    requestId = reqId,
                    event = RequestTracer.EVENT_PROCESS_STARTED,
                    botId = botId
                )
                RequestTracer.logEvent(
                    requestId = reqId,
                    event = RequestTracer.EVENT_CLI_INPUT_DELIVERED,
                    botId = botId,
                    details = mapOf("hasAttachments" to currentAttachments.isNotEmpty())
                )

                // Démarrer un délai d'attente adaptatif (300s pour la phase initiale de démarrage/réflexion)
                resetTurnTimeout(300000L, isStreaming = false)

                val domainAttachments = currentAttachments.map {
                    FileAttachment(
                        id = it.id,
                        name = it.name,
                        sizeBytes = it.sizeBytes,
                        mimeType = it.mimeType,
                        localUri = it.uri.toString()
                    )
                }

                val result = chatRepository.sendMessage(botId, text, reply, domainAttachments)
                result.onSuccess {
                    RequestTracer.logEvent(
                        requestId = reqId,
                        event = RequestTracer.EVENT_PROVIDER_REQUEST_STARTED,
                        botId = botId
                    )
                }
                result.onFailure { error ->
                    turnTimeoutJob?.cancel()
                    turnTimeoutJob = null
                    _isBotWorking.value = false
                    botRepository.updateBotStatus(botId, BotStatus.IDLE)
                    _errorMessage.value = error.message ?: "Échec de l'envoi au serveur"
                    RequestTracer.logEvent(
                        requestId = reqId,
                        event = RequestTracer.EVENT_REQUEST_FAILED,
                        botId = botId,
                        details = mapOf("error" to error.localizedMessage)
                    )
                    context?.let { ctx ->
                        CodeStationNotificationManager.updateTaskNotification(
                            context = ctx,
                            botId = botId,
                            botName = displayName,
                            agentBackend = engine,
                            phase = TaskNotificationPhase.FAILED,
                            summary = "Erreur de transmission : ${error.localizedMessage}"
                        )
                    }
                    (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                        botId = botId,
                        text = "Erreur de transmission : ${error.localizedMessage}"
                    )
                }
            } finally {
                isSending = false
            }
        }
    }

    fun retryLastMessage() {
        val text = lastSentText ?: return
        _errorMessage.value = null
        _inputDraft.value = text
        sendMessage()
    }

    fun regenerateResponse(botMessageId: String) {
        val currentMessages = uiState.value.messages
        val botIndex = currentMessages.indexOfFirst { it.id == botMessageId }
        val userPrompt = if (botIndex > 0) {
            currentMessages.subList(0, botIndex).lastOrNull { it.author.type == MessageAuthorType.USER }?.text
        } else null

        deleteMessage(botMessageId)

        if (!userPrompt.isNullOrBlank()) {
            _inputDraft.value = userPrompt
            sendMessage()
        } else {
            retryLastMessage()
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun respondApproval(approvalId: String, status: ApprovalStatus, alwaysApprove: Boolean = false) {
        viewModelScope.launch {
            chatRepository.respondApproval(approvalId, status, alwaysApprove)
        }
    }

    fun stopTurn() {
        turnTimeoutJob?.cancel()
        turnTimeoutJob = null
        isSending = false
        _isBotWorking.value = false
        com.ghdinteractivestudio.codestation.util.RequestTracer.logEvent(
            requestId = "cancel-$botId",
            event = com.ghdinteractivestudio.codestation.util.RequestTracer.EVENT_REQUEST_CANCELLED,
            details = mapOf("botId" to botId)
        )
        viewModelScope.launch {
            val bot = botRepository.getBotById(botId)
            val displayName = CodeStationNotificationManager.formatAgentDisplayName(botId, bot?.name)
            context?.let { ctx ->
                CodeStationNotificationManager.updateTaskNotification(
                    context = ctx,
                    botId = botId,
                    botName = displayName,
                    agentBackend = bot?.provider?.backendId ?: "agent",
                    phase = TaskNotificationPhase.STOPPED,
                    summary = "Tâche interrompue par l'utilisateur."
                )
            }
            botRepository.updateBotStatus(botId, BotStatus.IDLE)
            botRepository.stopBotTurn(botId)
            (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                botId = botId,
                text = "⏹️ Génération interrompue par l'utilisateur."
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory(botId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    companion object {
        fun provideFactory(
            botId: String,
            botRepository: BotRepository,
            chatRepository: ChatRepository,
            context: Context? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(botId, botRepository, chatRepository, context) as T
            }
        }
    }
}
