package com.ghdinteractivestudio.codestation.domain.repository

import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.Conversation
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesStream(botId: String): Flow<List<ChatMessage>>
    fun getMessagesByConversationStream(botId: String, conversationId: String?): Flow<List<ChatMessage>>
    fun getConversationsStream(botId: String): Flow<List<Conversation>>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun createConversation(botId: String, title: String = "Nouvelle discussion", cliId: String? = null, runnerId: String? = null): Result<Conversation>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun renameConversation(conversationId: String, title: String): Result<Unit>
    fun getPendingApprovalsStream(botId: String? = null): Flow<List<ApprovalRequest>>
    suspend fun sendMessage(
        botId: String,
        text: String,
        replyTo: QuotedReply? = null,
        attachments: List<com.ghdinteractivestudio.codestation.domain.model.FileAttachment> = emptyList(),
        conversationId: String? = null
    ): Result<ChatMessage>
    suspend fun respondApproval(approvalId: String, status: ApprovalStatus, alwaysApprove: Boolean = false): Result<Unit>
    suspend fun clearHistory(botId: String): Result<Unit>
    suspend fun markChatRead(botId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
}
