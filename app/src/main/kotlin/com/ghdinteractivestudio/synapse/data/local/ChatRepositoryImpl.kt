package com.ghdinteractivestudio.synapse.data.local

import com.ghdinteractivestudio.synapse.data.remote.RespondPermissionRequest
import com.ghdinteractivestudio.synapse.data.remote.SendMessageRequest
import com.ghdinteractivestudio.synapse.data.remote.StopTurnRequest
import com.ghdinteractivestudio.synapse.data.remote.SynapseApiService
import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthor
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthorType
import com.ghdinteractivestudio.synapse.domain.model.QuotedReply
import com.ghdinteractivestudio.synapse.domain.repository.ChatRepository
import com.ghdinteractivestudio.synapse.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepositoryImpl(
    private val messageDao: MessageDao,
    private val approvalDao: ApprovalDao,
    private val botDao: BotDao,
    private val apiServiceProvider: (suspend () -> SynapseApiService?)? = null,
    private val serverRepository: ServerRepository? = null
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

            // Tentative d'envoi réseau vers apiService si connecté
            if (apiServiceProvider != null && serverRepository != null) {
                try {
                    val config = serverRepository.getServerConfig()
                    val api = apiServiceProvider.invoke()
                    if (api != null && !config.token.isNullOrBlank()) {
                        val auth = "Bearer ${config.token}"
                        api.sendMessage(
                            SendMessageRequest(
                                botId = botId,
                                text = text,
                                clientNonce = userMsg.id,
                                replyTo = replyTo?.messageId
                            ),
                            authHeader = auth
                        )
                    }
                } catch (_: Exception) {
                    // Fallback silencieux en local
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
            if (apiServiceProvider != null && serverRepository != null) {
                try {
                    val config = serverRepository.getServerConfig()
                    val api = apiServiceProvider.invoke()
                    if (api != null && !config.token.isNullOrBlank()) {
                        val auth = "Bearer ${config.token}"
                        val optionId = when (status) {
                            ApprovalStatus.APPROVED -> if (alwaysApprove) "allow_always" else "allow_once"
                            ApprovalStatus.REJECTED -> if (alwaysApprove) "reject_always" else "reject_once"
                            else -> "allow_once"
                        }
                        api.respondPermission(
                            RespondPermissionRequest(entryId = approvalId, optionId = optionId),
                            authHeader = auth
                        )
                    }
                } catch (_: Exception) {
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
            // Marquage local
        }
    }

    suspend fun insertAgentReply(
        botId: String,
        botName: String,
        text: String,
        traceSteps: List<com.ghdinteractivestudio.synapse.domain.model.ExecutionStep> = emptyList()
    ): Result<ChatMessage> {
        return runCatching {
            val agentMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                botId = botId,
                author = MessageAuthor(MessageAuthorType.BOT, botId, botName),
                text = text,
                timestamp = System.currentTimeMillis(),
                isFinal = true
            )
            messageDao.insertMessage(ChatMessageEntity.fromDomain(agentMsg))
            botDao.updateLastMessage(botId, text, agentMsg.timestamp)
            agentMsg
        }
    }

    suspend fun createApprovalRequest(
        botId: String,
        botName: String,
        command: String,
        diff: String? = null
    ): Result<ApprovalRequest> {
        return runCatching {
            val approval = ApprovalRequest(
                id = UUID.randomUUID().toString(),
                botId = botId,
                botName = botName,
                actionType = com.ghdinteractivestudio.synapse.domain.model.ApprovalActionType.SHELL_COMMAND,
                commandOrPath = command,
                diffPreview = diff,
                timestamp = System.currentTimeMillis(),
                status = ApprovalStatus.PENDING
            )
            approvalDao.insertApproval(ApprovalEntity.fromDomain(approval))
            approval
        }
    }
}
