package com.wattson.domain.model

import java.time.Instant

/**
 * Represents a Wattson user account with subscription and preference data.
 */
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val authProvider: AuthProvider,
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    val preferences: UserPreferences = UserPreferences(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Authentication provider options
 * Supports email/password and OAuth providers
 */
enum class AuthProvider {
    EMAIL,
    GOOGLE,
    APPLE
}

/**
 * User subscription tiers and their document limits.
 * - FREE: 5 documents max
 * - PREMIUM: 15 documents max (5.99€/month)
 * - PREMIUM_UNLIMITED: Unlimited documents (24.99€/month)
 * - ENTERPRISE: Custom limits (quote-based)
 */
enum class SubscriptionType {
    FREE,
    PREMIUM,
    PREMIUM_UNLIMITED,
    ENTERPRISE
}

/**
 * User preferences that impact the Wattson Score algorithm
 * Order matters: first choice has highest weight
 */
data class UserPreferences(
    val orderedPreferences: List<PreferenceType> = listOf(
        PreferenceType.ECONOMY,
        PreferenceType.ECOLOGY,
        PreferenceType.REPAIRABILITY
    )
)

/**
 * Available preference types for score weighting
 */
enum class PreferenceType {
    ECOLOGY,
    ECONOMY,
    REPAIRABILITY
}

/**
 * Document limit based on subscription type
 */
fun SubscriptionType.getDocumentLimit(): Int? {
    return when (this) {
        SubscriptionType.FREE -> 5
        SubscriptionType.PREMIUM -> 15
        SubscriptionType.PREMIUM_UNLIMITED -> null // Unlimited
        SubscriptionType.ENTERPRISE -> null // Custom
    }
}

/**
 * Monthly price based on subscription type (in euros)
 */
fun SubscriptionType.getMonthlyPrice(): Double? {
    return when (this) {
        SubscriptionType.FREE -> 0.0
        SubscriptionType.PREMIUM -> 5.99
        SubscriptionType.PREMIUM_UNLIMITED -> 24.99
        SubscriptionType.ENTERPRISE -> null // Custom pricing
    }
}
