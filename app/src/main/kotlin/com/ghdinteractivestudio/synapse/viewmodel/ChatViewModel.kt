package com.ghdinteractivestudio.synapse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.ExecutionStep
import com.ghdinteractivestudio.synapse.domain.model.QuotedReply
import com.ghdinteractivestudio.synapse.domain.model.StepType
import com.ghdinteractivestudio.synapse.domain.repository.BotRepository
import com.ghdinteractivestudio.synapse.domain.repository.ChatRepository
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

    val uiState: StateFlow<ChatUiState> = combine(
        botRepository.getBotByIdStream(botId),
        chatRepository.getMessagesStream(botId),
        chatRepository.getPendingApprovalsStream(botId),
        _inputDraft,
        _quotedReply,
        _isBotWorking,
        _workingSteps
    ) { bot, messages, approvals, draft, reply, working, steps ->
        ChatUiState(
            bot = bot,
            messages = messages,
            pendingApprovals = approvals,
            isBotWorking = working || bot?.status == BotStatus.WORKING,
            currentWorkingSteps = steps,
            inputDraft = draft,
            quotedReply = reply,
            errorMessage = _errorMessage.value
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
        _inputDraft.value = ""
        _quotedReply.value = null

        viewModelScope.launch {
            chatRepository.sendMessage(botId, text, reply)
            simulateAutonomousTurn(text)
        }
    }

    private fun simulateAutonomousTurn(prompt: String) {
        viewModelScope.launch {
            _isBotWorking.value = true
            botRepository.updateBotStatus(botId, BotStatus.WORKING)

            _workingSteps.value = listOf(
                ExecutionStep(
                    id = "step-1",
                    type = StepType.THOUGHT,
                    title = "Analyse du contexte & instructions",
                    content = "Prise en compte de la demande de l'utilisateur."
                )
            )

            delay(1000)

            // Si la requête contient "bash" ou "script" ou "fichier", créer une demande d'approbation
            if (prompt.contains("bash", ignoreCase = true) || prompt.contains("run", ignoreCase = true) || prompt.contains("exec", ignoreCase = true)) {
                val currentBot = botRepository.getBotById(botId)
                val botName = currentBot?.name ?: "Agent"
                (chatRepository as? com.ghdinteractivestudio.synapse.data.local.ChatRepositoryImpl)?.createApprovalRequest(
                    botId = botId,
                    botName = botName,
                    command = "pytest tests/ --verbose"
                )
            } else {
                _workingSteps.value = _workingSteps.value + ExecutionStep(
                    id = "step-2",
                    type = StepType.TOOL_CALL,
                    title = "Lecture des sources locales",
                    content = "Inspecte les fichiers du projet."
                )
                delay(1200)

                val bot = botRepository.getBotById(botId)
                val botName = bot?.name ?: "Synapse Assistant"
                val replyText = "J'ai traité votre consigne avec succès. Tout est synchronisé et conforme à l'architecture."

                (chatRepository as? com.ghdinteractivestudio.synapse.data.local.ChatRepositoryImpl)?.insertAgentReply(
                    botId = botId,
                    botName = botName,
                    text = replyText,
                    traceSteps = _workingSteps.value
                )
            }

            _isBotWorking.value = false
            botRepository.updateBotStatus(botId, BotStatus.IDLE)
        }
    }

    fun respondApproval(approvalId: String, status: ApprovalStatus, alwaysApprove: Boolean = false) {
        viewModelScope.launch {
            chatRepository.respondApproval(approvalId, status, alwaysApprove)
            if (status == ApprovalStatus.APPROVED) {
                // Débloque et poursuit le tour
                delay(500)
                val bot = botRepository.getBotById(botId)
                val botName = bot?.name ?: "Agent"
                (chatRepository as? com.ghdinteractivestudio.synapse.data.local.ChatRepositoryImpl)?.insertAgentReply(
                    botId = botId,
                    botName = botName,
                    text = "Commande shell exécutée avec succès (Code 0)."
                )
                botRepository.updateBotStatus(botId, BotStatus.IDLE)
            }
        }
    }

    fun stopTurn() {
        viewModelScope.launch {
            _isBotWorking.value = false
            botRepository.stopBotTurn(botId)
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
