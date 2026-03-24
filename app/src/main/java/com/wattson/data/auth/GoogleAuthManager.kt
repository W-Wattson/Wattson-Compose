package com.wattson.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.wattson.BuildConfig
import com.wattson.R
import com.wattson.ui.i18n.UiText
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor() {

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val MAX_RETRIES = 1
        private const val RETRY_DELAY_MS = 500L
    }

    suspend fun getGoogleIdToken(activityContext: Context): String {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            throw GoogleAuthException(
                UiText.StringResource(R.string.google_sign_in_not_configured)
            )
        }

        val credentialManager = CredentialManager.create(activityContext)
        val googleSignInOption = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleSignInOption)
            .build()

        var lastException: Exception? = null

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
                Log.w(TAG, "Google Sign-In canceled by user")
                throw GoogleAuthException(
                    UiText.StringResource(R.string.google_sign_in_cancelled),
                    e
                )
            } catch (e: NoCredentialException) {
                Log.w(
                    TAG,
                    "Google Sign-In NoCredentialException on attempt ${attempt + 1} - ${e.message}"
                )
                lastException = e
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed on attempt ${attempt + 1}: ${e.message}")
                lastException = e
            }
        }

        Log.e(TAG, "Google Sign-In failed after ${MAX_RETRIES + 1} attempts")
        throw GoogleAuthException(
            UiText.StringResource(R.string.google_sign_in_no_credentials),
            lastException
        )
    }

    private fun extractIdToken(response: GetCredentialResponse): String {
        val credential = response.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return googleIdTokenCredential.idToken
            } catch (e: GoogleIdTokenParsingException) {
                throw GoogleAuthException(
                    UiText.StringResource(R.string.google_sign_in_response_error),
                    e
                )
            }
        }

        throw GoogleAuthException(
            UiText.StringResource(R.string.google_sign_in_unexpected_credential, credential.type)
        )
    }
}

class GoogleAuthException(
    val uiText: UiText,
    cause: Throwable? = null
) : Exception(null, cause)
