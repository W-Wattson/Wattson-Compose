package com.wattson.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request for user login.
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

/**
 * Request for user registration.
 */
data class RegisterRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("fullName")
    val fullName: String? = null
)

/**
 * Request for token refresh.
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Authentication response from backend.
 */
data class AuthResponse(
    @SerializedName("user")
    val user: UserDto,
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("expiresIn")
    val expiresIn: Long
)

/**
 * User DTO from authentication response.
 */
data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("authProvider")
    val authProvider: String,
    @SerializedName("subscriptionType")
    val subscriptionType: String
)

/**
 * Error response from authentication.
 */
data class AuthErrorResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String
)

/**
 * Request for Google OAuth authentication.
 */
data class GoogleAuthRequest(
    @SerializedName("idToken")
    val idToken: String
)

/**
 * Request for forgot password.
 */
data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

/**
 * Request for password reset.
 */
data class ResetPasswordRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("newPassword")
    val newPassword: String
)

/**
 * Generic message response from backend.
 * Used by forgot-password and reset-password endpoints.
 */
data class MessageResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

/**
 * Response from delete account endpoint (RGPD).
 */
data class DeleteAccountResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("userId")
    val userId: String?,
    @SerializedName("deletionType")
    val deletionType: String?,
    @SerializedName("message")
    val message: String
)
