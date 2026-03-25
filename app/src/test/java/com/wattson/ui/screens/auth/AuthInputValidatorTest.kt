package com.wattson.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInputValidatorTest {

    @Test
    fun `editing validation ignores blank email`() {
        assertNull(AuthInputValidator.validateEmailForEditing(""))
    }

    @Test
    fun `submit validation rejects malformed email`() {
        assertEquals(
            "Format d'email invalide \u2014 ex : nom@domaine.com",
            AuthInputValidator.validateEmailForSubmit("invalid@email")
        )
    }

    @Test
    fun `registration password validation requires uppercase digit and special character`() {
        assertEquals(
            "Une majuscule est requise",
            AuthInputValidator.validateRegistrationPassword("password1!")
        )
        assertEquals(
            "Un chiffre est requis",
            AuthInputValidator.validateRegistrationPassword("Password!")
        )
        assertEquals(
            "Un caractere special est requis (!@#$%...)",
            AuthInputValidator.validateRegistrationPassword("Password1")
        )
    }

    @Test
    fun `confirm password validation rejects mismatch`() {
        assertEquals(
            "Les mots de passe ne correspondent pas",
            AuthInputValidator.validateConfirmPassword(
                password = "Password1!",
                confirmPassword = "Password2!"
            )
        )
    }
}
