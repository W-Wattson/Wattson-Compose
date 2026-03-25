package com.wattson.data.repository

import com.wattson.R
import com.wattson.data.remote.api.WattsonApi
import com.wattson.data.remote.dto.MobileSubscribeRequest
import com.wattson.data.remote.dto.MobileSubscriptionResponse
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.UserFacingException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Stripe subscription operations.
 *
 * Calls the backend to create a mobile subscription and returns
 * the data needed for the Stripe PaymentSheet.
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: WattsonApi,
    private val authRepository: AuthRepository
) {

    /**
     * Creates a mobile subscription via the backend.
     * The backend creates a Stripe Subscription with incomplete payment,
     * an EphemeralKey, and returns all data needed for PaymentSheet.
     *
     * @param priceId the Stripe Price ID for the chosen plan.
     * @return [MobileSubscriptionResponse] with clientSecret, ephemeralKey, etc.
     * @throws UserFacingException if the API call fails.
     */
    suspend fun createMobileSubscription(priceId: String): MobileSubscriptionResponse {
        val token = authRepository.getAccessToken()
            ?: throw paymentException()

        try {
            val response = api.createMobileSubscription(
                token = "Bearer $token",
                request = MobileSubscribeRequest(priceId = priceId)
            )

            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }

            throw paymentException()

        } catch (e: UserFacingException) {
            throw e
        } catch (e: Exception) {
            throw paymentException(e)
        }
    }

    private fun paymentException(cause: Throwable? = null): UserFacingException {
        return UserFacingException(
            UiText.StringResource(R.string.error_payment_generic),
            cause
        )
    }
}
