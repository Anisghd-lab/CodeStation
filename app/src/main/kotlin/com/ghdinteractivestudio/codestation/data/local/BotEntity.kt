package com.ghdinteractivestudio.codestation.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.PermissionPolicy
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.WorkspaceMode
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bots")
data class BotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val avatarGlyph: String,
    val avatarColorHex: String,
    val provider: String,
    val model: String,
    val isFastMode: Boolean,
    val workspaceMode: String,
    val workspacePath: String,
    val instructions: String,
    val permissionPolicy: String,
    val isLead: Boolean,
    val isPinned: Boolean,
    val status: String,
    val lastMessage: String?,
    val lastActivityAt: Long,
    val unreadCount: Int,
    val pendingApprovalsCount: Int,
    val membersCsv: String = ""
) {
    fun toDomain(): Bot {
        return Bot(
            id = id,
            name = name,
            description = description,
            avatarGlyph = avatarGlyph,
            avatarColorHex = avatarColorHex,
            provider = try { ProviderType.valueOf(provider) } catch (e: Exception) { ProviderType.CLAUDE_CODE },
            model = model,
            isFastMode = isFastMode,
            workspaceMode = try { WorkspaceMode.valueOf(workspaceMode) } catch (e: Exception) { WorkspaceMode.PERSONAL },
            workspacePath = workspacePath,
            instructions = instructions,
            permissionPolicy = try { PermissionPolicy.valueOf(permissionPolicy) } catch (e: Exception) { PermissionPolicy.ASK_SHELL },
            isLead = isLead,
            isPinned = isPinned,
            status = try { BotStatus.valueOf(status) } catch (e: Exception) { BotStatus.IDLE },
            lastMessage = lastMessage,
            lastActivityAt = lastActivityAt,
            unreadCount = unreadCount,
            pendingApprovalsCount = pendingApprovalsCount,
            memberBotIds = if (membersCsv.isBlank()) emptyList() else membersCsv.split(",")
        )
    }

    companion object {
        fun fromDomain(bot: Bot): BotEntity {
            return BotEntity(
                id = bot.id,
                name = bot.name,
                description = bot.description,
                avatarGlyph = bot.avatarGlyph,
                avatarColorHex = bot.avatarColorHex,
                provider = bot.provider.name,
                model = bot.model,
                isFastMode = bot.isFastMode,
                workspaceMode = bot.workspaceMode.name,
                workspacePath = bot.workspacePath,
                instructions = bot.instructions,
                permissionPolicy = bot.permissionPolicy.name,
                isLead = bot.isLead,
                isPinned = bot.isPinned,
                status = bot.status.name,
                lastMessage = bot.lastMessage,
                lastActivityAt = bot.lastActivityAt,
                unreadCount = bot.unreadCount,
                pendingApprovalsCount = bot.pendingApprovalsCount,
                membersCsv = bot.memberBotIds.joinToString(",")
            )
        }
    }
}

@Dao
interface BotDao {
    @Query("SELECT * FROM bots ORDER BY isLead DESC, isPinned DESC, lastActivityAt DESC")
    fun getAllBotsStream(): Flow<List<BotEntity>>

    @Query("SELECT * FROM bots WHERE id = :id LIMIT 1")
    fun getBotByIdStream(id: String): Flow<BotEntity?>

    @Query("SELECT * FROM bots WHERE id = :id LIMIT 1")
    suspend fun getBotById(id: String): BotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBot(bot: BotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bots: List<BotEntity>)

    @Update
    suspend fun updateBot(bot: BotEntity)

    @Query("DELETE FROM bots WHERE id = :id")
    suspend fun deleteBotById(id: String)

    @Query("UPDATE bots SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: String)

    @Query("UPDATE bots SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE bots SET lastMessage = :message, lastActivityAt = :timestamp WHERE id = :id")
    suspend fun updateLastMessage(id: String, message: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM bots")
    suspend fun getBotCount(): Int
}
