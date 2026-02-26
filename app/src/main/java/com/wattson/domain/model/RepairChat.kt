package com.wattson.domain.model

import java.time.Instant

/**
 * Domain model for a repair chat conversation.
 */
data class RepairConversation(
    val id: String,
    val userId: String,
    val title: String,
    val lastMessagePreview: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Role of a chat message sender.
 */
enum class MessageRole {
    USER,
    ASSISTANT
}

/**
 * Domain model for a chat message.
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant
)
