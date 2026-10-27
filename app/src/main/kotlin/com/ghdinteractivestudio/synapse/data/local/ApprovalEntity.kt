package com.ghdinteractivestudio.synapse.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ghdinteractivestudio.synapse.domain.model.ApprovalActionType
import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "approvals")
data class ApprovalEntity(
    @PrimaryKey val id: String,
    val botId: String,
    val botName: String,
    val actionType: String,
    val commandOrPath: String,
    val diffPreview: String?,
    val timestamp: Long,
    val status: String,
    val isAlwaysApproved: Boolean
) {
    fun toDomain(): ApprovalRequest {
        return ApprovalRequest(
            id = id,
            botId = botId,
            botName = botName,
            actionType = try { ApprovalActionType.valueOf(actionType) } catch (e: Exception) { ApprovalActionType.SHELL_COMMAND },
            commandOrPath = commandOrPath,
            diffPreview = diffPreview,
            timestamp = timestamp,
            status = try { ApprovalStatus.valueOf(status) } catch (e: Exception) { ApprovalStatus.PENDING },
            isAlwaysApproved = isAlwaysApproved
        )
    }

    companion object {
        fun fromDomain(req: ApprovalRequest): ApprovalEntity {
            return ApprovalEntity(
                id = req.id,
                botId = req.botId,
                botName = req.botName,
                actionType = req.actionType.name,
                commandOrPath = req.commandOrPath,
                diffPreview = req.diffPreview,
                timestamp = req.timestamp,
                status = req.status.name,
                isAlwaysApproved = req.isAlwaysApproved
            )
        }
    }
}

@Dao
interface ApprovalDao {
    @Query("SELECT * FROM approvals WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getAllPendingApprovalsStream(): Flow<List<ApprovalEntity>>

    @Query("SELECT * FROM approvals WHERE botId = :botId AND status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingApprovalsForBotStream(botId: String): Flow<List<ApprovalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApproval(approval: ApprovalEntity)

    @Query("UPDATE approvals SET status = :status, isAlwaysApproved = :isAlways WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, isAlways: Boolean)
}
