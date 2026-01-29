package com.wattson.ui.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Subscription plan data.
 */
data class SubscriptionPlan(
    val type: SubscriptionType,
    val name: String,
    val price: String,
    val priceValue: Double,
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
 * One-shot events for premium screen.
 */
sealed interface PremiumEvent {
    data object NavigateBack : PremiumEvent
    data object SubscriptionSuccess : PremiumEvent
    data class ShowError(val message: String) : PremiumEvent
    data object OpenPaymentSheet : PremiumEvent
}

/**
 * User intents for premium screen.
 */
sealed interface PremiumIntent {
    data object LoadPlans : PremiumIntent
    data class SelectPlan(val plan: SubscriptionPlan) : PremiumIntent
    data object ConfirmSubscription : PremiumIntent
    data object CancelSubscription : PremiumIntent
    data object NavigateBack : PremiumIntent
    data object DismissError : PremiumIntent
}

/**
 * ViewModel for the Premium screen.
 * Manages subscription plans and payment flow.
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val getCurrentSubscriptionUseCase: GetCurrentSubscriptionUseCase,
    // private val getAvailablePlansUseCase: GetAvailablePlansUseCase,
    // private val subscribeUseCase: SubscribeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PremiumEvent>()
    val events = _events.asSharedFlow()

    init {
        loadPlans()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: PremiumIntent) {
        when (intent) {
            is PremiumIntent.LoadPlans -> loadPlans()
            is PremiumIntent.SelectPlan -> selectPlan(intent.plan)
            is PremiumIntent.ConfirmSubscription -> confirmSubscription()
            is PremiumIntent.CancelSubscription -> cancelSubscription()
            is PremiumIntent.NavigateBack -> navigateBack()
            is PremiumIntent.DismissError -> dismissError()
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // TODO: Replace with actual use cases
                // val currentSub = getCurrentSubscriptionUseCase()
                // val plans = getAvailablePlansUseCase()
                
                kotlinx.coroutines.delay(300)
                
                val plans = getAvailablePlans()
                val currentSub = SubscriptionType.FREE

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

    private fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    private fun confirmSubscription() {
        val plan = _uiState.value.selectedPlan ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = true) }

            try {
                // TODO: Replace with actual payment flow
                // subscribeUseCase(plan.type)
                
                // Simulate payment processing
                kotlinx.coroutines.delay(2000)
                
                _uiState.update {
                    it.copy(
                        isProcessingPayment = false,
                        currentSubscription = plan.type
                    )
                }
                
                _events.emit(PremiumEvent.SubscriptionSuccess)
                
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

    private fun cancelSubscription() {
        viewModelScope.launch {
            // TODO: Implement subscription cancellation
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

    private fun getAvailablePlans(): List<SubscriptionPlan> = listOf(
        SubscriptionPlan(
            type = SubscriptionType.PREMIUM,
            name = "Premium",
            price = "5,99 €/mois",
            priceValue = 5.99,
            features = listOf(
                "Conciergerie 15 documents",
                "Conseil de réparabilité basique",
                "Historique illimité",
                "Support prioritaire"
            ),
            isPopular = false
        ),
        SubscriptionPlan(
            type = SubscriptionType.PREMIUM_UNLIMITED,
            name = "Premium Illimité",
            price = "9,95 €/mois",
            priceValue = 9.95,
            features = listOf(
                "Conciergerie 1000 documents",
                "Conseil de réparabilité avancé",
                "Historique illimité",
                "Support prioritaire",
                "Alertes de garantie",
                "Export des données"
            ),
            isPopular = true
        )
    )
}
