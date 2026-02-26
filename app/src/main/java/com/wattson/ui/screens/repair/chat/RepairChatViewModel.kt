package com.wattson.ui.screens.repair.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.ChatMessage
import com.wattson.data.repository.RepairChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
 * Manages the message list, input state, and AI assistant interaction.
 */
@HiltViewModel
class RepairChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repairChatRepository: RepairChatRepository
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

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                repairChatRepository.getMessages(conversationId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            } catch (e: Exception) {
                _events.emit(RepairChatEvent.ShowError(e.message ?: "Erreur de chargement"))
            }
        }
    }

    private fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || _uiState.value.isAssistantTyping) return

        viewModelScope.launch {
            try {
                // Clear input immediately
                _uiState.update { it.copy(inputText = "") }

                // Send user message
                repairChatRepository.sendMessage(conversationId, content)
                _events.emit(RepairChatEvent.ScrollToBottom)

                // Show typing indicator
                _uiState.update { it.copy(isAssistantTyping = true) }

                // Get AI response
                repairChatRepository.getAssistantResponse(conversationId, content)

                // Hide typing indicator
                _uiState.update { it.copy(isAssistantTyping = false) }
                _events.emit(RepairChatEvent.ScrollToBottom)
            } catch (e: Exception) {
                _uiState.update { it.copy(isAssistantTyping = false) }
                _events.emit(RepairChatEvent.ShowError(e.message ?: "Erreur d'envoi"))
            }
        }
    }
}
