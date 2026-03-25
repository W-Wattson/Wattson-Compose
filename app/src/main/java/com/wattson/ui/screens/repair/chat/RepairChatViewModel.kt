package com.wattson.ui.screens.repair.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.RepairChatRepository
import com.wattson.domain.model.ChatMessage
import com.wattson.domain.model.MessageRole
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.UserFacingException
import com.wattson.ui.i18n.toUiTextOr
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

data class RepairChatUiState(
    val conversationId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isAssistantTyping: Boolean = false,
    val errorMessage: UiText? = null
)

sealed interface RepairChatEvent {
    data object ScrollToBottom : RepairChatEvent
    data class ShowError(val message: UiText) : RepairChatEvent
}

sealed interface RepairChatIntent {
    data class UpdateInput(val text: String) : RepairChatIntent
    data object SendMessage : RepairChatIntent
    data object DismissError : RepairChatIntent
}

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

    fun onIntent(intent: RepairChatIntent) {
        when (intent) {
            is RepairChatIntent.UpdateInput -> _uiState.update { it.copy(inputText = intent.text) }
            is RepairChatIntent.SendMessage -> sendMessage()
            is RepairChatIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            val result = repairChatRepository.getMessagesList(conversationId, userId)

            result.fold(
                onSuccess = { messages ->
                    _uiState.update { it.copy(messages = messages) }
                    if (messages.isNotEmpty()) {
                        _events.emit(RepairChatEvent.ScrollToBottom)
                    }
                },
                onFailure = { error ->
                    _events.emit(
                        RepairChatEvent.ShowError(
                            error.toUiTextOr(UiText.StringResource(R.string.error_loading_generic))
                        )
                    )
                }
            )
        }
    }

    private fun rollbackOptimisticMessage(messageId: String, content: String) {
        _uiState.update { state ->
            state.copy(
                inputText = content,
                isAssistantTyping = false,
                messages = state.messages.filterNot { it.id == messageId }
            )
        }
    }

    private fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || _uiState.value.isAssistantTyping) {
            return
        }

        viewModelScope.launch {
            val optimisticUserMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = content,
                timestamp = Instant.now()
            )

            try {
                _uiState.update {
                    it.copy(
                        inputText = "",
                        messages = it.messages + optimisticUserMessage
                    )
                }
                _events.emit(RepairChatEvent.ScrollToBottom)

                _uiState.update { it.copy(isAssistantTyping = true) }

                val userId = authRepository.getCurrentUserId()
                val subscription = authRepository.currentUser.value?.subscriptionType?.name ?: "FREE"

                val assistantResponse = repairChatRepository.sendMessageAndGetResponse(
                    conversationId = conversationId,
                    userId = userId,
                    subscription = subscription,
                    content = content
                )

                _uiState.update {
                    it.copy(
                        isAssistantTyping = false,
                        messages = it.messages + assistantResponse
                    )
                }
                _events.emit(RepairChatEvent.ScrollToBottom)
            } catch (exception: UserFacingException) {
                rollbackOptimisticMessage(
                    messageId = optimisticUserMessage.id,
                    content = content
                )
                _events.emit(RepairChatEvent.ShowError(exception.uiText))
            } catch (exception: Exception) {
                rollbackOptimisticMessage(
                    messageId = optimisticUserMessage.id,
                    content = content
                )
                _events.emit(
                    RepairChatEvent.ShowError(
                        exception.toUiTextOr(UiText.StringResource(R.string.error_send_generic))
                    )
                )
            }
        }
    }
}
