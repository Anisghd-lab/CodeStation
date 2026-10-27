package com.ghdinteractivestudio.synapse.domain.repository

import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.QuotedReply
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesStream(botId: String): Flow<List<ChatMessage>>
    fun getPendingApprovalsStream(botId: String? = null): Flow<List<ApprovalRequest>>
    suspend fun sendMessage(botId: String, text: String, replyTo: QuotedReply? = null): Result<ChatMessage>
    suspend fun respondApproval(approvalId: String, status: ApprovalStatus, alwaysApprove: Boolean = false): Result<Unit>
    suspend fun clearHistory(botId: String): Result<Unit>
    suspend fun markChatRead(botId: String): Result<Unit>
}
