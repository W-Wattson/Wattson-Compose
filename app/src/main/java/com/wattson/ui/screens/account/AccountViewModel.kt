package com.wattson.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.remote.api.WattsonApi
import com.wattson.data.repository.AuthRepository
import com.wattson.domain.model.PreferenceType
import com.wattson.domain.model.SubscriptionType
import com.wattson.domain.model.User
import com.wattson.domain.model.UserPreferences
import com.wattson.domain.model.getDocumentLimit
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val preferences: UserPreferences? = null,
    val isPremium: Boolean = false,
    val documentCount: Int = 0,
    val documentLimit: Int? = null,
    val scanCount: Int = 0,
    val errorMessage: UiText? = null,
    val showLogoutConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isUpdatingPreferences: Boolean = false
)

sealed interface AccountEvent {
    data object NavigateToPremium : AccountEvent
    data object NavigateToLogin : AccountEvent
    data class ShowError(val message: UiText) : AccountEvent
    data object LogoutSuccess : AccountEvent
    data object AccountDeleted : AccountEvent
    data object PreferencesUpdated : AccountEvent
}

sealed interface AccountIntent {
    data object LoadProfile : AccountIntent
    data object RefreshProfile : AccountIntent
    data class UpdatePreferenceOrder(val newOrder: List<PreferenceType>) : AccountIntent
    data object NavigateToPremium : AccountIntent
    data object RequestLogout : AccountIntent
    data object ConfirmLogout : AccountIntent
    data object CancelLogout : AccountIntent
    data object RequestDeleteAccount : AccountIntent
    data object ConfirmDeleteAccount : AccountIntent
    data object CancelDeleteAccount : AccountIntent
    data object DismissError : AccountIntent
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: WattsonApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AccountEvent>()
    val events = _events.asSharedFlow()

    init {
        loadProfile()
        observeUserChanges()
    }

    private fun observeUserChanges() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            preferences = user.preferences,
                            isPremium = user.subscriptionType != SubscriptionType.FREE,
                            documentLimit = user.subscriptionType.getDocumentLimit()
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: AccountIntent) {
        when (intent) {
            is AccountIntent.LoadProfile -> loadProfile()
            is AccountIntent.RefreshProfile -> refreshProfile()
            is AccountIntent.UpdatePreferenceOrder -> updatePreferenceOrder(intent.newOrder)
            is AccountIntent.NavigateToPremium -> navigateToPremium()
            is AccountIntent.RequestLogout -> requestLogout()
            is AccountIntent.ConfirmLogout -> confirmLogout()
            is AccountIntent.CancelLogout -> cancelLogout()
            is AccountIntent.RequestDeleteAccount -> requestDeleteAccount()
            is AccountIntent.ConfirmDeleteAccount -> confirmDeleteAccount()
            is AccountIntent.CancelDeleteAccount -> cancelDeleteAccount()
            is AccountIntent.DismissError -> dismissError()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val user = authRepository.currentUser.value

                if (user == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AccountEvent.NavigateToLogin)
                    return@launch
                }

                var documentCount = 0
                var scanCount = 0

                try {
                    val token = authRepository.getAccessToken()
                    if (!token.isNullOrBlank()) {
                        val docResponse = api.getDocuments("Bearer $token", user.id, limit = 1, offset = 0)
                        if (docResponse.isSuccessful) {
                            documentCount = docResponse.body()?.total?.toInt() ?: 0
                        }
                    } else {
                        android.util.Log.w("AccountViewModel", "Missing access token for document count")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AccountViewModel", "Failed to fetch documents", e)
                }

                try {
                    val scanResponse = api.getScanHistory(user.id, page = 0, size = 1)
                    if (scanResponse.isSuccessful) {
                        scanCount = scanResponse.body()?.totalCount?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AccountViewModel", "Failed to fetch scan history", e)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        preferences = user.preferences,
                        isPremium = user.subscriptionType != SubscriptionType.FREE,
                        documentCount = documentCount,
                        documentLimit = user.subscriptionType.getDocumentLimit(),
                        scanCount = scanCount
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AccountViewModel", "Failed to load profile", e)
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

    private fun refreshProfile() {
        loadProfile()
    }

    private fun updatePreferenceOrder(newOrder: List<PreferenceType>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPreferences = true) }

            try {
                val updatedPreferences = UserPreferences(orderedPreferences = newOrder)
                val result = authRepository.updatePreferences(updatedPreferences)

                result.fold(
                    onSuccess = { updatedUser ->
                        _uiState.update {
                            it.copy(
                                isUpdatingPreferences = false,
                                preferences = updatedUser.preferences,
                                user = updatedUser
                            )
                        }
                        _events.emit(AccountEvent.PreferencesUpdated)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isUpdatingPreferences = false,
                                errorMessage = error.toUiTextOr(
                                    UiText.StringResource(R.string.error_update_generic)
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingPreferences = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_update_generic)
                        )
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
                val result = authRepository.logout()

                result.fold(
                    onSuccess = {
                        android.util.Log.d("AccountViewModel", "Logout successful")
                        _uiState.update { it.copy(isLoading = false, user = null) }
                        _events.emit(AccountEvent.LogoutSuccess)
                        _events.emit(AccountEvent.NavigateToLogin)
                    },
                    onFailure = { error ->
                        android.util.Log.e("AccountViewModel", "Logout failed", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.toUiTextOr(
                                    UiText.StringResource(R.string.error_logout_generic)
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_logout_generic)
                        )
                    )
                }
            }
        }
    }

    private fun cancelLogout() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    private fun requestDeleteAccount() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = true) }
    }

    private fun confirmDeleteAccount() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(showDeleteAccountConfirmation = false, isDeletingAccount = true)
            }

            try {
                val result = authRepository.deleteAccount()

                result.fold(
                    onSuccess = {
                        _uiState.update { state ->
                            state.copy(isDeletingAccount = false, user = null)
                        }
                        _events.emit(AccountEvent.AccountDeleted)
                        _events.emit(AccountEvent.NavigateToLogin)
                    },
                    onFailure = { error ->
                        android.util.Log.e("AccountViewModel", "Delete account failed", error)
                        _uiState.update {
                            it.copy(
                                isDeletingAccount = false,
                                errorMessage = error.toUiTextOr(
                                    UiText.StringResource(R.string.error_delete_generic)
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_delete_generic)
                        )
                    )
                }
            }
        }
    }

    private fun cancelDeleteAccount() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
