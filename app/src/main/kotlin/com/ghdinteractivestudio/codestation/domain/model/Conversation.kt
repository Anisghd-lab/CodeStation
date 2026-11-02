package com.ghdinteractivestudio.codestation.domain.model

enum class ConversationStatus {
    ACTIVE,
    ARCHIVED
}

data class Conversation(
    val id: String,
    val botId: String,
    val title: String,
    val cliId: String? = null,
    val runnerId: String? = null,
    val cliSessionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: ConversationStatus = ConversationStatus.ACTIVE,
    val lastMessageText: String = "",
    val messageCount: Int = 0
)
