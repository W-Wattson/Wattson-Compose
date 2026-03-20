package com.wattson.ui.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.SubscriptionRepository
import com.wattson.domain.model.SubscriptionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Subscription plan data displayed in the Premium screen.
 *
 * @property type          the [SubscriptionType] enum value.
 * @property name          display name (e.g. "Premium").
 * @property price         formatted price string (e.g. "5,99 EUR/mois").
 * @property priceValue    numeric price for sorting/comparison.
 * @property stripePriceId the Stripe Price ID used for checkout.
 * @property features      list of feature descriptions for the plan card.
 * @property isPopular     whether to show the "Populaire" badge.
 */
data class SubscriptionPlan(
    val type: SubscriptionType,
    val name: String,
    val price: String,
    val priceValue: Double,
    val stripePriceId: String = "",
    val features: List<String>,
    val isPopular: Boolean = false
)

/**
 * UI State for the Premium screen.
 */
data class PremiumUiState(
    val isLoading: Boolean = false,
    val currentSubscription: SubscriptionType = SubscriptionType.FREE,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val selectedPlan: SubscriptionPlan? = null,
    val isProcessingPayment: Boolean = false,
    val errorMessage: String? = null
)

/**
 * One-shot events emitted by the ViewModel.
 */
sealed interface PremiumEvent {
    data object NavigateBack : PremiumEvent
    data object SubscriptionSuccess : PremiumEvent
    data class ShowError(val message: String) : PremiumEvent

    /**
     * Emitted when the backend returns PaymentSheet data.
     * The Screen composable should present the Stripe PaymentSheet.
     */
    data class OpenPaymentSheet(
        val clientSecret: String,
        val ephemeralKey: String,
        val customerId: String,
        val publishableKey: String
    ) : PremiumEvent
}

/**
 * User intents for the Premium screen.
 */
sealed interface PremiumIntent {
    data object LoadPlans : PremiumIntent
    data class SelectPlan(val plan: SubscriptionPlan) : PremiumIntent
    data object ConfirmSubscription : PremiumIntent
    data object CancelSubscription : PremiumIntent
    data object NavigateBack : PremiumIntent
    data object DismissError : PremiumIntent

    /** Called from the Screen after PaymentSheet completes successfully. */
    data object PaymentCompleted : PremiumIntent

    /** Called from the Screen after PaymentSheet fails or is canceled. */
    data class PaymentFailed(val message: String) : PremiumIntent
}

/**
 * ViewModel for the Premium screen.
 *
 * Manages subscription plans and the Stripe PaymentSheet flow:
 * 1. User selects a plan and taps "Subscribe"
 * 2. ViewModel calls backend [POST /mobile/subscribe] to get PaymentSheet data
 * 3. ViewModel emits [PremiumEvent.OpenPaymentSheet]
 * 4. Screen presents the Stripe PaymentSheet
 * 5. Screen calls [PremiumIntent.PaymentCompleted] or [PremiumIntent.PaymentFailed]
 * 6. Webhook on the backend activates the subscription asynchronously
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PremiumEvent>()
    val events = _events.asSharedFlow()

    init {
        loadPlans()
    }

    /**
     * Dispatches user intents to the appropriate handler.
     */
    fun onIntent(intent: PremiumIntent) {
        when (intent) {
            is PremiumIntent.LoadPlans -> loadPlans()
            is PremiumIntent.SelectPlan -> selectPlan(intent.plan)
            is PremiumIntent.ConfirmSubscription -> confirmSubscription()
            is PremiumIntent.CancelSubscription -> cancelSubscription()
            is PremiumIntent.NavigateBack -> navigateBack()
            is PremiumIntent.DismissError -> dismissError()
            is PremiumIntent.PaymentCompleted -> handlePaymentCompleted()
            is PremiumIntent.PaymentFailed -> handlePaymentFailed(intent.message)
        }
    }

    /**
     * Loads available subscription plans and the user's current subscription.
     */
    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUser = authRepository.currentUser.value
                val currentSub = currentUser?.subscriptionType ?: SubscriptionType.FREE
                val plans = getAvailablePlans()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSubscription = currentSub,
                        availablePlans = plans,
                        selectedPlan = plans.find { p -> p.isPopular }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur lors du chargement"
                    )
                }
            }
        }
    }

    /**
     * Updates the selected plan in the UI state.
     */
    private fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    /**
     * Initiates the Stripe PaymentSheet flow:
     * 1. Calls the backend to create a subscription with incomplete payment
     * 2. Emits [PremiumEvent.OpenPaymentSheet] with the PaymentSheet configuration data
     */
    private fun confirmSubscription() {
        val plan = _uiState.value.selectedPlan ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = true, errorMessage = null) }

            try {
                val response = subscriptionRepository.createMobileSubscription(plan.stripePriceId)

                _uiState.update { it.copy(isProcessingPayment = false) }

                // Emit event for the Screen to present PaymentSheet
                _events.emit(
                    PremiumEvent.OpenPaymentSheet(
                        clientSecret = response.clientSecret,
                        ephemeralKey = response.ephemeralKey,
                        customerId = response.customerId,
                        publishableKey = response.publishableKey
                    )
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingPayment = false,
                        errorMessage = e.message ?: "Erreur de paiement"
                    )
                }
                _events.emit(PremiumEvent.ShowError(e.message ?: "Erreur de paiement"))
            }
        }
    }

    /**
     * Called when the Stripe PaymentSheet completes successfully.
     * The actual subscription activation is done server-side via the Stripe webhook.
     */
    private fun handlePaymentCompleted() {
        viewModelScope.launch {
            val plan = _uiState.value.selectedPlan ?: return@launch

            _uiState.update {
                it.copy(currentSubscription = plan.type)
            }

            // Refresh user profile from backend to get updated subscription data
            try {
                authRepository.refreshProfile()
                android.util.Log.i("PremiumViewModel", "Profile refreshed after payment: plan=${plan.type}")
            } catch (e: Exception) {
                android.util.Log.w("PremiumViewModel", "Failed to refresh profile after payment", e)
            }

            _events.emit(PremiumEvent.SubscriptionSuccess)
        }
    }

    /**
     * Called when the Stripe PaymentSheet fails or is canceled by the user.
     */
    private fun handlePaymentFailed(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = false) }
            _events.emit(PremiumEvent.ShowError(message))
        }
    }

    private fun cancelSubscription() {
        viewModelScope.launch {
            _events.emit(PremiumEvent.NavigateBack)
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _events.emit(PremiumEvent.NavigateBack)
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Returns the available premium plans with Stripe Price IDs.
     * Price IDs are configured for Stripe test mode.
     */
    private fun getAvailablePlans(): List<SubscriptionPlan> = listOf(
        SubscriptionPlan(
            type = SubscriptionType.PREMIUM,
            name = "Premium",
            price = "5,99 \u20AC/mois",
            priceValue = 5.99,
            stripePriceId = "price_1TCTn0HPNKboKHLrDPXNM547",
            features = listOf(
                "Conciergerie 15 documents",
                "Conseil de r\u00E9parabilit\u00E9 basique",
                "Historique illimit\u00E9",
                "Support prioritaire"
            ),
            isPopular = false
        ),
        SubscriptionPlan(
            type = SubscriptionType.PREMIUM_UNLIMITED,
            name = "Premium Illimit\u00E9",
            price = "9,95 \u20AC/mois",
            priceValue = 9.95,
            stripePriceId = "price_1TCTnyHPNKboKHLrBRvlqzZp",
            features = listOf(
                "Conciergerie 1000 documents",
                "Conseil de r\u00E9parabilit\u00E9 avanc\u00E9",
                "Historique illimit\u00E9",
                "Support prioritaire",
                "Alertes de garantie",
                "Export des donn\u00E9es"
            ),
            isPopular = true
        )
    )
}
