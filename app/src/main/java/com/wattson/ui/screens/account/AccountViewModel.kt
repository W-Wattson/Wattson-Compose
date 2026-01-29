package com.wattson.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.AuthProvider
import com.wattson.domain.model.PreferenceType
import com.wattson.domain.model.SubscriptionType
import com.wattson.domain.model.User
import com.wattson.domain.model.UserPreferences
import com.wattson.domain.model.getDocumentLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * UI State for the Account screen.
 */
data class AccountUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val preferences: UserPreferences? = null,
    val isPremium: Boolean = false,
    val documentCount: Int = 0,
    val documentLimit: Int? = null,
    val errorMessage: String? = null,
    val showLogoutConfirmation: Boolean = false,
    val isUpdatingPreferences: Boolean = false
)

/**
 * One-shot events for account screen.
 */
sealed interface AccountEvent {
    data object NavigateToPremium : AccountEvent
    data object NavigateToLogin : AccountEvent
    data class ShowError(val message: String) : AccountEvent
    data object LogoutSuccess : AccountEvent
    data object PreferencesUpdated : AccountEvent
}

/**
 * User intents for account screen.
 */
sealed interface AccountIntent {
    data object LoadProfile : AccountIntent
    data object RefreshProfile : AccountIntent
    data class UpdatePreferenceOrder(val newOrder: List<PreferenceType>) : AccountIntent
    data object NavigateToPremium : AccountIntent
    data object RequestLogout : AccountIntent
    data object ConfirmLogout : AccountIntent
    data object CancelLogout : AccountIntent
    data object DismissError : AccountIntent
}

/**
 * ViewModel for the Account screen.
 * Manages user profile and preferences.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val getCurrentUserUseCase: GetCurrentUserUseCase,
    // private val updatePreferencesUseCase: UpdatePreferencesUseCase,
    // private val logoutUseCase: LogoutUseCase,
    // private val getUserDocumentCountUseCase: GetUserDocumentCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AccountEvent>()
    val events = _events.asSharedFlow()

    init {
        loadProfile()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: AccountIntent) {
        when (intent) {
            is AccountIntent.LoadProfile -> loadProfile()
            is AccountIntent.RefreshProfile -> refreshProfile()
            is AccountIntent.UpdatePreferenceOrder -> updatePreferenceOrder(intent.newOrder)
            is AccountIntent.NavigateToPremium -> navigateToPremium()
            is AccountIntent.RequestLogout -> requestLogout()
            is AccountIntent.ConfirmLogout -> confirmLogout()
            is AccountIntent.CancelLogout -> cancelLogout()
            is AccountIntent.DismissError -> dismissError()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // TODO: Replace with actual use cases
                // val user = getCurrentUserUseCase()
                // val docCount = getUserDocumentCountUseCase()
                
                // Mock data for development
                val user = getMockUser()
                val docCount = 8

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        preferences = user.preferences,
                        isPremium = user.subscriptionType != SubscriptionType.FREE,
                        documentCount = docCount,
                        documentLimit = user.subscriptionType.getDocumentLimit()
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur lors du chargement"
                    )
                }
            }
        }
    }

    private fun refreshProfile() {
        loadProfile()
    }

    private fun updatePreferenceOrder(newOrder: List<PreferenceType>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPreferences = true) }

            try {
                // TODO: Replace with actual use case
                // updatePreferencesUseCase(newOrder)
                
                kotlinx.coroutines.delay(500)

                val updatedPreferences = UserPreferences(orderedPreferences = newOrder)
                
                _uiState.update {
                    it.copy(
                        isUpdatingPreferences = false,
                        preferences = updatedPreferences,
                        user = it.user?.copy(preferences = updatedPreferences)
                    )
                }
                
                _events.emit(AccountEvent.PreferencesUpdated)
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingPreferences = false,
                        errorMessage = e.message ?: "Erreur lors de la mise à jour"
                    )
                }
            }
        }
    }

    private fun navigateToPremium() {
        viewModelScope.launch {
            _events.emit(AccountEvent.NavigateToPremium)
        }
    }

    private fun requestLogout() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    private fun confirmLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(showLogoutConfirmation = false, isLoading = true) }

            try {
                // TODO: Replace with actual logout
                // logoutUseCase()
                
                kotlinx.coroutines.delay(300)
                
                _events.emit(AccountEvent.LogoutSuccess)
                _events.emit(AccountEvent.NavigateToLogin)
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur lors de la déconnexion"
                    )
                }
            }
        }
    }

    private fun cancelLogout() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Mock data for development
    private fun getMockUser(): User = User(
        id = "user_1",
        email = "amelie.brun@gmail.com",
        fullName = "Amélie Brun",
        authProvider = AuthProvider.EMAIL,
        subscriptionType = SubscriptionType.FREE,
        preferences = UserPreferences(
            orderedPreferences = listOf(
                PreferenceType.ECONOMY,
                PreferenceType.ECOLOGY,
                PreferenceType.REPAIRABILITY
            )
        ),
        createdAt = Instant.parse("2024-06-15T10:00:00Z"),
        updatedAt = Instant.now()
    )
}
