package com.wattson.data.remote

import com.wattson.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * OkHttp Authenticator that handles 401 responses by refreshing
 * the access token using the stored refresh token.
 *
 * Uses synchronized refresh to avoid race conditions when multiple
 * requests receive 401 simultaneously.
 *
 * Note: [runBlocking] is safe here because OkHttp's Authenticator
 * runs on OkHttp's thread pool, never on the main thread.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider
) : Authenticator {

    private val lock = Object()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite retry loops: max 2 attempts
        if (responseCount(response) >= 2) {
            android.util.Log.w("TokenAuthenticator", "Max retry reached, clearing session")
            tokenProvider.clearSession()
            return null
        }

        synchronized(lock) {
            val currentToken = tokenProvider.getAccessToken()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")

            // If another thread already refreshed the token, retry with the new one
            if (currentToken != null && currentToken != requestToken) {
                android.util.Log.d("TokenAuthenticator", "Token already refreshed by another thread")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Attempt token refresh
            val refreshToken = tokenProvider.getRefreshToken()
            if (refreshToken == null) {
                android.util.Log.w("TokenAuthenticator", "No refresh token available, clearing session")
                tokenProvider.clearSession()
                return null
            }

            val newAccessToken = runBlocking {
                try {
                    val refreshResponse = tokenProvider.refreshTokenApi(
                        RefreshTokenRequest(refreshToken)
                    )
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val authResponse = refreshResponse.body()!!
                        tokenProvider.saveTokens(
                            authResponse.accessToken,
                            authResponse.refreshToken
                        )
                        android.util.Log.d("TokenAuthenticator", "Token refreshed successfully")
                        authResponse.accessToken
                    } else {
                        android.util.Log.w("TokenAuthenticator", "Refresh failed: ${refreshResponse.code()}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TokenAuthenticator", "Refresh error", e)
                    null
                }
            }

            return if (newAccessToken != null) {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                tokenProvider.clearSession()
                null
            }
        }
    }

    /** Counts how many prior responses exist in the chain (for retry limit). */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
