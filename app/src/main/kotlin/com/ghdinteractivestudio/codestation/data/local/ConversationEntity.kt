package com.ghdinteractivestudio.codestation.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ghdinteractivestudio.codestation.domain.model.Conversation
import com.ghdinteractivestudio.codestation.domain.model.ConversationStatus
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["botId"]),
        Index(value = ["updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val botId: String,
    val title: String,
    val cliId: String? = null,
    val runnerId: String? = null,
    val cliSessionId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val lastMessageText: String = "",
    val messageCount: Int = 0
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        botId = botId,
        title = title,
        cliId = cliId,
        runnerId = runnerId,
        cliSessionId = cliSessionId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = try { ConversationStatus.valueOf(status) } catch (_: Exception) { ConversationStatus.ACTIVE },
        lastMessageText = lastMessageText,
        messageCount = messageCount
    )

    companion object {
        fun fromDomain(conv: Conversation): ConversationEntity = ConversationEntity(
            id = conv.id,
            botId = conv.botId,
            title = conv.title,
            cliId = conv.cliId,
            runnerId = conv.runnerId,
            cliSessionId = conv.cliSessionId,
            createdAt = conv.createdAt,
            updatedAt = conv.updatedAt,
            status = conv.status.name,
            lastMessageText = conv.lastMessageText,
            messageCount = conv.messageCount
        )
    }
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE botId = :botId ORDER BY updatedAt DESC")
    fun getConversationsByBotIdStream(botId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversationsStream(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE botId = :botId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestConversationForBot(botId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: ConversationEntity)

    @Update
    suspend fun updateConversation(conv: ConversationEntity)

    @Query("UPDATE conversations SET lastMessageText = :text, updatedAt = :timestamp, messageCount = messageCount + 1 WHERE id = :id")
    suspend fun updateLastMessage(id: String, text: String, timestamp: Long)

    @Query("UPDATE conversations SET title = :title, updatedAt = :timestamp WHERE id = :id")
    suspend fun renameConversation(id: String, title: String, timestamp: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Query("DELETE FROM conversations WHERE botId = :botId")
    suspend fun deleteConversationsForBot(botId: String)
}
