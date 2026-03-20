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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Last Edit : 20/03/2026 -- Victorio Garcia
// Resume : ---------------------------------
// Step 1 : Remplacement de Room (local) par Retrofit (backend) pour les conversations
// Step 2 : Remplacement de MockRepairAssistantService par appel API reel vers Ollama
// Step 3 : Gestion des erreurs HTTP (403 Premium, 500 serveur, timeout)
// Step 4 : StateFlow pour observer les conversations (au lieu de Room Flow)
// Explication Total : Repository pour le chat de reparation IA. Communique avec le
//   backend recommendation-service via Retrofit. Les conversations et messages sont
//   stockes cote serveur (MongoDB). Le backend appelle Ollama (Mistral 7B) pour les
//   reponses IA. La verification premium est faite cote serveur via X-Subscription.
// Historique : 17/03/2026 -- Creation initiale (Room + MockRepairAssistantService)
//              20/03/2026 -- Migration vers API backend (Retrofit + Ollama)

/**
 * Repository for repair chat operations.
 * Communicates with the backend recommendation-service via Retrofit.
 * Conversations and messages are stored server-side in MongoDB.
 */
@Singleton
class RepairChatRepository @Inject constructor(
    private val api: WattsonApi
) {
    companion object {
        private const val TAG = "RepairChatRepository"
    }

    private val _conversations = MutableStateFlow<List<RepairConversation>>(emptyList())

    /**
     * Observe all conversations for a user.
     * Call [refreshConversations] to fetch latest from server.
     */
    fun getConversations(userId: String): Flow<List<RepairConversation>> {
        return _conversations.asStateFlow()
    }

    /**
     * Observe all messages for a conversation.
     * Returns a one-shot list (not reactive — call again to refresh).
     */
    suspend fun getMessagesList(conversationId: String, userId: String): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getRepairMessages(userId, conversationId)
                if (response.isSuccessful) {
                    val messages = response.body() ?: emptyList()
                    Log.d(TAG, "Loaded ${messages.size} messages for conversation $conversationId")
                    messages.map { it.toDomain(conversationId) }
                } else {
                    Log.e(TAG, "Failed to load messages: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages: ${e.message}", e)
                emptyList()
            }
        }

    /**
     * Refresh conversations list from the server.
     */
    suspend fun refreshConversations(userId: String) = withContext(Dispatchers.IO) {
        try {
            val response = api.getRepairConversations(userId)
            if (response.isSuccessful) {
                val conversations = response.body() ?: emptyList()
                _conversations.value = conversations.map { it.toDomain() }
                Log.d(TAG, "Refreshed ${conversations.size} conversations for user $userId")
            } else {
                Log.e(TAG, "Failed to refresh conversations: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing conversations: ${e.message}", e)
        }
    }

    /**
     * Create a new conversation and return the domain model.
     */
    suspend fun createConversation(userId: String, title: String): RepairConversation =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createRepairConversation(userId, CreateConversationRequest(title))
                if (response.isSuccessful) {
                    val conversation = response.body()!!.toDomain()
                    Log.d(TAG, "Created conversation: ${conversation.id}")
                    refreshConversations(userId)
                    conversation
                } else {
                    Log.e(TAG, "Failed to create conversation: ${response.code()}")
                    throw RuntimeException("Failed to create conversation: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating conversation: ${e.message}", e)
                throw e
            }
        }

    /**
     * Send a user message and get the AI assistant response.
     *
     * @throws PremiumRequiredException if the user is on FREE plan (403 from server).
     * @throws RuntimeException on other errors.
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
        } catch (e: PremiumRequiredException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            throw e
        }
    }

    /**
     * Delete a conversation and all its messages.
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
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting conversation: ${e.message}", e)
            }
        }

    // ===== Mappers =====

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
            role = try { MessageRole.valueOf(role) } catch (e: Exception) { MessageRole.ASSISTANT },
            content = content,
            timestamp = parseInstant(createdAt)
        )
    }

    private fun parseInstant(isoString: String): Instant {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            Instant.now()
        }
    }

    /**
     * Exception thrown when a FREE user tries to use the repair assistant.
     */
    class PremiumRequiredException : RuntimeException("L'assistant de reparation necessite un abonnement Premium.")
}
