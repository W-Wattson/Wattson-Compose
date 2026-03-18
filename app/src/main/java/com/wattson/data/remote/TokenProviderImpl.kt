package com.wattson.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wattson.data.remote.dto.AuthResponse
import com.wattson.data.remote.dto.RefreshTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [TokenProvider] that reads/writes tokens
 * from EncryptedSharedPreferences and calls the refresh endpoint
 * via a separate, non-authenticated Retrofit instance.
 *
 * Uses the same SharedPreferences file ("wattson_auth_prefs") and keys
 * as AuthRepository to share token state.
 */
@Singleton
class TokenProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val baseUrl: String
) : TokenProvider {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.w("TokenProviderImpl", "Encrypted prefs failed, using regular", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Separate Retrofit instance WITHOUT AuthInterceptor/TokenAuthenticator
     * to avoid infinite loops during token refresh.
     */
    private val refreshApi: RefreshApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshApi::class.java)
    }

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        android.util.Log.d("TokenProviderImpl", "Tokens saved")
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_FULL_NAME)
            .remove(KEY_AUTH_PROVIDER)
            .remove(KEY_SUBSCRIPTION)
            .apply()
        _sessionExpired.tryEmit(Unit)
        android.util.Log.d("TokenProviderImpl", "Session cleared, sessionExpired emitted")
    }

    override suspend fun refreshTokenApi(request: RefreshTokenRequest): Response<AuthResponse> {
        return refreshApi.refreshToken(request)
    }

    companion object {
        private const val PREFS_NAME = "wattson_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_SUBSCRIPTION = "subscription"
    }
}

/**
 * Minimal Retrofit interface for the refresh endpoint only.
 * Used by [TokenProviderImpl] to avoid circular dependency.
 */
private interface RefreshApi {
    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>
}
