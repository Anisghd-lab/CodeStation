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
import com.ghdinteractivestudio.codestation.domain.model.Conversation
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

enum class TurnState {
    IDLE,
    STARTING,
    RUNNING
}

enum class TurnTerminationReason {
    COMPLETED,
    CANCELLED,
    ERROR,
    EXPIRED
}

data class ChatUiState(
    val bot: Bot? = null,
    val messages: List<ChatMessage> = emptyList(),
    val pendingApprovals: List<ApprovalRequest> = emptyList(),
    val isBotWorking: Boolean = false,
    val currentWorkingSteps: List<ExecutionStep> = emptyList(),
    val inputDraft: String = "",
    val editingMessage: ChatMessage? = null,
    val quotedReply: QuotedReply? = null,
    val attachments: List<PendingAttachment> = emptyList(),
    val isVoiceRecording: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val isTranscribing: Boolean = false,
    val errorMessage: String? = null,
    val serverUrl: String = "http://127.0.0.1:19222",
    val serverToken: String = "",
    val activeConversationId: String? = null,
    val activeConversationTitle: String = "Discussion",
    val conversations: List<Conversation> = emptyList(),
    val generationElapsedSeconds: Int = 0,
    val queuedMessagesCount: Int = 0
)

class ChatViewModel(
    private val botId: String,
    private val botRepository: BotRepository,
    private val chatRepository: ChatRepository,
    private val context: Context? = null,
    private val initialConversationId: String? = null
) : ViewModel() {

    private val _activeConversationId = MutableStateFlow<String?>(initialConversationId)
    val activeConversationId = _activeConversationId.asStateFlow()

    val conversations: StateFlow<List<Conversation>> =
        chatRepository.getConversationsStream(botId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _messagesFlow = _activeConversationId.flatMapLatest { convId ->
        chatRepository.getMessagesByConversationStream(botId, convId)
    }

    private val _inputDraft = MutableStateFlow("")
    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    private val _quotedReply = MutableStateFlow<QuotedReply?>(null)
    private val _isBotWorking = MutableStateFlow(false)
    private val _workingSteps = MutableStateFlow<List<ExecutionStep>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _attachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    private val _isVoiceRecording = MutableStateFlow(false)
    private val _voiceDurationSeconds = MutableStateFlow(0)
    private val _isTranscribing = MutableStateFlow(false)
    private val _serverUrl = MutableStateFlow("http://127.0.0.1:19222")
    private val _serverToken = MutableStateFlow("")
    private val _generationElapsedSeconds = MutableStateFlow(0)
    private val _queuedMessagesCount = MutableStateFlow(0)

    private var lastSentText: String? = null
    private var lastSentAttachments: List<PendingAttachment> = emptyList()
    private var lastSentMessageId: String? = null
    private var turnTimeoutJob: Job? = null
    private var timerJob: Job? = null
    private var turnStartNano: Long = 0L
    private var isSending = false
    private var turnStartTimeMs = 0L
    private var activeTurnStartTimeMs = 0L
    private var activeTurnId: String? = null
    private var activeTurnConversationId: String? = null
    private var activeTurnState: TurnState = TurnState.IDLE
    private var turnExplicitlyFinished: Boolean = false
    private var lastBotMsgId: String? = null
    private var lastBotMsgLength: Int = 0

    private data class QueuedMessage(
        val clientRequestId: String,
        val text: String,
        val attachments: List<PendingAttachment>,
        val reply: QuotedReply?,
        val conversationId: String?
    )

    private val outgoingQueue = ConcurrentLinkedQueue<QueuedMessage>()

    private fun startTimer() {
        timerJob?.cancel()
        turnStartNano = System.nanoTime()
        _generationElapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500L)
                val elapsedSec = ((System.nanoTime() - turnStartNano) / 1_000_000_000L).toInt()
                _generationElapsedSeconds.value = elapsedSec
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateNotification(phase: TaskNotificationPhase, summary: String) {
        context?.let { ctx ->
            viewModelScope.launch {
                val bot = botRepository.getBotById(botId)
                val displayName = CodeStationNotificationManager.formatAgentDisplayName(botId, bot?.name)
                val engine = bot?.provider?.backendId ?: "opencode"
                CodeStationNotificationManager.updateTaskNotification(
                    context = ctx,
                    botId = botId,
                    botName = displayName,
                    agentBackend = engine,
                    phase = phase,
                    summary = summary
                )
            }
        }
    }

    private fun finishTurn(
        reason: TurnTerminationReason,
        errorDetails: String? = null,
        expectedTurnId: String? = null
    ) {
        if (expectedTurnId != null && activeTurnId != null && activeTurnId != expectedTurnId) {
            return
        }
        if (activeTurnState == TurnState.IDLE && !_isBotWorking.value) {
            return
        }

        val closedTurnId = activeTurnId
        activeTurnState = TurnState.IDLE
        activeTurnId = null
        activeTurnConversationId = null
        turnExplicitlyFinished = true
        isSending = false

        stopTimer()
        turnTimeoutJob?.cancel()
        turnTimeoutJob = null
        _isBotWorking.value = false

        viewModelScope.launch {
            botRepository.updateBotStatus(botId, BotStatus.IDLE)
        }

        when (reason) {
            TurnTerminationReason.COMPLETED -> {
                RequestTracer.logEvent(
                    requestId = closedTurnId ?: "turn-$botId",
                    event = RequestTracer.EVENT_RESPONSE_COMPLETED,
                    botId = botId
                )
                updateNotification(TaskNotificationPhase.COMPLETED, "Réponse terminée.")
                dispatchNextQueuedMessage()
            }
            TurnTerminationReason.CANCELLED -> {
                RequestTracer.logEvent(
                    requestId = closedTurnId ?: "turn-$botId",
                    event = RequestTracer.EVENT_REQUEST_CANCELLED,
                    botId = botId
                )
                updateNotification(TaskNotificationPhase.STOPPED, "Tâche interrompue par l'utilisateur.")
                outgoingQueue.clear()
                _queuedMessagesCount.value = 0
            }
            TurnTerminationReason.ERROR -> {
                _errorMessage.value = errorDetails ?: "Erreur de transmission"
                RequestTracer.logEvent(
                    requestId = closedTurnId ?: "turn-$botId",
                    event = RequestTracer.EVENT_REQUEST_FAILED,
                    botId = botId,
                    details = mapOf("error" to (errorDetails ?: "unknown"))
                )
                updateNotification(TaskNotificationPhase.FAILED, errorDetails ?: "Erreur de transmission")
            }
            TurnTerminationReason.EXPIRED -> {
                val reasonText = errorDetails ?: "Délai d'attente dépassé"
                _errorMessage.value = reasonText
                RequestTracer.logEvent(
                    requestId = closedTurnId ?: "turn-$botId",
                    event = RequestTracer.EVENT_REQUEST_FAILED,
                    botId = botId,
                    details = mapOf("reason" to "turn_timeout")
                )
                updateNotification(TaskNotificationPhase.FAILED, reasonText)
            }
        }
    }

    private fun dispatchNextQueuedMessage() {
        val nextMsg = outgoingQueue.poll() ?: return
        _queuedMessagesCount.value = outgoingQueue.size
        executeSendMessage(
            text = nextMsg.text,
            attachments = nextMsg.attachments,
            reply = nextMsg.reply,
            clientRequestId = nextMsg.clientRequestId,
            targetConversationId = nextMsg.conversationId
        )
    }

    private fun resetTurnTimeout(timeoutMs: Long, isStreaming: Boolean = false, expectedTurnId: String? = null) {
        turnTimeoutJob?.cancel()
        val turnId = expectedTurnId ?: activeTurnId
        turnTimeoutJob = viewModelScope.launch {
            delay(timeoutMs)
            if (_isBotWorking.value && (turnId == null || activeTurnId == turnId)) {
                val currentBot = botRepository.getBotById(botId)
                if (currentBot?.status == BotStatus.WORKING || currentBot?.status == BotStatus.BUSY) {
                    android.util.Log.i("ChatViewModel", "Bot $botId is still reported as ${currentBot.status} on host, extending watchdog by 60s")
                    resetTurnTimeout(60000L, isStreaming = isStreaming, expectedTurnId = turnId)
                    return@launch
                }
                val timeoutSec = timeoutMs / 1000
                val reason = if (isStreaming) {
                    "Délai d'inactivité dépassé pendant la génération (${timeoutSec}s)."
                } else {
                    "Délai d'attente dépassé (${timeoutSec}s). Le modèle ou le serveur n'a pas répondu à temps."
                }
                finishTurn(
                    reason = TurnTerminationReason.EXPIRED,
                    errorDetails = reason,
                    expectedTurnId = turnId
                )
                (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                    botId = botId,
                    text = "$reason Vérifiez que Termux et le CLI du bot sont actifs, puis appuyez sur Réessayer."
                )
            }
        }
    }

    private data class InputState(
        val draft: String,
        val editing: ChatMessage?,
        val reply: QuotedReply?,
        val working: Boolean,
        val steps: List<ExecutionStep>,
        val attachments: List<PendingAttachment>,
        val isVoiceRecording: Boolean,
        val voiceDurationSeconds: Int,
        val isTranscribing: Boolean,
        val error: String?,
        val generationElapsedSeconds: Int,
        val queuedMessagesCount: Int
    )

    private val _botDataFlow = combine(
        botRepository.getBotByIdStream(botId),
        _messagesFlow,
        chatRepository.getPendingApprovalsStream(botId)
    ) { bot, messages, approvals ->
        Triple(bot, messages, approvals)
    }

    private val _uiInputFlow = combine(
        _inputDraft,
        _editingMessage,
        _quotedReply,
        _isBotWorking,
        _workingSteps,
        _attachments,
        _isVoiceRecording,
        _voiceDurationSeconds,
        _isTranscribing,
        _errorMessage,
        _generationElapsedSeconds,
        _queuedMessagesCount
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        InputState(
            draft = args[0] as String,
            editing = args[1] as ChatMessage?,
            reply = args[2] as QuotedReply?,
            working = args[3] as Boolean,
            steps = args[4] as List<ExecutionStep>,
            attachments = args[5] as List<PendingAttachment>,
            isVoiceRecording = args[6] as Boolean,
            voiceDurationSeconds = args[7] as Int,
            isTranscribing = args[8] as Boolean,
            error = args[9] as String?,
            generationElapsedSeconds = args[10] as Int,
            queuedMessagesCount = args[11] as Int
        )
    }

    val uiState: StateFlow<ChatUiState> = combine(
        _botDataFlow,
        _uiInputFlow,
        _serverUrl,
        _serverToken,
        _activeConversationId,
        conversations
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val triple = args[0] as Triple<Bot?, List<ChatMessage>, List<ApprovalRequest>>
        val (bot, messages, approvals) = triple
        val input = args[1] as InputState
        val sUrl = args[2] as String
        val sToken = args[3] as String
        val activeConvId = args[4] as String?
        @Suppress("UNCHECKED_CAST")
        val convList = args[5] as List<Conversation>
        val activeConv = convList.find { it.id == activeConvId }
        val activeTitle = when {
            activeConv != null -> activeConv.title.ifBlank { "Discussion sans titre" }
            !activeConvId.isNullOrBlank() -> "Nouvelle discussion"
            convList.isNotEmpty() -> convList.first().title.ifBlank { "Discussion sans titre" }
            else -> "Discussion"
        }

        val effectiveWorking = if (turnExplicitlyFinished && !input.working) {
            false
        } else {
            input.working || bot?.status == BotStatus.WORKING
        }

        ChatUiState(
            bot = bot,
            messages = messages,
            pendingApprovals = approvals,
            isBotWorking = effectiveWorking,
            currentWorkingSteps = input.steps,
            inputDraft = input.draft,
            editingMessage = input.editing,
            quotedReply = input.reply,
            attachments = input.attachments,
            isVoiceRecording = input.isVoiceRecording,
            voiceDurationSeconds = input.voiceDurationSeconds,
            isTranscribing = input.isTranscribing,
            errorMessage = input.error,
            serverUrl = sUrl,
            serverToken = sToken,
            activeConversationId = activeConvId,
            activeConversationTitle = activeTitle,
            conversations = convList,
            generationElapsedSeconds = input.generationElapsedSeconds,
            queuedMessagesCount = input.queuedMessagesCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        val serverRepo = context?.let {
            (it.applicationContext as? com.ghdinteractivestudio.codestation.CodeStationApplication)?.container?.serverRepository
        }
        viewModelScope.launch {
            serverRepo?.getServerConfigStream()?.collect { cfg ->
                _serverUrl.value = cfg.url.ifBlank { "http://127.0.0.1:19222" }
                _serverToken.value = cfg.token ?: ""
            }
        }

        _workingSteps.value = RequestTracer.getSteps(botId)
        viewModelScope.launch {
            RequestTracer.getStepsFlow(botId).collect { steps ->
                _workingSteps.value = steps
            }
        }

        viewModelScope.launch {
            chatRepository.markChatRead(botId)
        }

        // Si aucune conversation spécifique n'a été demandée, initialiser avec la plus récente
        if (initialConversationId == null) {
            viewModelScope.launch {
                chatRepository.getConversationsStream(botId).collect { list ->
                    if (_activeConversationId.value == null && list.isNotEmpty()) {
                        _activeConversationId.value = list.first().id
                    }
                }
            }
        }

        // Observer l'état du bot : s'il passe à IDLE ou OFFLINE alors qu'on est en état RUNNING, clore le tour
        viewModelScope.launch {
            botRepository.getBotByIdStream(botId).collect { bot ->
                if ((bot?.status == BotStatus.IDLE || bot?.status == BotStatus.OFFLINE) &&
                    _isBotWorking.value &&
                    activeTurnState == TurnState.RUNNING
                ) {
                    finishTurn(TurnTerminationReason.COMPLETED)
                }
            }
        }

        // Observer les messages : si une réponse finale du bot arrive pour la conversation et le tour en cours, clore le tour.
        // Pendant le streaming actif (chunks), prolonger dynamiquement le délai d'inactivité.
        viewModelScope.launch {
            chatRepository.getMessagesStream(botId).collect { msgs ->
                val currentConvId = _activeConversationId.value
                val lastBotMsg = msgs
                    .filter { it.author.type == MessageAuthorType.BOT }
                    .filter { currentConvId == null || it.conversationId == currentConvId }
                    .lastOrNull()

                if (lastBotMsg != null) {
                    val isCurrentTurnMsg = lastBotMsg.timestamp >= (turnStartTimeMs - 5000L)
                    if (lastBotMsg.isFinal && _isBotWorking.value && isCurrentTurnMsg) {
                        finishTurn(TurnTerminationReason.COMPLETED)
                        lastBotMsgId = null
                        lastBotMsgLength = 0
                    } else if (_isBotWorking.value && isCurrentTurnMsg) {
                        if (lastBotMsg.id != lastBotMsgId || lastBotMsg.text.length != lastBotMsgLength) {
                            lastBotMsgId = lastBotMsg.id
                            lastBotMsgLength = lastBotMsg.text.length
                            resetTurnTimeout(180000L, isStreaming = true, expectedTurnId = activeTurnId)
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

    fun startEditingMessage(message: ChatMessage) {
        _editingMessage.value = message
        _inputDraft.value = message.text
    }

    fun cancelEditingMessage() {
        _editingMessage.value = null
        _inputDraft.value = ""
    }

    fun confirmEditAndSend(newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isBlank() && _attachments.value.isEmpty()) return
        _editingMessage.value = null
        _inputDraft.value = trimmed
        sendMessage()
    }

    private suspend fun ensureActiveConversation(text: String, currentAttachments: List<PendingAttachment>): String? {
        var currentConvId = _activeConversationId.value
        val existingConv = if (!currentConvId.isNullOrBlank()) {
            chatRepository.getConversationById(currentConvId)
        } else null

        if (currentConvId.isNullOrBlank() || existingConv == null) {
            val smartTitle = generateConversationTitle(text, currentAttachments.map { it.name })
            val newConv = if (currentConvId.isNullOrBlank()) {
                chatRepository.createConversation(botId, smartTitle).getOrNull()
            } else {
                chatRepository.createConversation(botId, smartTitle, null, null, currentConvId).getOrNull()
            }
            currentConvId = newConv?.id ?: currentConvId
            _activeConversationId.value = currentConvId
        } else {
            if (isGenericConversationTitle(existingConv.title) || existingConv.messageCount == 0) {
                val smartTitle = generateConversationTitle(text, currentAttachments.map { it.name })
                chatRepository.renameConversation(existingConv.id, smartTitle)
            }
        }
        return currentConvId
    }

    private suspend fun enqueueMessage(
        clientRequestId: String,
        text: String,
        attachments: List<PendingAttachment>,
        reply: QuotedReply?,
        conversationId: String?
    ) {
        val domainAttachments = attachments.map {
            FileAttachment(
                id = it.id,
                name = it.name,
                sizeBytes = it.sizeBytes,
                mimeType = it.mimeType,
                localUri = it.uri.toString()
            )
        }

        RequestTracer.logEvent(
            requestId = clientRequestId,
            event = RequestTracer.EVENT_MESSAGE_QUEUED,
            botId = botId,
            details = mapOf("length" to text.length, "attachments" to attachments.size)
        )

        outgoingQueue.add(
            QueuedMessage(
                clientRequestId = clientRequestId,
                text = text,
                attachments = attachments,
                reply = reply,
                conversationId = conversationId
            )
        )
        _queuedMessagesCount.value = outgoingQueue.size

        // Inscription locale immédiate en base Room avec isQueued = true (visible avec badge • En attente)
        chatRepository.sendMessage(
            botId = botId,
            text = text,
            replyTo = reply,
            attachments = domainAttachments,
            conversationId = conversationId,
            clientRequestId = clientRequestId,
            isQueued = true
        )
    }

    private fun executeSendMessage(
        text: String,
        attachments: List<PendingAttachment>,
        reply: QuotedReply?,
        clientRequestId: String,
        targetConversationId: String?
    ) {
        isSending = true
        val turnId = UUID.randomUUID().toString()
        activeTurnId = turnId
        activeTurnConversationId = targetConversationId
        activeTurnStartTimeMs = System.currentTimeMillis()
        turnStartTimeMs = activeTurnStartTimeMs
        activeTurnState = TurnState.STARTING
        turnExplicitlyFinished = false

        lastSentText = text
        lastSentAttachments = attachments
        lastSentMessageId = clientRequestId

        _isBotWorking.value = true
        startTimer()

        RequestTracer.logEvent(
            requestId = clientRequestId,
            event = RequestTracer.EVENT_MESSAGE_CREATED,
            botId = botId,
            details = mapOf("length" to text.length, "attachments" to attachments.size)
        )

        viewModelScope.launch {
            try {
                botRepository.updateBotStatus(botId, BotStatus.WORKING)

                val bot = botRepository.getBotById(botId)
                val displayName = CodeStationNotificationManager.formatAgentDisplayName(botId, bot?.name)
                val engine = bot?.provider?.backendId ?: "opencode"

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

                resetTurnTimeout(300000L, isStreaming = false, expectedTurnId = turnId)

                val domainAttachments = attachments.map {
                    FileAttachment(
                        id = it.id,
                        name = it.name,
                        sizeBytes = it.sizeBytes,
                        mimeType = it.mimeType,
                        localUri = it.uri.toString()
                    )
                }

                val result = chatRepository.sendMessage(
                    botId = botId,
                    text = text,
                    replyTo = reply,
                    attachments = domainAttachments,
                    conversationId = targetConversationId,
                    clientRequestId = clientRequestId,
                    isQueued = false
                )

                result.onSuccess {
                    if (activeTurnId == turnId) {
                        activeTurnState = TurnState.RUNNING
                    }
                    RequestTracer.logEvent(
                        requestId = clientRequestId,
                        event = RequestTracer.EVENT_PROVIDER_REQUEST_STARTED,
                        botId = botId
                    )
                }

                result.onFailure { error ->
                    finishTurn(
                        reason = TurnTerminationReason.ERROR,
                        errorDetails = error.message ?: "Échec de l'envoi au serveur",
                        expectedTurnId = turnId
                    )
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

    fun sendMessage() {
        val text = _inputDraft.value.trim()
        val currentAttachments = _attachments.value
        if (text.isBlank() && currentAttachments.isEmpty()) return

        val reply = _quotedReply.value
        val clientMsgId = UUID.randomUUID().toString()

        // Vider la saisie immédiatement car l'action d'envoi volontaire est validée
        _inputDraft.value = ""
        _editingMessage.value = null
        _quotedReply.value = null
        _attachments.value = emptyList()
        _errorMessage.value = null

        viewModelScope.launch {
            val convId = ensureActiveConversation(text, currentAttachments)

            if (_isBotWorking.value || activeTurnState != TurnState.IDLE || isSending) {
                enqueueMessage(
                    clientRequestId = clientMsgId,
                    text = text,
                    attachments = currentAttachments,
                    reply = reply,
                    conversationId = convId
                )
            } else {
                executeSendMessage(
                    text = text,
                    attachments = currentAttachments,
                    reply = reply,
                    clientRequestId = clientMsgId,
                    targetConversationId = convId
                )
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
        if (_isBotWorking.value) return
        val currentMessages = uiState.value.messages
        val botIndex = currentMessages.indexOfFirst { it.id == botMessageId }
        val userMsg = if (botIndex >= 0) {
            currentMessages.subList(0, botIndex).lastOrNull { it.author.type == MessageAuthorType.USER }
        } else null

        if (userMsg != null && userMsg.text.isNotBlank()) {
            _errorMessage.value = null
            _inputDraft.value = userMsg.text
            if (userMsg.attachments.isNotEmpty()) {
                val pending = userMsg.attachments.map { att ->
                    PendingAttachment(
                        id = java.util.UUID.randomUUID().toString(),
                        uri = android.net.Uri.parse(att.localUri ?: ""),
                        name = att.name,
                        mimeType = att.mimeType,
                        sizeBytes = att.sizeBytes,
                        isImage = att.mimeType.startsWith("image/"),
                        isVideo = att.mimeType.startsWith("video/"),
                        isAudio = att.mimeType.startsWith("audio/")
                    )
                }
                _attachments.value = pending
            }
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
        finishTurn(TurnTerminationReason.CANCELLED)
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
            botRepository.stopBotTurn(botId)
            (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                botId = botId,
                text = "Génération interrompue par l'utilisateur."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        turnTimeoutJob?.cancel()
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory(botId)
            RequestTracer.clearSteps(botId)
            _workingSteps.value = emptyList()
        }
    }

    fun clearTraceSteps() {
        RequestTracer.clearSteps(botId)
        _workingSteps.value = emptyList()
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
    }

    fun createNewConversation(title: String = "Nouvelle discussion") {
        viewModelScope.launch {
            val res = chatRepository.createConversation(botId, title)
            res.onSuccess { conv ->
                _activeConversationId.value = conv.id
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_activeConversationId.value == id) {
                _activeConversationId.value = null
            }
        }
    }

    fun renameConversation(id: String, title: String) {
        viewModelScope.launch {
            chatRepository.renameConversation(id, title)
        }
    }

    companion object {
        fun isGenericConversationTitle(title: String?): Boolean {
            if (title.isNullOrBlank()) return true
            val normalized = title.trim().lowercase()
            return normalized in setOf(
                "discussion",
                "discussions",
                "nouvelle discussion",
                "discussion principale",
                "discussion sans titre",
                "nouvelle session",
                "new chat",
                "new conversation",
                "untitled",
                "sans titre"
            )
        }

        fun generateConversationTitle(messageText: String, attachmentNames: List<String> = emptyList()): String {
            val clean = messageText
                .trim()
                .replace(Regex("^/[a-zA-Z0-9_-]+\\s*"), "") // strip slash command e.g. /task, /fix
                .replace(Regex("^@[a-zA-Z0-9_.-]+\\s*"), "") // strip bot mentions e.g. @codebuddy
                .replace(Regex("[#*`_~>\\[\\]]"), "")       // strip markdown symbols
                .replace(Regex("\\s+"), " ")                // normalize whitespace
                .trim()

            if (clean.isNotBlank()) {
                val maxLen = 42
                return if (clean.length <= maxLen) {
                    clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    val truncated = clean.take(maxLen)
                    val lastSpace = truncated.lastIndexOf(' ')
                    val candidate = if (lastSpace > 18) truncated.substring(0, lastSpace) else truncated
                    candidate.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + "…"
                }
            }

            val firstAtt = attachmentNames.firstOrNull()?.trim()
            if (!firstAtt.isNullOrBlank()) {
                val cleanName = firstAtt.take(28)
                return "Fichier : $cleanName"
            }

            val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            return "Discussion $timeStr"
        }

        fun provideFactory(
            botId: String,
            botRepository: BotRepository,
            chatRepository: ChatRepository,
            context: Context? = null,
            conversationId: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(botId, botRepository, chatRepository, context, conversationId) as T
            }
        }
    }
}
