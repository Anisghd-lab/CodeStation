package com.ghdinteractivestudio.codestation.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.FileAttachment
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthor
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val botId: String,
    val authorType: String, // USER, BOT, SYSTEM
    val authorId: String,
    val authorName: String,
    val text: String,
    val timestamp: Long,
    val isFinal: Boolean,
    val replyToMessageId: String?,
    val replyToAuthor: String?,
    val replyToText: String?,
    val attachmentsJson: String? = null
) {
    fun toDomain(): ChatMessage {
        val author = MessageAuthor(
            type = try { MessageAuthorType.valueOf(authorType) } catch (e: Exception) { MessageAuthorType.BOT },
            id = authorId,
            displayName = authorName
        )
        val replyTo = if (replyToMessageId != null && replyToText != null) {
            QuotedReply(
                messageId = replyToMessageId,
                authorName = replyToAuthor ?: "",
                textExcerpt = replyToText
            )
        } else null

        val parsedAttachments: List<FileAttachment> = if (!attachmentsJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<FileAttachmentDto>>(attachmentsJson).map {
                    FileAttachment(it.id, it.name, it.sizeBytes, it.mimeType, it.localUri)
                }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        return ChatMessage(
            id = id,
            botId = botId,
            author = author,
            text = text,
            timestamp = timestamp,
            isFinal = isFinal,
            replyTo = replyTo,
            attachments = parsedAttachments
        )
    }

    companion object {
        fun fromDomain(msg: ChatMessage): ChatMessageEntity {
            val jsonStr = if (msg.attachments.isNotEmpty()) {
                try {
                    val dtos = msg.attachments.map {
                        FileAttachmentDto(it.id, it.name, it.sizeBytes, it.mimeType, it.localUri)
                    }
                    Json.encodeToString(dtos)
                } catch (_: Exception) { null }
            } else null

            return ChatMessageEntity(
                id = msg.id,
                botId = msg.botId,
                authorType = msg.author.type.name,
                authorId = msg.author.id,
                authorName = msg.author.displayName,
                text = msg.text,
                timestamp = msg.timestamp,
                isFinal = msg.isFinal,
                replyToMessageId = msg.replyTo?.messageId,
                replyToAuthor = msg.replyTo?.authorName,
                replyToText = msg.replyTo?.textExcerpt,
                attachmentsJson = jsonStr
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class FileAttachmentDto(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val localUri: String? = null
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages WHERE botId = :botId ORDER BY timestamp ASC")
    fun getMessagesByBotIdStream(botId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE botId = :botId")
    suspend fun deleteMessagesForBot(botId: String)
}
