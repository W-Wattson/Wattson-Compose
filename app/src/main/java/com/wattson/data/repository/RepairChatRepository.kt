package com.wattson.data.repository

import com.wattson.data.local.dao.ChatMessageDao
import com.wattson.data.local.dao.RepairConversationDao
import com.wattson.data.local.entity.ChatMessageEntity
import com.wattson.data.local.entity.RepairConversationEntity
import com.wattson.data.local.service.MockRepairAssistantService
import com.wattson.domain.model.ChatMessage
import com.wattson.domain.model.MessageRole
import com.wattson.domain.model.RepairConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for repair chat operations.
 * Manages conversations and messages via Room, and delegates AI responses to MockRepairAssistantService.
 */
@Singleton
class RepairChatRepository @Inject constructor(
    private val conversationDao: RepairConversationDao,
    private val messageDao: ChatMessageDao,
    private val mockAssistant: MockRepairAssistantService
) {

    /**
     * Observe all conversations for a user, ordered by most recent activity.
     * Each conversation includes a preview of its last message.
     */
    fun getConversations(userId: String): Flow<List<RepairConversation>> {
        return conversationDao.getConversationsForUser(userId).map { entities ->
            entities.map { entity ->
                val lastMessage = messageDao.getLastMessage(entity.id)
                entity.toDomain(lastMessagePreview = lastMessage?.content)
            }
        }
    }

    /**
     * Observe all messages for a conversation, ordered chronologically.
     */
    fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Create a new conversation and return the domain model.
     */
    suspend fun createConversation(userId: String, title: String): RepairConversation = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val entity = RepairConversationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli()
        )
        conversationDao.insertConversation(entity)
        entity.toDomain(lastMessagePreview = null)
    }

    /**
     * Send a user message and persist it to Room.
     * Also updates the conversation's updatedAt timestamp.
     */
    suspend fun sendMessage(conversationId: String, content: String): ChatMessage = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val entity = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = MessageRole.USER.name,
            content = content,
            timestamp = now.toEpochMilli()
        )
        messageDao.insertMessage(entity)
        updateConversationTimestamp(conversationId, now)
        entity.toDomain()
    }

    /**
     * Request an AI response for the given user message.
     * The response is persisted to Room and returned.
     */
    suspend fun getAssistantResponse(conversationId: String, userMessage: String): ChatMessage = withContext(Dispatchers.IO) {
        val responseContent = mockAssistant.generateResponse(userMessage)
        val now = Instant.now()
        val entity = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = MessageRole.ASSISTANT.name,
            content = responseContent,
            timestamp = now.toEpochMilli()
        )
        messageDao.insertMessage(entity)
        updateConversationTimestamp(conversationId, now)

        // Auto-update conversation title from first user message
        val conversation = conversationDao.getConversationById(conversationId)
        if (conversation != null && conversation.title == "Nouvelle conversation") {
            val newTitle = userMessage.take(40) + if (userMessage.length > 40) "..." else ""
            conversationDao.updateConversation(conversation.copy(title = newTitle))
        }

        entity.toDomain()
    }

    /**
     * Delete a conversation. Messages are cascade-deleted by Room.
     */
    suspend fun deleteConversation(conversationId: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversationById(conversationId)
    }

    // ===== Private helpers =====

    private suspend fun updateConversationTimestamp(conversationId: String, now: Instant) {
        conversationDao.getConversationById(conversationId)?.let { conv ->
            conversationDao.updateConversation(conv.copy(updatedAt = now.toEpochMilli()))
        }
    }

    private fun RepairConversationEntity.toDomain(lastMessagePreview: String?): RepairConversation {
        return RepairConversation(
            id = id,
            userId = userId,
            title = title,
            lastMessagePreview = lastMessagePreview,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt)
        )
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = MessageRole.valueOf(role),
            content = content,
            timestamp = Instant.ofEpochMilli(timestamp)
        )
    }
}
