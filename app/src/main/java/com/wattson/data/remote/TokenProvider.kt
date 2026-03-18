package com.wattson.data.remote

import com.wattson.data.remote.dto.AuthResponse
import com.wattson.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.SharedFlow
import retrofit2.Response

/**
 * Interface for token operations.
 * Breaks the circular dependency between TokenAuthenticator and AuthRepository
 * by providing direct access to tokens without depending on Retrofit/OkHttp.
 */
interface TokenProvider {

    /** Returns the stored access token, or null if not authenticated. */
    fun getAccessToken(): String?

    /** Returns the stored refresh token, or null if not available. */
    fun getRefreshToken(): String?

    /** Saves new access and refresh tokens to secure storage. */
    fun saveTokens(accessToken: String, refreshToken: String)

    /**
     * Clears all session data (tokens + user info).
     * Emits to [sessionExpired] to notify observers.
     */
    fun clearSession()

    /**
     * Calls the refresh token endpoint using a private Retrofit instance
     * (without auth interceptor, to avoid infinite loops).
     */
    suspend fun refreshTokenApi(request: RefreshTokenRequest): Response<AuthResponse>

    /**
     * Flow that emits when the session has expired (refresh failed).
     * AuthRepository observes this to reset its StateFlows.
     */
    val sessionExpired: SharedFlow<Unit>
}
