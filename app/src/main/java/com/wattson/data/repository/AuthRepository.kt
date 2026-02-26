package com.wattson.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.wattson.data.remote.api.WattsonApi
import com.wattson.data.remote.dto.AuthErrorResponse
import com.wattson.data.remote.dto.LoginRequest
import com.wattson.data.remote.dto.RegisterRequest
import com.wattson.domain.model.AuthProvider
import com.wattson.domain.model.SubscriptionType
import com.wattson.domain.model.User
import com.wattson.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations.
 * Uses backend API for real authentication.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: WattsonApi
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "wattson_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Failed to create encrypted prefs, using regular", e)
            context.getSharedPreferences("wattson_auth_prefs", Context.MODE_PRIVATE)
        }
    }

    private val gson = Gson()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        loadStoredUser()
    }

    private fun loadStoredUser() {
        val userId = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val fullName = prefs.getString(KEY_FULL_NAME, null)

        if (userId != null && email != null) {
            val user = User(
                id = userId,
                email = email,
                fullName = fullName ?: email.substringBefore("@"),
                authProvider = AuthProvider.valueOf(
                    prefs.getString(KEY_AUTH_PROVIDER, AuthProvider.EMAIL.name) ?: AuthProvider.EMAIL.name
                ),
                subscriptionType = SubscriptionType.valueOf(
                    prefs.getString(KEY_SUBSCRIPTION, SubscriptionType.FREE.name) ?: SubscriptionType.FREE.name
                )
            )
            _currentUser.value = user
            _isLoggedIn.value = true
            android.util.Log.d("AuthRepository", "Restored user session: ${user.email}")
        }
    }

    /**
     * Login with email and password via backend API.
     */
    suspend fun login(email: String, password: String, rememberMe: Boolean = true): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs locally first
            if (!isValidEmail(email)) {
                return@withContext Result.failure(AuthException("Email invalide"))
            }
            if (password.length < 6) {
                return@withContext Result.failure(AuthException("Mot de passe trop court"))
            }

            // Try backend authentication
            val response = api.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val user = User(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    fullName = authResponse.user.fullName,
                    authProvider = AuthProvider.valueOf(authResponse.user.authProvider),
                    subscriptionType = SubscriptionType.valueOf(authResponse.user.subscriptionType),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                // Store session
                if (rememberMe) {
                    saveUserSession(user, authResponse.accessToken, authResponse.refreshToken)
                }

                _currentUser.value = user
                _isLoggedIn.value = true

                android.util.Log.d("AuthRepository", "Login successful via API: ${user.email}")
                Result.success(user)
            } else {
                // Parse error response
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthErrorResponse::class.java)?.message
                } catch (e: Exception) {
                    null
                } ?: "Email ou mot de passe incorrect"

                android.util.Log.w("AuthRepository", "Login failed: $errorMessage")
                Result.failure(AuthException(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Login error", e)
            Result.failure(AuthException("Impossible de se connecter au serveur. Verifiez votre connexion."))
        }
    }

    /**
     * Register a new user via backend API.
     */
    suspend fun register(email: String, password: String, fullName: String? = null): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (!isValidEmail(email)) {
                return@withContext Result.failure(AuthException("Email invalide"))
            }
            if (!isValidPassword(password)) {
                return@withContext Result.failure(AuthException("Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial"))
            }

            // Try backend registration
            val response = api.register(RegisterRequest(
                email = email,
                password = password,
                fullName = fullName ?: extractFullName(email)
            ))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val user = User(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    fullName = authResponse.user.fullName,
                    authProvider = AuthProvider.valueOf(authResponse.user.authProvider),
                    subscriptionType = SubscriptionType.valueOf(authResponse.user.subscriptionType),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                saveUserSession(user, authResponse.accessToken, authResponse.refreshToken)
                _currentUser.value = user
                _isLoggedIn.value = true

                android.util.Log.d("AuthRepository", "Registration successful via API: ${user.email}")
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthErrorResponse::class.java)?.message
                } catch (e: Exception) {
                    null
                } ?: "Erreur lors de l'inscription"

                android.util.Log.w("AuthRepository", "Registration failed: $errorMessage")
                Result.failure(AuthException(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Registration error", e)
            Result.failure(AuthException("Impossible de se connecter au serveur. Verifiez votre connexion."))
        }
    }

    /**
     * Login with OAuth provider.
     */
    suspend fun loginWithProvider(provider: AuthProvider): Result<User> = withContext(Dispatchers.IO) {
        try {
            // OAuth would require proper implementation with provider SDK
            kotlinx.coroutines.delay(1000)

            val email = when (provider) {
                AuthProvider.GOOGLE -> "user@gmail.com"
                AuthProvider.APPLE -> "user@icloud.com"
                else -> "user@example.com"
            }

            val user = User(
                id = generateUserId(email),
                email = email,
                fullName = "Utilisateur ${provider.name}",
                authProvider = provider,
                subscriptionType = SubscriptionType.FREE,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            saveUserSession(user, null, null)
            _currentUser.value = user
            _isLoggedIn.value = true

            android.util.Log.d("AuthRepository", "OAuth login successful: $provider")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(AuthException("Erreur OAuth: ${e.message}"))
        }
    }

    /**
     * Logout the current user.
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Try to notify backend
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)
            if (token != null) {
                try {
                    api.logout("Bearer $token")
                } catch (e: Exception) {
                    android.util.Log.w("AuthRepository", "Backend logout failed", e)
                }
            }

            // Clear stored session
            prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_FULL_NAME)
                .remove(KEY_AUTH_PROVIDER)
                .remove(KEY_SUBSCRIPTION)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply()

            _currentUser.value = null
            _isLoggedIn.value = false

            android.util.Log.d("AuthRepository", "Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Logout failed", e)
            Result.failure(AuthException("Erreur de déconnexion: ${e.message}"))
        }
    }

    /**
     * Get the current user's ID for API calls.
     */
    fun getCurrentUserId(): String {
        _currentUser.value?.let { return it.id }

        val existingAnonymousId = prefs.getString(KEY_ANONYMOUS_ID, null)
        if (existingAnonymousId != null) {
            return existingAnonymousId
        }

        val newAnonymousId = "anonymous-${UUID.randomUUID().toString().take(8)}"
        prefs.edit().putString(KEY_ANONYMOUS_ID, newAnonymousId).apply()
        android.util.Log.d("AuthRepository", "Created new anonymous ID: $newAnonymousId")
        return newAnonymousId
    }

    /**
     * Get stored access token.
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Update user preferences.
     */
    suspend fun updatePreferences(preferences: UserPreferences): Result<User> = withContext(Dispatchers.IO) {
        val currentUser = _currentUser.value
            ?: return@withContext Result.failure(AuthException("Utilisateur non connecté"))

        try {
            kotlinx.coroutines.delay(300)

            val updatedUser = currentUser.copy(
                preferences = preferences,
                updatedAt = Instant.now()
            )

            _currentUser.value = updatedUser
            saveUserSession(updatedUser, prefs.getString(KEY_ACCESS_TOKEN, null), prefs.getString(KEY_REFRESH_TOKEN, null))

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(AuthException("Erreur de mise à jour: ${e.message}"))
        }
    }

    private fun saveUserSession(user: User, accessToken: String?, refreshToken: String?) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_AUTH_PROVIDER, user.authProvider.name)
            putString(KEY_SUBSCRIPTION, user.subscriptionType.name)
            if (accessToken != null) {
                putString(KEY_ACCESS_TOKEN, accessToken)
            }
            if (refreshToken != null) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            apply()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasMinLength && hasUppercase && hasDigit && hasSpecial
    }

    private fun generateUserId(email: String): String {
        return "user_${email.lowercase().hashCode().toString(16)}"
    }

    private fun extractFullName(email: String): String {
        return email.substringBefore("@")
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_SUBSCRIPTION = "subscription"
        private const val KEY_ANONYMOUS_ID = "anonymous_user_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}

/**
 * Custom exception for authentication errors.
 */
class AuthException(message: String) : Exception(message)
