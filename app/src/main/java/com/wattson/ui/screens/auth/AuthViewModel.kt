package com.wattson.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val rememberMe: Boolean = false
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
 * Follows UDF pattern: Intent → State → UI.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val loginUseCase: LoginUseCase,
    // private val registerUseCase: RegisterUseCase,
    // private val loginWithProviderUseCase: LoginWithProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    /**
     * Process user intents and update state accordingly.
     * Single entry point for all user actions.
     */
    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UpdateEmail -> updateEmail(intent.email)
            is AuthIntent.UpdatePassword -> updatePassword(intent.password)
            is AuthIntent.UpdateConfirmPassword -> updateConfirmPassword(intent.confirmPassword)
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
            state.copy(
                email = email,
                emailError = validateEmail(email)
            )
        }
    }

    private fun updatePassword(password: String) {
        _uiState.update { state ->
            state.copy(
                password = password,
                passwordError = validatePassword(password)
            )
        }
    }

    private fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { state ->
            state.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = validateConfirmPassword(state.password, confirmPassword)
            )
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
        
        // Validate inputs
        val emailError = validateEmail(currentState.email)
        val passwordError = if (currentState.password.isBlank()) "Password is required" else null
        
        if (emailError != null || passwordError != null) {
            _uiState.update { state ->
                state.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Simulate API call
                kotlinx.coroutines.delay(1000)
                
                // Allow specific test user for navigation
                if (currentState.email == "test@wattson.com" && currentState.password == "Password123!") {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.NavigateToMain)
                } else if (currentState.email.isNotBlank() && currentState.password.isNotBlank()) {
                    // For now, allow any non-empty credentials to ease testing
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.NavigateToMain)
                } else {
                    throw Exception("Identifiants invalides")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthEvent.ShowError(e.message ?: "Login failed"))
            }
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
            
            try {
                // TODO: Replace with actual register use case
                // val result = registerUseCase(currentState.email, currentState.password)
                kotlinx.coroutines.delay(1500)
                
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthEvent.ShowSnackbar("Account created successfully"))
                _events.emit(AuthEvent.NavigateToMain)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthEvent.ShowError(e.message ?: "Registration failed"))
            }
        }
    }

    fun loginWithProvider(provider: AuthProvider) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // TODO: Implement OAuth flow
                // val result = loginWithProviderUseCase(provider)
                kotlinx.coroutines.delay(1000)
                
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthEvent.NavigateToMain)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthEvent.ShowError("${provider.name} login failed"))
            }
        }
    }

    fun handleForgotPassword() {
        val currentState = _uiState.value
        val emailError = validateEmail(currentState.email)
        
        if (emailError != null) {
            _uiState.update { it.copy(emailError = "Please enter a valid email first") }
            return
        }

        viewModelScope.launch {
            _events.emit(AuthEvent.ShowSnackbar("Password reset email sent to ${currentState.email}"))
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

    // Validation helpers
    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !EMAIL_REGEX.matches(email) -> "Invalid email format"
            email.length > MAX_EMAIL_LENGTH -> "Email too long"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
            !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
            !password.any { it.isDigit() } -> "Password must contain at least one digit"
            !password.any { !it.isLetterOrDigit() } -> "Password must contain at least one special character"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_EMAIL_LENGTH = 255
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
