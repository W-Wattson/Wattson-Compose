package com.wattson.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.domain.model.AuthProvider
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

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val rememberMe: Boolean = true
)

sealed interface AuthEvent {
    data object NavigateToMain : AuthEvent
    data object NavigateToLogin : AuthEvent
    data object NavigateToRegister : AuthEvent
    data object NavigateBack : AuthEvent
    data object RequestGoogleSignIn : AuthEvent
    data class ShowError(val message: UiText) : AuthEvent
    data class ShowSnackbar(val message: UiText) : AuthEvent
}

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
}

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
                    android.util.Log.d("AuthViewModel", "User already logged in, navigating to main")
                    _events.emit(AuthEvent.NavigateToMain)
                }
            }
        }
    }

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
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { state ->
            val emailError = when {
                email.isBlank() -> null
                email.length > MAX_EMAIL_LENGTH -> UiText.StringResource(
                    R.string.validation_email_too_long,
                    MAX_EMAIL_LENGTH
                )

                email.contains("@") && email.contains(".") && !EMAIL_REGEX.matches(email) ->
                    UiText.StringResource(R.string.validation_email_format_example)

                else -> null
            }
            state.copy(email = email, emailError = emailError)
        }
    }

    private fun updatePassword(password: String) {
        _uiState.update { state ->
            state.copy(
                password = password,
                passwordError = null,
                confirmPasswordError = if (
                    state.confirmPassword.isNotBlank() && state.confirmPassword != password
                ) {
                    UiText.StringResource(R.string.validation_passwords_do_not_match)
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
                confirmPassword != state.password ->
                    UiText.StringResource(R.string.validation_passwords_do_not_match)

                else -> null
            }
            state.copy(confirmPassword = confirmPassword, confirmPasswordError = confirmError)
        }
    }

    private fun updateFullName(fullName: String) {
        _uiState.update { it.copy(fullName = fullName) }
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
        val emailError = validateEmail(currentState.email)
        val passwordError = validateLoginPassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
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
                    _events.emit(
                        AuthEvent.ShowSnackbar(
                            UiText.StringResource(
                                R.string.auth_welcome_user,
                                user.fullName ?: user.email.substringBefore("@")
                            )
                        )
                    )
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordError = UiText.StringResource(R.string.auth_email_or_password_incorrect)
                        )
                    }
                }
            )
        }
    }

    fun performRegister() {
        val currentState = _uiState.value
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)
        val confirmPasswordError = validateConfirmPassword(
            currentState.password,
            currentState.confirmPassword
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
                    _events.emit(
                        AuthEvent.ShowSnackbar(
                            UiText.StringResource(R.string.auth_account_created_success)
                        )
                    )
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowError(
                            error.toUiTextOr(UiText.StringResource(R.string.error_registration_generic))
                        )
                    )
                }
            )
        }
    }

    fun loginWithProvider(provider: AuthProvider) {
        when (provider) {
            AuthProvider.GOOGLE -> emitEvent(AuthEvent.RequestGoogleSignIn)
            else -> emitEvent(
                AuthEvent.ShowError(
                    UiText.StringResource(R.string.auth_provider_not_available)
                )
            )
        }
    }

    fun handleGoogleSignInError(message: UiText) {
        _uiState.update { it.copy(isLoading = false) }
        emitEvent(AuthEvent.ShowError(message))
    }

    fun handleGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.loginWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowSnackbar(
                            UiText.StringResource(R.string.auth_google_sign_in_success)
                        )
                    )
                    _events.emit(AuthEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowError(
                            error.toUiTextOr(UiText.StringResource(R.string.error_google_sign_in_generic))
                        )
                    )
                }
            )
        }
    }

    fun handleForgotPassword() {
        val currentState = _uiState.value
        val emailError = validateEmail(currentState.email)

        if (emailError != null) {
            _uiState.update {
                it.copy(emailError = UiText.StringResource(R.string.auth_enter_valid_email_first))
            }
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
                            UiText.StringResource(R.string.auth_forgot_password_sent)
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        AuthEvent.ShowError(
                            error.toUiTextOr(UiText.StringResource(R.string.error_send_generic))
                        )
                    )
                }
            )
        }
    }

    private fun clearErrors() {
        _uiState.update {
            it.copy(emailError = null, passwordError = null, confirmPasswordError = null)
        }
    }

    private fun emitEvent(event: AuthEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun validateEmail(email: String): UiText? = when {
        email.isBlank() -> UiText.StringResource(R.string.validation_email_required)
        !email.contains("@") -> UiText.StringResource(R.string.validation_email_format_example)
        !EMAIL_REGEX.matches(email) ->
            UiText.StringResource(R.string.validation_email_format_invalid)

        email.length > MAX_EMAIL_LENGTH ->
            UiText.StringResource(R.string.validation_email_too_long, MAX_EMAIL_LENGTH)

        else -> null
    }

    private fun validateLoginPassword(password: String): UiText? = when {
        password.isBlank() -> UiText.StringResource(R.string.validation_password_required)
        else -> null
    }

    private fun validatePassword(password: String): UiText? = when {
        password.isBlank() -> UiText.StringResource(R.string.validation_password_required)
        password.length < MIN_PASSWORD_LENGTH ->
            UiText.StringResource(R.string.validation_password_min_length, MIN_PASSWORD_LENGTH)

        !password.any { it.isUpperCase() } ->
            UiText.StringResource(R.string.validation_password_uppercase_required)

        !password.any { it.isDigit() } ->
            UiText.StringResource(R.string.validation_password_digit_required)

        !password.any { !it.isLetterOrDigit() } ->
            UiText.StringResource(R.string.validation_password_special_required)

        else -> null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): UiText? = when {
        confirmPassword.isBlank() ->
            UiText.StringResource(R.string.validation_confirm_password_required)

        confirmPassword != password ->
            UiText.StringResource(R.string.validation_passwords_do_not_match)

        else -> null
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_EMAIL_LENGTH = 255
        const val MIN_PASSWORD_LENGTH = 8
    }
}
