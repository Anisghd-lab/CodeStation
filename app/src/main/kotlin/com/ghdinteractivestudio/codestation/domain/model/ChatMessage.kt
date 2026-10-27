package com.ghdinteractivestudio.codestation.domain.model

enum class MessageAuthorType {
    USER,
    BOT,
    SYSTEM
}

data class MessageAuthor(
    val type: MessageAuthorType,
    val id: String,
    val displayName: String,
    val avatarGlyph: String? = null,
    val avatarColorHex: String? = null
)

data class QuotedReply(
    val messageId: String,
    val authorName: String,
    val textExcerpt: String
)

data class FileAttachment(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val localUri: String? = null
)

data class ArtifactMetadata(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val previewHtmlSnippet: String? = null
)

data class ChatMessage(
    val id: String,
    val botId: String,
    val author: MessageAuthor,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFinal: Boolean = true,
    val replyTo: QuotedReply? = null,
    val attachments: List<FileAttachment> = emptyList(),
    val artifact: ArtifactMetadata? = null,
    val executionTrace: List<ExecutionStep> = emptyList(),
    val reactions: Map<String, Int> = emptyMap(),
    val isSending: Boolean = false,
    val hasError: Boolean = false
)
