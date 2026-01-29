package com.wattson.ui.screens.repair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Repair case status.
 */
enum class RepairCaseStatus {
    OPEN,
    DIAGNOSING,
    WAITING_PARTS,
    IN_REPAIR,
    COMPLETED,
    CANCELLED
}

/**
 * Repair case data.
 */
data class RepairCase(
    val id: String,
    val product: Product,
    val status: RepairCaseStatus,
    val symptoms: List<String>,
    val diagnosis: String?,
    val recommendedAction: String?,
    val estimatedCost: Double?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Repair tip data.
 */
data class RepairTip(
    val id: String,
    val title: String,
    val description: String,
    val category: ProductCategory
)

/**
 * UI State for the Repair screen.
 */
data class RepairUiState(
    val isLoading: Boolean = false,
    val activeCases: List<RepairCase> = emptyList(),
    val completedCases: List<RepairCase> = emptyList(),
    val repairTips: List<RepairTip> = emptyList(),
    val selectedCase: RepairCase? = null,
    val errorMessage: String? = null,
    val showNewCaseDialog: Boolean = false
)

/**
 * One-shot events for repair screen.
 */
sealed interface RepairEvent {
    data class NavigateToCaseDetail(val caseId: String) : RepairEvent
    data object NavigateToScan : RepairEvent
    data class ShowError(val message: String) : RepairEvent
    data object CaseCreated : RepairEvent
}

/**
 * User intents for repair screen.
 */
sealed interface RepairIntent {
    data object LoadRepairData : RepairIntent
    data object RefreshRepairData : RepairIntent
    data class OpenCase(val caseId: String) : RepairIntent
    data object StartNewCase : RepairIntent
    data object DismissNewCaseDialog : RepairIntent
    data object NavigateToScan : RepairIntent
    data object DismissError : RepairIntent
}

/**
 * ViewModel for the Repair screen.
 * Manages repair cases and provides repair assistance.
 */
@HiltViewModel
class RepairViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val getRepairCasesUseCase: GetRepairCasesUseCase,
    // private val getRepairTipsUseCase: GetRepairTipsUseCase,
    // private val createRepairCaseUseCase: CreateRepairCaseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepairUiState())
    val uiState: StateFlow<RepairUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RepairEvent>()
    val events = _events.asSharedFlow()

    init {
        loadRepairData()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: RepairIntent) {
        when (intent) {
            is RepairIntent.LoadRepairData -> loadRepairData()
            is RepairIntent.RefreshRepairData -> refreshRepairData()
            is RepairIntent.OpenCase -> openCase(intent.caseId)
            is RepairIntent.StartNewCase -> startNewCase()
            is RepairIntent.DismissNewCaseDialog -> dismissNewCaseDialog()
            is RepairIntent.NavigateToScan -> navigateToScan()
            is RepairIntent.DismissError -> dismissError()
        }
    }

    private fun loadRepairData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // TODO: Replace with actual use cases
                kotlinx.coroutines.delay(500)
                
                val tips = getMockRepairTips()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeCases = emptyList(), // User has no active cases
                        completedCases = emptyList(),
                        repairTips = tips
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

    private fun refreshRepairData() {
        loadRepairData()
    }

    private fun openCase(caseId: String) {
        viewModelScope.launch {
            _events.emit(RepairEvent.NavigateToCaseDetail(caseId))
        }
    }

    private fun startNewCase() {
        _uiState.update { it.copy(showNewCaseDialog = true) }
    }

    private fun dismissNewCaseDialog() {
        _uiState.update { it.copy(showNewCaseDialog = false) }
    }

    private fun navigateToScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(showNewCaseDialog = false) }
            _events.emit(RepairEvent.NavigateToScan)
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Mock data for development
    private fun getMockRepairTips(): List<RepairTip> = listOf(
        RepairTip(
            id = "tip_1",
            title = "Nettoyez régulièrement vos filtres",
            description = "Pour les appareils électroménagers, un nettoyage mensuel des filtres prolonge leur durée de vie.",
            category = ProductCategory.ELECTROMENAGER
        ),
        RepairTip(
            id = "tip_2",
            title = "Évitez les charges à 100%",
            description = "Pour préserver la batterie de vos appareils électroniques, évitez de les charger au-delà de 80%.",
            category = ProductCategory.ELECTRONIQUE
        ),
        RepairTip(
            id = "tip_3",
            title = "Mises à jour logicielles",
            description = "Gardez vos appareils à jour pour bénéficier des correctifs de performance et de sécurité.",
            category = ProductCategory.ELECTRONIQUE
        ),
        RepairTip(
            id = "tip_4",
            title = "Débranchez en cas d'orage",
            description = "Protégez vos appareils des surtensions en les débranchant pendant les orages.",
            category = ProductCategory.ELECTROMENAGER
        )
    )
}
