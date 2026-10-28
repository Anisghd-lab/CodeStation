package com.ghdinteractivestudio.codestation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.ExecutionStep
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import com.ghdinteractivestudio.codestation.domain.model.StepType
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.domain.repository.ChatRepository
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
    val errorMessage: String? = null
)

class ChatViewModel(
    private val botId: String,
    private val botRepository: BotRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _inputDraft = MutableStateFlow("")
    private val _quotedReply = MutableStateFlow<QuotedReply?>(null)
    private val _isBotWorking = MutableStateFlow(false)
    private val _workingSteps = MutableStateFlow<List<ExecutionStep>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var lastSentText: String? = null
    private var turnTimeoutJob: Job? = null

    private data class InputState(
        val draft: String,
        val reply: QuotedReply?,
        val working: Boolean,
        val steps: List<ExecutionStep>,
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
        _errorMessage
    ) { draft, reply, working, steps, error ->
        InputState(draft, reply, working, steps, error)
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

        // Observer l'état du bot : s'il passe à IDLE, libérer l'état de génération
        viewModelScope.launch {
            botRepository.getBotByIdStream(botId).collect { bot ->
                if (bot?.status == BotStatus.IDLE && _isBotWorking.value) {
                    turnTimeoutJob?.cancel()
                    _isBotWorking.value = false
                }
            }
        }

        // Observer les messages : si une réponse finale du bot arrive, libérer l'état de génération
        viewModelScope.launch {
            chatRepository.getMessagesStream(botId).collect { msgs ->
                val lastMsg = msgs.lastOrNull()
                if (lastMsg != null && lastMsg.author.type == com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType.BOT && lastMsg.isFinal) {
                    turnTimeoutJob?.cancel()
                    _isBotWorking.value = false
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _inputDraft.value = text
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
        val text = _inputDraft.value.trim()
        if (text.isBlank()) return

        val reply = _quotedReply.value
        lastSentText = text
        _inputDraft.value = ""
        _quotedReply.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            _isBotWorking.value = true
            botRepository.updateBotStatus(botId, BotStatus.WORKING)

            // Démarrer un délai d'attente raisonnable (45s) pour éviter l'attente infinie
            turnTimeoutJob?.cancel()
            turnTimeoutJob = launch {
                delay(45000)
                if (_isBotWorking.value) {
                    _isBotWorking.value = false
                    botRepository.updateBotStatus(botId, BotStatus.IDLE)
                    _errorMessage.value = "Délai d'attente dépassé (45s). Le modèle ou le serveur n'a pas répondu à temps."
                    (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                        botId = botId,
                        text = "⏱️ Délai de réponse dépassé (45s). Vérifiez que Termux et le CLI du bot sont actifs, puis appuyez sur Réessayer."
                    )
                }
            }

            val result = chatRepository.sendMessage(botId, text, reply)
            result.onFailure { error ->
                turnTimeoutJob?.cancel()
                _isBotWorking.value = false
                botRepository.updateBotStatus(botId, BotStatus.IDLE)
                _errorMessage.value = error.message ?: "Échec de l'envoi au serveur"
                (chatRepository as? com.ghdinteractivestudio.codestation.data.local.ChatRepositoryImpl)?.insertNotice(
                    botId = botId,
                    text = "Erreur de transmission : ${error.localizedMessage}"
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
        _isBotWorking.value = false
        viewModelScope.launch {
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

    companion object {
        fun provideFactory(
            botId: String,
            botRepository: BotRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(botId, botRepository, chatRepository) as T
            }
        }
    }
}
