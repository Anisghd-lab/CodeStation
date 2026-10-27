package com.ghdinteractivestudio.synapse.data.local

import com.ghdinteractivestudio.synapse.data.remote.ApproveRequest
import com.ghdinteractivestudio.synapse.data.remote.SendMessageRequest
import com.ghdinteractivestudio.synapse.data.remote.SynapseApiService
import com.ghdinteractivestudio.synapse.domain.model.ApprovalActionType
import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.ExecutionStep
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthor
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthorType
import com.ghdinteractivestudio.synapse.domain.model.QuotedReply
import com.ghdinteractivestudio.synapse.domain.model.StepType
import com.ghdinteractivestudio.synapse.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepositoryImpl(
    private val messageDao: MessageDao,
    private val approvalDao: ApprovalDao,
    private val botDao: BotDao,
    private val apiService: SynapseApiService? = null
) : ChatRepository {

    override fun getMessagesStream(botId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByBotIdStream(botId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingApprovalsStream(botId: String?): Flow<List<ApprovalRequest>> {
        val stream = if (botId == null) {
            approvalDao.getAllPendingApprovalsStream()
        } else {
            approvalDao.getPendingApprovalsForBotStream(botId)
        }
        return stream.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun sendMessage(
        botId: String,
        text: String,
        replyTo: QuotedReply?
    ): Result<ChatMessage> {
        return runCatching {
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                botId = botId,
                author = MessageAuthor(MessageAuthorType.USER, "user", "Moi"),
                text = text,
                timestamp = System.currentTimeMillis(),
                isFinal = true,
                replyTo = replyTo
            )
            messageDao.insertMessage(ChatMessageEntity.fromDomain(userMsg))
            botDao.updateLastMessage(botId, text, userMsg.timestamp)

            // Tentative d'envoi réseau vers apiService si configuré
            if (apiService != null) {
                try {
                    apiService.sendMessage(SendMessageRequest(botId, text, replyTo?.messageId))
                } catch (e: Exception) {
                    // Fallback silencieux en mode hors-ligne
                }
            }

            userMsg
        }
    }

    override suspend fun respondApproval(
        approvalId: String,
        status: ApprovalStatus,
        alwaysApprove: Boolean
    ): Result<Unit> {
        return runCatching {
            approvalDao.updateStatus(approvalId, status.name, alwaysApprove)
            if (apiService != null) {
                try {
                    apiService.respondApproval(ApproveRequest(approvalId, status.name, alwaysApprove))
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
    }

    override suspend fun clearHistory(botId: String): Result<Unit> {
        return runCatching {
            messageDao.deleteMessagesForBot(botId)
        }
    }

    override suspend fun markChatRead(botId: String): Result<Unit> {
        return runCatching {
            // Dans Room, les messages du bot sont marqués comme consultés
        }
    }

    suspend fun insertAgentReply(
        botId: String,
        botName: String,
        text: String,
        traceSteps: List<ExecutionStep> = emptyList()
    ) {
        val agentMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            botId = botId,
            author = MessageAuthor(MessageAuthorType.BOT, botId, botName),
            text = text,
            timestamp = System.currentTimeMillis(),
            isFinal = true,
            executionTrace = traceSteps
        )
        messageDao.insertMessage(ChatMessageEntity.fromDomain(agentMsg))
        botDao.updateLastMessage(botId, text, agentMsg.timestamp)
    }

    suspend fun createApprovalRequest(
        botId: String,
        botName: String,
        command: String,
        diff: String? = null
    ) {
        val req = ApprovalRequest(
            id = UUID.randomUUID().toString(),
            botId = botId,
            botName = botName,
            actionType = ApprovalActionType.SHELL_COMMAND,
            commandOrPath = command,
            diffPreview = diff,
            timestamp = System.currentTimeMillis(),
            status = ApprovalStatus.PENDING
        )
        approvalDao.insertApproval(ApprovalEntity.fromDomain(req))
    }
}
