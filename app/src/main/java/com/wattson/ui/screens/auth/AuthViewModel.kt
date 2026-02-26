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
 * UI State for authentication screens.
 * Represents all possible states during the authentication flow.
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
 * One-shot events for authentication flow.
 * These events are consumed once and trigger navigation or UI feedback.
 */
sealed interface AuthEvent {
    data object NavigateToMain : AuthEvent
    data object NavigateToLogin : AuthEvent
    data object NavigateToRegister : AuthEvent
    data object NavigateBack : AuthEvent
    data class ShowError(val message: String) : AuthEvent
    data class ShowSnackbar(val message: String) : AuthEvent
}

/**
 * User actions/intents from the authentication screens.
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
    data object ForgotPassword : AuthIntent
    data object NavigateToLogin : AuthIntent
    data object NavigateToRegister : AuthIntent
    data object NavigateBack : AuthIntent
    data object ClearErrors : AuthIntent
    data object AutoFillTestUser : AuthIntent
}

/**
 * ViewModel for authentication screens (Auth, Login, Register).
 * Follows UDF pattern: Intent -> State -> UI.
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
        // Check if already logged in
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    android.util.Log.d("AuthViewModel", "User already logged in, navigating to main")
                    _events.emit(AuthEvent.NavigateToMain)
                }
            }
        }
    }

    /**
     * Process user intents and update state accordingly.
     * Single entry point for all user actions.
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
            // Real-time email validation: only show error after user has typed enough
            val emailError = when {
                email.isBlank() -> null // Don't show error while field is empty
                email.length > MAX_EMAIL_LENGTH -> "Email trop long (max $MAX_EMAIL_LENGTH caracteres)"
                email.contains("@") && email.contains(".") && !EMAIL_REGEX.matches(email) ->
                    "Format invalide — ex : nom@domaine.com"
                else -> null
            }
            state.copy(
                email = email,
                emailError = emailError
            )
        }
    }

    private fun updatePassword(password: String) {
        _uiState.update { state ->
            state.copy(
                password = password,
                passwordError = null,
                // Also re-validate confirm password if it's already filled
                confirmPasswordError = if (state.confirmPassword.isNotBlank() && state.confirmPassword != password) {
                    "Les mots de passe ne correspondent pas"
                } else {
                    null
                }
            )
        }
    }

    private fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { state ->
            val confirmError = when {
                confirmPassword.isBlank() -> null
                confirmPassword != state.password -> "Les mots de passe ne correspondent pas"
                else -> null
            }
            state.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = confirmError
            )
        }
    }

    private fun updateFullName(fullName: String) {
        _uiState.update { state ->
            state.copy(fullName = fullName)
        }
    }

    private fun togglePasswordVisibility(isConfirmField: Boolean) {
        _uiState.update { state ->
            if (isConfirmField) {
                state.copy(isConfirmPasswordVisible = !state.isConfirmPasswordVisible)
            } else {
                state.copy(isPasswordVisible = !state.isPasswordVisible)
            }
        }
    }

    private fun toggleRememberMe(checked: Boolean) {
        _uiState.update { it.copy(rememberMe = checked) }
    }

    fun performLogin() {
        val currentState = _uiState.value

        // Validate inputs with detailed feedback
        val emailError = validateEmail(currentState.email)
        val passwordError = validateLoginPassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _uiState.update { state ->
                state.copy(emailError = emailError, passwordError = passwordError)
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
                    android.util.Log.d("AuthViewModel", "Login successful: ${user.email}")
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowSnackbar("Bienvenue ${user.fullName}!"))
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    android.util.Log.e("AuthViewModel", "Login failed", error)
                    val errorMessage = error.message ?: "Erreur de connexion"
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            // Show API error in the appropriate field for clear UX feedback
                            passwordError = "Email ou mot de passe incorrect"
                        )
                    }
                }
            )
        }
    }

    fun performRegister() {
        val currentState = _uiState.value

        // Validate all inputs
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)
        val confirmPasswordError = validateConfirmPassword(currentState.password, currentState.confirmPassword)

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update { state ->
                state.copy(
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
                onSuccess = { user ->
                    android.util.Log.d("AuthViewModel", "Registration successful: ${user.email}")
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowSnackbar("Compte cree avec succes!"))
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    android.util.Log.e("AuthViewModel", "Registration failed", error)
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowError(error.message ?: "Erreur d'inscription"))
                }
            )
        }
    }

    fun loginWithProvider(provider: AuthProvider) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = authRepository.loginWithProvider(provider)

            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowSnackbar("Connexion ${provider.name} reussie!"))
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.ShowError("Erreur ${provider.name}: ${error.message}"))
                }
            )
        }
    }

    fun handleForgotPassword() {
        val currentState = _uiState.value
        val emailError = validateEmail(currentState.email)

        if (emailError != null) {
            _uiState.update { it.copy(emailError = "Entrez d'abord un email valide") }
            return
        }

        viewModelScope.launch {
            _events.emit(AuthEvent.ShowSnackbar("Email de reinitialisation envoye a ${currentState.email}"))
        }
    }

    private fun clearErrors() {
        _uiState.update { state ->
            state.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }

    private fun emitEvent(event: AuthEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    // ===== Validation helpers =====

    /**
     * Validates email format with detailed user feedback.
     * Used on both Login and Register screens.
     */
    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email requis"
            !email.contains("@") -> "Format invalide — ex : nom@domaine.com"
            !EMAIL_REGEX.matches(email) -> "Format d'email invalide — ex : nom@domaine.com"
            email.length > MAX_EMAIL_LENGTH -> "Email trop long (max $MAX_EMAIL_LENGTH caracteres)"
            else -> null
        }
    }

    /**
     * Validates password for LOGIN only — we don't reveal password policy on login
     * to avoid giving hints to attackers. Just check it's not blank.
     */
    private fun validateLoginPassword(password: String): String? {
        return when {
            password.isBlank() -> "Mot de passe requis"
            else -> null
        }
    }

    /**
     * Validates password for REGISTRATION — full policy feedback.
     * Returns the FIRST unmet requirement for clear UX guidance.
     */
    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Mot de passe requis"
            password.length < MIN_PASSWORD_LENGTH -> "$MIN_PASSWORD_LENGTH caracteres minimum"
            !password.any { it.isUpperCase() } -> "Une majuscule est requise"
            !password.any { it.isDigit() } -> "Un chiffre est requis"
            !password.any { !it.isLetterOrDigit() } -> "Un caractere special est requis (!@#\$%...)"
            else -> null
        }
    }

    /**
     * Validates confirm password matches.
     */
    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Confirmez votre mot de passe"
            confirmPassword != password -> "Les mots de passe ne correspondent pas"
            else -> null
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_EMAIL_LENGTH = 255
        const val MIN_PASSWORD_LENGTH = 8
    }
}
