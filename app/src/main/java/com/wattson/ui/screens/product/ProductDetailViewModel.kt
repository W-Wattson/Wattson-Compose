package com.wattson.ui.screens.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.CarbonScore
import com.wattson.domain.model.DurabilityScore
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.EnergyScore
import com.wattson.domain.model.GlobalScore
import com.wattson.domain.model.MetricSource
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.ProductCharacteristics
import com.wattson.domain.model.ProductMetrics
import com.wattson.domain.model.ProductScores
import com.wattson.domain.model.RepairabilityScore
import com.wattson.domain.model.ResistanceClass
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
 * UI State for the Product Detail screen.
 */
data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val metrics: ProductMetrics? = null,
    val globalScore: GlobalScore? = null,
    val userRating: Double? = null,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false
)

/**
 * One-shot events for product detail screen.
 */
sealed interface ProductDetailEvent {
    data object NavigateBack : ProductDetailEvent
    data object NavigateToRepair : ProductDetailEvent
    data class ShowError(val message: String) : ProductDetailEvent
    data object AddedToFavorites : ProductDetailEvent
    data object RemovedFromFavorites : ProductDetailEvent
}

/**
 * User intents for product detail screen.
 */
sealed interface ProductDetailIntent {
    data object LoadProduct : ProductDetailIntent
    data object RefreshProduct : ProductDetailIntent
    data object ToggleFavorite : ProductDetailIntent
    data object StartRepair : ProductDetailIntent
    data object NavigateBack : ProductDetailIntent
    data object DismissError : ProductDetailIntent
}

/**
 * ViewModel for the Product Detail screen.
 * Displays comprehensive product information and sustainability metrics.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject use cases when implemented
    // private val getProductByIdUseCase: GetProductByIdUseCase,
    // private val getProductMetricsUseCase: GetProductMetricsUseCase,
    // private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>("productId") ?: ""

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProductDetailEvent>()
    val events = _events.asSharedFlow()

    init {
        if (productId.isNotBlank()) {
            loadProduct()
        }
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: ProductDetailIntent) {
        when (intent) {
            is ProductDetailIntent.LoadProduct -> loadProduct()
            is ProductDetailIntent.RefreshProduct -> refreshProduct()
            is ProductDetailIntent.ToggleFavorite -> toggleFavorite()
            is ProductDetailIntent.StartRepair -> startRepair()
            is ProductDetailIntent.NavigateBack -> navigateBack()
            is ProductDetailIntent.DismissError -> dismissError()
        }
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // TODO: Replace with actual use cases
                // val product = getProductByIdUseCase(productId)
                // val metrics = getProductMetricsUseCase(product.gtin)
                
                kotlinx.coroutines.delay(500)
                
                // Mock data for development
                val (product, metrics) = getMockProductData()
                val globalScore = calculateGlobalScore(metrics)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = product,
                        metrics = metrics,
                        globalScore = globalScore,
                        userRating = 7.5
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

    private fun refreshProduct() {
        loadProduct()
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val currentFavorite = _uiState.value.isFavorite
            
            _uiState.update { it.copy(isFavorite = !currentFavorite) }
            
            try {
                // TODO: Replace with actual use case
                // toggleFavoriteUseCase(productId, !currentFavorite)
                
                if (!currentFavorite) {
                    _events.emit(ProductDetailEvent.AddedToFavorites)
                } else {
                    _events.emit(ProductDetailEvent.RemovedFromFavorites)
                }
                
            } catch (e: Exception) {
                // Revert on error
                _uiState.update { it.copy(isFavorite = currentFavorite) }
                _events.emit(ProductDetailEvent.ShowError(e.message ?: "Erreur"))
            }
        }
    }

    private fun startRepair() {
        viewModelScope.launch {
            _events.emit(ProductDetailEvent.NavigateToRepair)
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _events.emit(ProductDetailEvent.NavigateBack)
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun calculateGlobalScore(metrics: ProductMetrics): GlobalScore {
        // Calculate weighted average based on user preferences
        // For now, simple average
        val scores = listOfNotNull(
            metrics.scores.durability?.value,
            metrics.scores.repairability?.value,
            metrics.scores.carbon?.value
        )
        
        val average = if (scores.isEmpty()) 0.0 else scores.average()
        
        val (letter, label) = when {
            average >= 8.0 -> "A" to "Excellent"
            average >= 6.5 -> "B" to "Très bon"
            average >= 5.0 -> "C" to "Bon"
            average >= 3.5 -> "D" to "Moyen"
            average >= 2.0 -> "E" to "Passable"
            else -> "F" to "Médiocre"
        }
        
        return GlobalScore(numericValue = average * 10, letter = letter, label = label)
    }

    // Mock data for development
    private fun getMockProductData(): Pair<Product, ProductMetrics> {
        val product = Product(
            id = productId,
            gtin = "3760000000001",
            name = "iPhone 13 Pro",
            brand = "Apple",
            model = "A3517",
            category = ProductCategory.ELECTRONIQUE,
            energyLabel = EnergyClass.E,
            repairabilityIndex = 2.85,
            characteristics = ProductCharacteristics(
                enduranceHours = 40,
                dropResistanceClass = ResistanceClass.B
            ),
            imageUrl = null,
            updatedAt = Instant.now()
        )

        val metrics = ProductMetrics(
            version = 1,
            completeness = 0.85,
            scores = ProductScores(
                energy = EnergyScore(value = "E", kwhPerYear = 25),
                carbon = CarbonScore(value = 5.5, kgLifetime = 75.0),
                durability = DurabilityScore(value = 6.0),
                repairability = RepairabilityScore(value = 2.85, repairabilityClass = "D")
            ),
            sources = listOf(
                MetricSource(type = "ADEME", fetchedAt = Instant.now())
            ),
            fetchedAt = Instant.now()
        )

        return product to metrics
    }
}
