package com.wattson.ui.screens.auth

/**
 * Centralizes authentication form validation rules shared by the auth screens.
 *
 * The rules stay outside the ViewModel so they can be tested in isolation and reused by
 * edit-time validation as well as submit-time validation.
 */
internal object AuthInputValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private const val maxEmailLength = 255
    const val minPasswordLength = 8

    fun validateEmailForEditing(email: String): String? = when {
        email.isBlank() -> null
        email.length > maxEmailLength -> "Email trop long (max $maxEmailLength caracteres)"
        email.contains("@") && email.contains(".") && !emailRegex.matches(email) ->
            "Format invalide \u2014 ex : nom@domaine.com"
        else -> null
    }

    fun validateEmailForSubmit(email: String): String? = when {
        email.isBlank() -> "Email requis"
        !email.contains("@") -> "Format invalide \u2014 ex : nom@domaine.com"
        !emailRegex.matches(email) -> "Format d'email invalide \u2014 ex : nom@domaine.com"
        email.length > maxEmailLength -> "Email trop long (max $maxEmailLength caracteres)"
        else -> null
    }

    fun validateLoginPassword(password: String): String? = when {
        password.isBlank() -> "Mot de passe requis"
        else -> null
    }

    fun validateRegistrationPassword(password: String): String? = when {
        password.isBlank() -> "Mot de passe requis"
        password.length < minPasswordLength -> "$minPasswordLength caracteres minimum"
        !password.any { it.isUpperCase() } -> "Une majuscule est requise"
        !password.any { it.isDigit() } -> "Un chiffre est requis"
        !password.any { !it.isLetterOrDigit() } -> "Un caractere special est requis (!@#\$%...)"
        else -> null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> "Confirmez votre mot de passe"
        confirmPassword != password -> "Les mots de passe ne correspondent pas"
        else -> null
    }
}
