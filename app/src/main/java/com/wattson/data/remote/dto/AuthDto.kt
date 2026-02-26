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
