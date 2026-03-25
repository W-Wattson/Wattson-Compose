package com.wattson.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.data.repository.AuthRepository
import com.wattson.domain.model.AuthProvider
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
 * UI state for the authentication flow.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val rememberMe: Boolean = true
)

/**
 * One-shot events emitted by the authentication flow.
 */
sealed interface AuthEvent {
    data object NavigateToMain : AuthEvent
    data object NavigateToLogin : AuthEvent
    data object NavigateToRegister : AuthEvent
    data object NavigateBack : AuthEvent
    data object RequestGoogleSignIn : AuthEvent
    data class ShowError(val message: String) : AuthEvent
    data class ShowSnackbar(val message: String) : AuthEvent
}

/**
 * User intents dispatched from the authentication screens.
 */
sealed interface AuthIntent {
    data class UpdateEmail(val email: String) : AuthIntent
    data class UpdatePassword(val password: String) : AuthIntent
    data class UpdateConfirmPassword(val confirmPassword: String) : AuthIntent
    data class UpdateFullName(val fullName: String) : AuthIntent
    data class TogglePasswordVisibility(val isConfirmField: Boolean = false) : AuthIntent
    data class ToggleRememberMe(val checked: Boolean) : AuthIntent
    data object Login : AuthIntent
    data object Register : AuthIntent
    data class LoginWithProvider(val provider: AuthProvider) : AuthIntent
    data class GoogleIdTokenReceived(val idToken: String) : AuthIntent
    data object ForgotPassword : AuthIntent
    data object NavigateToLogin : AuthIntent
    data object NavigateToRegister : AuthIntent
    data object NavigateBack : AuthIntent
    data object ClearErrors : AuthIntent
    data object AutoFillTestUser : AuthIntent
}

/**
 * ViewModel for the authentication entry, login, and registration screens.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    android.util.Log.d(
                        "AuthViewModel",
                        "User already logged in, navigating to main"
                    )
                    _events.emit(AuthEvent.NavigateToMain)
                }
            }
        }
    }

    /**
     * Processes all user intents through a single entry point.
     */
    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UpdateEmail -> updateEmail(intent.email)
            is AuthIntent.UpdatePassword -> updatePassword(intent.password)
            is AuthIntent.UpdateConfirmPassword -> updateConfirmPassword(intent.confirmPassword)
            is AuthIntent.UpdateFullName -> updateFullName(intent.fullName)
            is AuthIntent.TogglePasswordVisibility -> togglePasswordVisibility(intent.isConfirmField)
            is AuthIntent.ToggleRememberMe -> toggleRememberMe(intent.checked)
            is AuthIntent.Login -> performLogin()
            is AuthIntent.Register -> performRegister()
            is AuthIntent.LoginWithProvider -> loginWithProvider(intent.provider)
            is AuthIntent.GoogleIdTokenReceived -> handleGoogleIdToken(intent.idToken)
            is AuthIntent.ForgotPassword -> handleForgotPassword()
            is AuthIntent.NavigateToLogin -> emitEvent(AuthEvent.NavigateToLogin)
            is AuthIntent.NavigateToRegister -> emitEvent(AuthEvent.NavigateToRegister)
            is AuthIntent.NavigateBack -> emitEvent(AuthEvent.NavigateBack)
            is AuthIntent.ClearErrors -> clearErrors()
            is AuthIntent.AutoFillTestUser -> autoFillTestUser()
        }
    }

    private fun autoFillTestUser() {
        _uiState.update {
            it.copy(
                email = "test@wattson.com",
                password = "Password123!",
                emailError = null,
                passwordError = null
            )
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { state ->
            state.copy(
                email = email,
                emailError = AuthInputValidator.validateEmailForEditing(email)
            )
        }
    }

    private fun updatePassword(password: String) {
        _uiState.update { state ->
            state.copy(
                password = password,
                passwordError = null,
                confirmPasswordError = if (
                    state.confirmPassword.isNotBlank() &&
                    state.confirmPassword != password
                ) {
                    "Les mots de passe ne correspondent pas"
                } else {
                    null
                }
            )
        }
    }

    private fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { state ->
            state.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = if (
                    confirmPassword.isBlank() ||
                    confirmPassword == state.password
                ) {
                    null
                } else {
                    "Les mots de passe ne correspondent pas"
                }
            )
        }
    }

    private fun updateFullName(fullName: String) {
        _uiState.update { it.copy(fullName = fullName) }
    }

    private fun togglePasswordVisibility(isConfirmField: Boolean) {
        _uiState.update { state ->
            if (isConfirmField) {
                state.copy(
                    isConfirmPasswordVisible = !state.isConfirmPasswordVisible
                )
            } else {
                state.copy(
                    isPasswordVisible = !state.isPasswordVisible
                )
            }
        }
    }

    private fun toggleRememberMe(checked: Boolean) {
        _uiState.update { it.copy(rememberMe = checked) }
    }

    fun performLogin() {
        val currentState = _uiState.value
        val emailError = AuthInputValidator.validateEmailForSubmit(currentState.email)
        val passwordError = AuthInputValidator.validateLoginPassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.login(
                email = currentState.email,
                password = currentState.password,
                rememberMe = currentState.rememberMe
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowSnackbar("Bienvenue ${user.fullName}!"))
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordError = "Email ou mot de passe incorrect"
                        )
                    }
                }
            )
        }
    }

    fun performRegister() {
        val currentState = _uiState.value
        val emailError = AuthInputValidator.validateEmailForSubmit(currentState.email)
        val passwordError = AuthInputValidator.validateRegistrationPassword(currentState.password)
        val confirmPasswordError = AuthInputValidator.validateConfirmPassword(
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.register(
                email = currentState.email,
                password = currentState.password,
                fullName = currentState.fullName.takeIf { it.isNotBlank() }
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowSnackbar("Compte cree avec succes!"))
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowError(
                            error.message ?: "Erreur d'inscription"
                        )
                    )
                }
            )
        }
    }

    /**
     * Triggers a third-party login flow from the UI layer.
     */
    fun loginWithProvider(provider: AuthProvider) {
        when (provider) {
            AuthProvider.GOOGLE -> emitEvent(AuthEvent.RequestGoogleSignIn)
            else -> emitEvent(
                AuthEvent.ShowError("Ce fournisseur n'est pas encore disponible")
            )
        }
    }

    /**
     * Updates state after a Google Sign-In failure surfaced by the UI layer.
     */
    fun handleGoogleSignInError(message: String) {
        _uiState.update { it.copy(isLoading = false) }
        emitEvent(AuthEvent.ShowError(message))
    }

    /**
     * Sends the Google ID token returned by Credential Manager to the backend.
     */
    fun handleGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.loginWithGoogle(idToken)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowSnackbar("Connexion Google reussie!"))
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowError(
                            error.message ?: "Erreur de connexion Google"
                        )
                    )
                }
            )
        }
    }

    /**
     * Requests a password reset without revealing whether the email exists.
     */
    fun handleForgotPassword() {
        val currentState = _uiState.value
        val emailError = AuthInputValidator.validateEmailForSubmit(currentState.email)

        if (emailError != null) {
            _uiState.update { it.copy(emailError = "Entrez d'abord un email valide") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.forgotPassword(currentState.email)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowSnackbar(
                            "Si un compte existe avec cet email, un lien de reinitialisation a ete envoye."
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowError(
                            error.message ?: "Erreur lors de l'envoi"
                        )
                    )
                }
            )
        }
    }

    private fun clearErrors() {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }

    private fun emitEvent(event: AuthEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}
