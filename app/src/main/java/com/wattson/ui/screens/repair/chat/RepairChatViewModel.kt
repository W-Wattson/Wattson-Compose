package com.wattson.ui.screens.repair.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.ChatMessage
import com.wattson.domain.model.MessageRole
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.RepairChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

// Last Edit : 20/03/2026 -- Victorio Garcia
// Resume : ---------------------------------
// Step 1 : Chargement des messages depuis le backend (GET /conversations/{id}/messages)
// Step 2 : Envoi de message user + reception reponse IA (POST /conversations/{id}/messages)
// Step 3 : Ajout optimiste du message user dans la liste (avant la reponse serveur)
// Step 4 : Gestion du typing indicator pendant l'appel Ollama (~3-8s)
// Step 5 : Gestion erreur 403 PremiumRequired (afficher prompt upgrade)
// Explication Total : ViewModel pour l'ecran de chat reparation. Envoie les messages
//   au backend recommendation-service qui appelle Ollama (Mistral 7B) sur Hetzner.
//   Le message user est ajoute optimistiquement a la liste pour une UX reactive,
//   puis la reponse IA est ajoutee quand elle arrive du serveur.
// Historique : 17/03/2026 -- Creation initiale (Room + MockRepairAssistantService)
//              20/03/2026 -- Migration vers API backend (Retrofit + Ollama)

/**
 * UI State for the Repair Chat screen.
 */
data class RepairChatUiState(
    val conversationId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isAssistantTyping: Boolean = false,
    val errorMessage: String? = null
)

/**
 * One-shot events for the chat screen.
 */
sealed interface RepairChatEvent {
    data object ScrollToBottom : RepairChatEvent
    data class ShowError(val message: String) : RepairChatEvent
}

/**
 * User intents for the chat screen.
 */
sealed interface RepairChatIntent {
    data class UpdateInput(val text: String) : RepairChatIntent
    data object SendMessage : RepairChatIntent
    data object DismissError : RepairChatIntent
}

/**
 * ViewModel for the Repair Chat screen.
 * Manages the message list, input state, and AI assistant interaction via backend API.
 */
@HiltViewModel
class RepairChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repairChatRepository: RepairChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val conversationId: String = savedStateHandle["conversationId"] ?: ""

    private val _uiState = MutableStateFlow(RepairChatUiState(conversationId = conversationId))
    val uiState: StateFlow<RepairChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RepairChatEvent>()
    val events = _events.asSharedFlow()

    init {
        loadMessages()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: RepairChatIntent) {
        when (intent) {
            is RepairChatIntent.UpdateInput -> _uiState.update { it.copy(inputText = intent.text) }
            is RepairChatIntent.SendMessage -> sendMessage()
            is RepairChatIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    /**
     * Loads messages from the backend for this conversation.
     */
    private fun loadMessages() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val messages = repairChatRepository.getMessagesList(conversationId, userId)
                _uiState.update { it.copy(messages = messages) }
                if (messages.isNotEmpty()) {
                    _events.emit(RepairChatEvent.ScrollToBottom)
                }
            } catch (e: Exception) {
                _events.emit(RepairChatEvent.ShowError(e.message ?: "Erreur de chargement"))
            }
        }
    }

    /**
     * Sends a user message and waits for the AI response.
     * The user message is added optimistically to the list for instant UX feedback.
     */
    private fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || _uiState.value.isAssistantTyping) return

        viewModelScope.launch {
            try {
                // Clear input immediately
                _uiState.update { it.copy(inputText = "") }

                // Optimistic add: show user message immediately (before server confirms)
                val optimisticUserMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = content,
                    timestamp = Instant.now()
                )
                _uiState.update { it.copy(messages = it.messages + optimisticUserMsg) }
                _events.emit(RepairChatEvent.ScrollToBottom)

                // Show typing indicator
                _uiState.update { it.copy(isAssistantTyping = true) }

                // Send message and get AI response from backend (calls Ollama)
                val userId = authRepository.getCurrentUserId()
                val subscription = authRepository.currentUser.value?.subscriptionType?.name ?: "FREE"

                val assistantResponse = repairChatRepository.sendMessageAndGetResponse(
                    conversationId = conversationId,
                    userId = userId,
                    subscription = subscription,
                    content = content
                )

                // Add AI response to the list
                _uiState.update {
                    it.copy(
                        isAssistantTyping = false,
                        messages = it.messages + assistantResponse
                    )
                }
                _events.emit(RepairChatEvent.ScrollToBottom)

            } catch (e: RepairChatRepository.PremiumRequiredException) {
                _uiState.update { it.copy(isAssistantTyping = false) }
                _events.emit(RepairChatEvent.ShowError(
                    "L'assistant de reparation necessite un abonnement Premium. Passez a Premium pour acceder a cette fonctionnalite."
                ))
            } catch (e: Exception) {
                _uiState.update { it.copy(isAssistantTyping = false) }
                _events.emit(RepairChatEvent.ShowError(e.message ?: "Erreur d'envoi"))
            }
        }
    }
}
