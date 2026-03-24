package com.wattson.ui.screens.repair

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.RepairChatRepository
import com.wattson.domain.model.RepairConversation
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepairUiState(
    val isLoading: Boolean = false,
    val conversations: List<RepairConversation> = emptyList(),
    val errorMessage: UiText? = null
)

sealed interface RepairEvent {
    data class NavigateToChat(val conversationId: String) : RepairEvent
    data class ShowError(val message: UiText) : RepairEvent
}

sealed interface RepairIntent {
    data object LoadConversations : RepairIntent
    data object CreateNewConversation : RepairIntent
    data class OpenConversation(val conversationId: String) : RepairIntent
    data class DeleteConversation(val conversationId: String) : RepairIntent
    data object DismissError : RepairIntent
}

@HiltViewModel
class RepairViewModel @Inject constructor(
    private val repairChatRepository: RepairChatRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepairUiState())
    val uiState: StateFlow<RepairUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RepairEvent>()
    val events = _events.asSharedFlow()

    init {
        loadConversations()
    }

    fun onIntent(intent: RepairIntent) {
        when (intent) {
            is RepairIntent.LoadConversations -> loadConversations()
            is RepairIntent.CreateNewConversation -> createNewConversation()
            is RepairIntent.OpenConversation -> openConversation(intent.conversationId)
            is RepairIntent.DeleteConversation -> deleteConversation(intent.conversationId)
            is RepairIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = authRepository.getCurrentUserId()
                repairChatRepository.refreshConversations(userId)
                repairChatRepository.getConversations(userId).collect { conversations ->
                    _uiState.update {
                        it.copy(isLoading = false, conversations = conversations)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_loading_generic)
                        )
                    )
                }
            }
        }
    }

    private fun createNewConversation() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val conversation = repairChatRepository.createConversation(
                    userId = userId,
                    title = appContext.getString(R.string.repair_new_conversation)
                )
                _events.emit(RepairEvent.NavigateToChat(conversation.id))
            } catch (e: Exception) {
                _events.emit(
                    RepairEvent.ShowError(
                        e.toUiTextOr(UiText.StringResource(R.string.error_create_generic))
                    )
                )
            }
        }
    }

    private fun openConversation(conversationId: String) {
        viewModelScope.launch {
            _events.emit(RepairEvent.NavigateToChat(conversationId))
        }
    }

    private fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                repairChatRepository.deleteConversation(conversationId, userId)
            } catch (e: Exception) {
                _events.emit(
                    RepairEvent.ShowError(
                        e.toUiTextOr(UiText.StringResource(R.string.error_delete_generic))
                    )
                )
            }
        }
    }
}
