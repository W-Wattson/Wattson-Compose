package com.wattson.data.repository

import com.wattson.data.remote.api.WattsonApi
import com.wattson.data.remote.dto.MobileSubscribeRequest
import com.wattson.data.remote.dto.MobileSubscriptionResponse
import org.json.JSONObject
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
     * @throws SubscriptionException if the API call fails.
     */
    suspend fun createMobileSubscription(priceId: String): MobileSubscriptionResponse {
        val token = authRepository.getAccessToken()
            ?: throw SubscriptionException("Non authentifié. Veuillez vous reconnecter.")

        try {
            val response = api.createMobileSubscription(
                token = "Bearer $token",
                request = MobileSubscribeRequest(priceId = priceId)
            )

            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }

            // Parse error message from response
            val errorMessage = try {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    JSONObject(errorBody).optString("message", "Erreur lors de la création de l'abonnement")
                } else {
                    "Erreur lors de la création de l'abonnement (code: ${response.code()})"
                }
            } catch (e: Exception) {
                "Erreur lors de la création de l'abonnement (code: ${response.code()})"
            }

            throw SubscriptionException(errorMessage)

        } catch (e: SubscriptionException) {
            throw e
        } catch (e: Exception) {
            throw SubscriptionException("Erreur réseau: ${e.message}", e)
        }
    }

    /**
     * Exception thrown when a subscription operation fails.
     */
    class SubscriptionException(
        message: String,
        cause: Throwable? = null
    ) : RuntimeException(message, cause)
}
