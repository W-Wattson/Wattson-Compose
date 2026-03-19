package com.wattson.data.remote.dto

/**
 * Request body for creating a mobile subscription via Stripe PaymentSheet.
 *
 * @property priceId the Stripe Price ID for the chosen plan (e.g. "price_xxx").
 */
data class MobileSubscribeRequest(
    val priceId: String
)

/**
 * Response from the backend containing all data needed to initialize
 * the Stripe PaymentSheet on the mobile client.
 *
 * @property clientSecret   PaymentIntent client secret for PaymentSheet.
 * @property ephemeralKey    ephemeral key for customer authentication.
 * @property customerId     Stripe customer ID.
 * @property publishableKey Stripe publishable key (pk_test_... or pk_live_...).
 * @property subscriptionId Stripe subscription ID for tracking.
 */
data class MobileSubscriptionResponse(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String,
    val subscriptionId: String
)
