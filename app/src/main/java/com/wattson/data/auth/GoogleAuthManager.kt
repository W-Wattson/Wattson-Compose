package com.wattson.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.wattson.BuildConfig
import android.util.Log
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Sign-In using the Credential Manager API.
 *
 * This class wraps the Android Credential Manager and handles the
 * Google ID token retrieval flow. It requires an Activity context
 * to display the account picker UI.
 *
 * Includes an automatic retry mechanism for the first attempt, as Google Play
 * Services often returns BAD_AUTHENTICATION on cold starts.
 *
 * @see <a href="https://developer.android.com/identity/sign-in/credential-manager-siwg">
 *     Sign in with Google using Credential Manager</a>
 */
@Singleton
class GoogleAuthManager @Inject constructor() {

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val MAX_RETRIES = 1
        private const val RETRY_DELAY_MS = 500L
    }

    /**
     * Launches the Google Sign-In flow via Credential Manager and returns
     * the Google ID token on success.
     *
     * Automatically retries once on [NoCredentialException], as Google Play Services
     * often fails on the first attempt after a cold start (BAD_AUTHENTICATION).
     *
     * @param activityContext The Activity context (required for the picker UI).
     * @return The Google ID token string.
     * @throws GoogleAuthException on failure (cancellation, no accounts, parsing error, etc.).
     */
    suspend fun getGoogleIdToken(activityContext: Context): String {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            throw GoogleAuthException(
                "Google Sign-In non configure. " +
                "Ajoutez google.web.client.id dans local.properties."
            )
        }

        val credentialManager = CredentialManager.create(activityContext)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        var lastException: Exception? = null

        // Retry loop: Google Play Services often fails on first cold-start attempt
        for (attempt in 0..MAX_RETRIES) {
            try {
                if (attempt > 0) {
                    Log.i(TAG, "Google Sign-In retry attempt $attempt/$MAX_RETRIES")
                    delay(RETRY_DELAY_MS)
                }

                val response = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                Log.i(TAG, "Google Sign-In successful on attempt ${attempt + 1}")
                return extractIdToken(response)

            } catch (e: GetCredentialCancellationException) {
                // User canceled — don't retry
                Log.w(TAG, "Google Sign-In canceled by user")
                throw GoogleAuthException("Connexion Google annulee", e)

            } catch (e: NoCredentialException) {
                // Often happens on first attempt (cold start) — retry
                Log.w(TAG, "Google Sign-In NoCredentialException on attempt ${attempt + 1} — ${e.message}")
                lastException = e

            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed on attempt ${attempt + 1}: ${e.message}")
                lastException = e
            }
        }

        // All retries exhausted
        Log.e(TAG, "Google Sign-In failed after ${MAX_RETRIES + 1} attempts")
        throw GoogleAuthException(
            "Aucun compte Google disponible. Ajoutez un compte Google dans les parametres.",
            lastException
        )
    }

    /**
     * Extracts the Google ID token from the Credential Manager response.
     */
    private fun extractIdToken(response: GetCredentialResponse): String {
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)
                return googleIdTokenCredential.idToken
            } catch (e: GoogleIdTokenParsingException) {
                throw GoogleAuthException("Erreur lors du traitement de la reponse Google", e)
            }
        }

        throw GoogleAuthException("Type de credential inattendu: ${credential.type}")
    }
}

/**
 * Exception for Google authentication errors.
 */
class GoogleAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
