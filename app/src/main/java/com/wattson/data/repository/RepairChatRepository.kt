package com.wattson.data.repository

import android.util.Log
import com.wattson.data.remote.api.ConversationResponse
import com.wattson.data.remote.api.CreateConversationRequest
import com.wattson.data.remote.api.RepairMessageResponse
import com.wattson.data.remote.api.SendRepairMessageRequest
import com.wattson.data.remote.api.WattsonApi
import com.wattson.domain.model.ChatMessage
import com.wattson.domain.model.MessageRole
import com.wattson.domain.model.RepairConversation
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository for repair-chat conversations and messages persisted by the backend.
 */
@Singleton
class RepairChatRepository @Inject constructor(
    private val api: WattsonApi
) {
    companion object {
        private const val TAG = "RepairChatRepository"
    }

    private val conversations = MutableStateFlow<List<RepairConversation>>(emptyList())

    /**
     * Returns the cached list of conversations.
     *
     * Call [refreshConversations] to fetch the latest server state.
     */
    fun getConversations(): Flow<List<RepairConversation>> = conversations.asStateFlow()

    /**
     * Loads all messages for a conversation as a one-shot request.
     */
    suspend fun getMessagesList(
        conversationId: String,
        userId: String
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRepairMessages(userId, conversationId)
            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                Log.d(TAG, "Loaded ${messages.size} messages for conversation $conversationId")
                Result.success(messages.map { it.toDomain(conversationId) })
            } else {
                val message = "Erreur serveur: ${response.code()}"
                Log.e(TAG, "Failed to load messages: ${response.code()} - ${response.message()}")
                Result.failure(RuntimeException(message))
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error loading messages: ${exception.message}", exception)
            Result.failure(exception)
        }
    }

    /**
     * Refreshes the cached conversation list from the backend.
     */
    suspend fun refreshConversations(userId: String) = withContext(Dispatchers.IO) {
        try {
            val response = api.getRepairConversations(userId)
            if (response.isSuccessful) {
                val serverConversations = response.body() ?: emptyList()
                conversations.value = serverConversations.map { it.toDomain() }
                Log.d(
                    TAG,
                    "Refreshed ${serverConversations.size} conversations for user $userId"
                )
            } else {
                Log.e(
                    TAG,
                    "Failed to refresh conversations: ${response.code()} - ${response.message()}"
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error refreshing conversations: ${exception.message}", exception)
        }
    }

    /**
     * Creates a new conversation and refreshes the cached list.
     */
    suspend fun createConversation(userId: String, title: String): RepairConversation =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createRepairConversation(
                    userId,
                    CreateConversationRequest(title)
                )
                if (response.isSuccessful) {
                    val conversation = response.body()!!.toDomain()
                    Log.d(TAG, "Created conversation: ${conversation.id}")
                    refreshConversations(userId)
                    conversation
                } else {
                    Log.e(TAG, "Failed to create conversation: ${response.code()}")
                    throw RuntimeException("Failed to create conversation: ${response.code()}")
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Error creating conversation: ${exception.message}", exception)
                throw exception
            }
        }

    /**
     * Sends a user message and returns the assistant response.
     *
     * @throws PremiumRequiredException when the backend rejects the request for a free plan.
     */
    suspend fun sendMessageAndGetResponse(
        conversationId: String,
        userId: String,
        subscription: String,
        content: String
    ): ChatMessage = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message: conversationId=$conversationId, subscription=$subscription")
            val response = api.sendRepairMessage(
                userId = userId,
                subscription = subscription,
                conversationId = conversationId,
                request = SendRepairMessageRequest(content)
            )

            if (response.isSuccessful) {
                val message = response.body()!!.toDomain(conversationId)
                Log.d(TAG, "AI response received: ${message.content.take(50)}...")
                refreshConversations(userId)
                message
            } else if (response.code() == 403) {
                Log.w(TAG, "Premium required for repair chat")
                throw PremiumRequiredException()
            } else {
                Log.e(TAG, "Failed to send message: ${response.code()} - ${response.message()}")
                throw RuntimeException("Erreur serveur: ${response.code()}")
            }
        } catch (exception: PremiumRequiredException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Error sending message: ${exception.message}", exception)
            throw exception
        }
    }

    /**
     * Deletes a conversation and refreshes the cached list.
     */
    suspend fun deleteConversation(conversationId: String, userId: String) =
        withContext(Dispatchers.IO) {
            try {
                val response = api.deleteRepairConversation(userId, conversationId)
                if (response.isSuccessful) {
                    Log.d(TAG, "Conversation deleted: $conversationId")
                    refreshConversations(userId)
                } else {
                    Log.e(TAG, "Failed to delete conversation: ${response.code()}")
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Error deleting conversation: ${exception.message}", exception)
            }
        }

    private fun ConversationResponse.toDomain(): RepairConversation {
        return RepairConversation(
            id = id,
            userId = "",
            title = title,
            lastMessagePreview = lastMessagePreview,
            createdAt = parseInstant(createdAt),
            updatedAt = parseInstant(updatedAt)
        )
    }

    private fun RepairMessageResponse.toDomain(conversationId: String): ChatMessage {
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = try {
                MessageRole.valueOf(role)
            } catch (_: Exception) {
                MessageRole.ASSISTANT
            },
            content = content,
            timestamp = parseInstant(createdAt)
        )
    }

    private fun parseInstant(isoString: String): Instant {
        return try {
            Instant.parse(isoString)
        } catch (_: Exception) {
            Instant.now()
        }
    }

    /**
     * Thrown when the repair assistant is restricted to premium subscribers.
     */
    class PremiumRequiredException :
        RuntimeException("L'assistant de reparation necessite un abonnement Premium.")
}
