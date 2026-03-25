package com.wattson.ui.screens.premium

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.SubscriptionRepository
import com.wattson.domain.model.SubscriptionType
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionPlan(
    val type: SubscriptionType,
    val name: String,
    val price: String,
    val priceValue: Double,
    val stripePriceId: String = "",
    val features: List<String>,
    val isPopular: Boolean = false
)

data class PremiumUiState(
    val isLoading: Boolean = false,
    val currentSubscription: SubscriptionType = SubscriptionType.FREE,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val selectedPlan: SubscriptionPlan? = null,
    val isProcessingPayment: Boolean = false,
    val errorMessage: UiText? = null
)

sealed interface PremiumEvent {
    data object NavigateBack : PremiumEvent
    data object SubscriptionSuccess : PremiumEvent
    data class ShowError(val message: UiText) : PremiumEvent

    data class OpenPaymentSheet(
        val clientSecret: String,
        val ephemeralKey: String,
        val customerId: String,
        val publishableKey: String
    ) : PremiumEvent
}

sealed interface PremiumIntent {
    data object LoadPlans : PremiumIntent
    data class SelectPlan(val plan: SubscriptionPlan) : PremiumIntent
    data object ConfirmSubscription : PremiumIntent
    data object CancelSubscription : PremiumIntent
    data object NavigateBack : PremiumIntent
    data object DismissError : PremiumIntent
    data object PaymentCompleted : PremiumIntent
    data class PaymentFailed(val message: UiText) : PremiumIntent
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PremiumEvent>()
    val events = _events.asSharedFlow()

    init {
        loadPlans()
    }

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
                        selectedPlan = plans.find { plan -> plan.isPopular }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_loading_generic)
                        )
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
            _uiState.update { it.copy(isProcessingPayment = true, errorMessage = null) }

            try {
                val response = subscriptionRepository.createMobileSubscription(plan.stripePriceId)

                _uiState.update { it.copy(isProcessingPayment = false) }
                _events.emit(
                    PremiumEvent.OpenPaymentSheet(
                        clientSecret = response.clientSecret,
                        ephemeralKey = response.ephemeralKey,
                        customerId = response.customerId,
                        publishableKey = response.publishableKey
                    )
                )
            } catch (e: Exception) {
                val errorText = e.toUiTextOr(
                    UiText.StringResource(R.string.error_payment_generic)
                )
                _uiState.update {
                    it.copy(
                        isProcessingPayment = false,
                        errorMessage = errorText
                    )
                }
                _events.emit(PremiumEvent.ShowError(errorText))
            }
        }
    }

    private fun handlePaymentCompleted() {
        viewModelScope.launch {
            val plan = _uiState.value.selectedPlan ?: return@launch

            _uiState.update { it.copy(currentSubscription = plan.type) }

            try {
                authRepository.refreshProfile()
                android.util.Log.i("PremiumViewModel", "Profile refreshed after payment: plan=${plan.type}")
            } catch (e: Exception) {
                android.util.Log.w("PremiumViewModel", "Failed to refresh profile after payment", e)
            }

            _events.emit(PremiumEvent.SubscriptionSuccess)
        }
    }

    private fun handlePaymentFailed(message: UiText) {
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

    private fun getAvailablePlans(): List<SubscriptionPlan> = listOf(
        SubscriptionPlan(
            type = SubscriptionType.PREMIUM,
            name = appContext.getString(R.string.premium_plan_basic_name),
            price = appContext.getString(R.string.premium_plan_basic_price),
            priceValue = 5.99,
            stripePriceId = "price_1TCTn0HPNKboKHLrDPXNM547",
            features = listOf(
                appContext.getString(R.string.premium_plan_basic_feature_documents),
                appContext.getString(R.string.premium_plan_basic_feature_repair),
                appContext.getString(R.string.premium_plan_basic_feature_history),
                appContext.getString(R.string.premium_plan_basic_feature_support)
            ),
            isPopular = false
        ),
        SubscriptionPlan(
            type = SubscriptionType.PREMIUM_UNLIMITED,
            name = appContext.getString(R.string.premium_plan_unlimited_name),
            price = appContext.getString(R.string.premium_plan_unlimited_price),
            priceValue = 9.95,
            stripePriceId = "price_1TCTnyHPNKboKHLrBRvlqzZp",
            features = listOf(
                appContext.getString(R.string.premium_plan_unlimited_feature_documents),
                appContext.getString(R.string.premium_plan_unlimited_feature_repair),
                appContext.getString(R.string.premium_plan_unlimited_feature_history),
                appContext.getString(R.string.premium_plan_unlimited_feature_support),
                appContext.getString(R.string.premium_plan_unlimited_feature_alerts),
                appContext.getString(R.string.premium_plan_unlimited_feature_export)
            ),
            isPopular = true
        )
    )
}
