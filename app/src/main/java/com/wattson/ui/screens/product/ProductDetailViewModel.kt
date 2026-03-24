package com.wattson.ui.screens.product

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.domain.model.DurabilityScore
import com.wattson.domain.model.EnergyScore
import com.wattson.domain.model.GlobalScore
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.ProductMetrics
import com.wattson.domain.model.ProductScores
import com.wattson.domain.model.RepairabilityScore
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
import java.time.Instant
import javax.inject.Inject

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val metrics: ProductMetrics? = null,
    val globalScore: GlobalScore? = null,
    val userRating: Double? = null,
    val errorMessage: UiText? = null,
    val isFavorite: Boolean = false
)

sealed interface ProductDetailEvent {
    data object NavigateBack : ProductDetailEvent
    data object NavigateToRepair : ProductDetailEvent
    data class ShowError(val message: UiText) : ProductDetailEvent
    data object AddedToFavorites : ProductDetailEvent
    data object RemovedFromFavorites : ProductDetailEvent
}

sealed interface ProductDetailIntent {
    data object LoadProduct : ProductDetailIntent
    data object RefreshProduct : ProductDetailIntent
    data object ToggleFavorite : ProductDetailIntent
    data object StartRepair : ProductDetailIntent
    data object NavigateBack : ProductDetailIntent
    data object DismissError : ProductDetailIntent
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: com.wattson.data.repository.ProductRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
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
                android.util.Log.d("ProductDetailViewModel", "Loading product: $productId")

                val result = when {
                    productId.startsWith("scan:") -> {
                        val scanId = productId.removePrefix("scan:")
                        val userId = authRepository.getCurrentUserId()
                        productRepository.getProductFromScan(userId, scanId)
                    }

                    productId.startsWith("eprel:") -> {
                        val eprelPath = productId.removePrefix("eprel:")
                        val parts = eprelPath.split("/", limit = 2)
                        if (parts.size == 2) {
                            productRepository.getProductByEprelId(parts[0], parts[1])
                        } else {
                            Result.failure(Exception("invalid_eprel_identifier:$productId"))
                        }
                    }

                    productId.length in 8..13 && productId.all { it.isDigit() } -> {
                        productRepository.getProductByEan(productId)
                    }

                    else -> {
                        productRepository.getProductById(productId)
                    }
                }

                result.fold(
                    onSuccess = { product ->
                        android.util.Log.d("ProductDetailViewModel", "Product loaded: ${product.name}")
                        updateUiWithProduct(product)
                    },
                    onFailure = { error ->
                        android.util.Log.e("ProductDetailViewModel", "Failed to load product from catalog", error)
                        val snapshotProduct = loadFromScanHistory(productId)
                        if (snapshotProduct != null) {
                            android.util.Log.d(
                                "ProductDetailViewModel",
                                "Using scan snapshot: ${snapshotProduct.name}"
                            )
                            updateUiWithProduct(snapshotProduct)
                        } else {
                            updateUiWithProduct(createFallbackProduct(productId))
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailViewModel", "Exception loading product", e)
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

    private fun updateUiWithProduct(product: Product) {
        val metrics = createMetricsFromProduct(product)
        val globalScore = calculateGlobalScore(metrics)

        _uiState.update {
            it.copy(
                isLoading = false,
                product = product,
                metrics = metrics,
                globalScore = globalScore,
                userRating = null
            )
        }
    }

    private suspend fun loadFromScanHistory(ean: String): Product? {
        return try {
            val userId = authRepository.getCurrentUserId()
            val historyResult = productRepository.getScanHistory(userId, 0, 50)
            historyResult.getOrNull()
                ?.firstOrNull { it.gtin == ean }
                ?.let { scan ->
                    Product(
                        id = ean,
                        gtin = ean,
                        name = scan.snapshotData.productName,
                        brand = scan.snapshotData.brand,
                        model = scan.snapshotData.model,
                        category = scan.snapshotData.category,
                        commercialName = scan.snapshotData.commercialName,
                        energyLabel = scan.snapshotData.energyClass,
                        kwhPerYear = scan.snapshotData.kwhPerYear,
                        energyEfficiencyIndex = scan.snapshotData.energyEfficiencyIndex,
                        powerStandbyMode = scan.snapshotData.powerStandbyMode,
                        powerOffMode = scan.snapshotData.powerOffMode,
                        noiseDecibels = scan.snapshotData.noiseDecibels,
                        noiseClass = scan.snapshotData.noiseClass,
                        wetGripClass = scan.snapshotData.wetGripClass,
                        repairabilityIndex = scan.snapshotData.repairabilityIndex,
                        eprelProductGroup = scan.snapshotData.eprelProductGroup,
                        eprelDetails = scan.snapshotData.eprelDetails,
                        implementingAct = scan.snapshotData.implementingAct,
                        onMarketStartYear = scan.snapshotData.onMarketStartYear,
                        productFicheUrl = scan.snapshotData.productFicheUrl,
                        sourceName = scan.snapshotData.sourceName,
                        sourceUrl = scan.snapshotData.sourceUrl
                    )
                }
        } catch (e: Exception) {
            android.util.Log.w("ProductDetailViewModel", "Failed to load from scan history", e)
            null
        }
    }

    private fun createFallbackProduct(ean: String): Product {
        return Product(
            id = ean,
            gtin = ean,
            name = appContext.getString(R.string.product_scanned_name),
            brand = appContext.getString(R.string.product_unknown_brand),
            model = null,
            category = ProductCategory.OTHER,
            energyLabel = null
        )
    }

    private fun createMetricsFromProduct(product: Product): ProductMetrics {
        val totalFields = 10
        val filledFields = listOfNotNull(
            product.energyLabel,
            product.kwhPerYear,
            product.energyEfficiencyIndex,
            product.noiseDecibels,
            product.wetGripClass,
            product.repairabilityIndex,
            product.implementingAct,
            product.onMarketStartYear,
            product.sourceName,
            product.productFicheUrl
        ).size

        return ProductMetrics(
            version = 1,
            completeness = filledFields.toDouble() / totalFields,
            scores = ProductScores(
                energy = product.energyLabel?.let {
                    EnergyScore(value = it.name, kwhPerYear = product.kwhPerYear)
                },
                carbon = null,
                durability = product.characteristics?.enduranceHours?.let {
                    DurabilityScore(value = it / 10.0)
                },
                repairability = product.repairabilityIndex?.let {
                    val repClass = when {
                        it >= 8.0 -> "A"
                        it >= 6.0 -> "B"
                        it >= 4.0 -> "C"
                        it >= 2.0 -> "D"
                        else -> "E"
                    }
                    RepairabilityScore(value = it, repairabilityClass = repClass)
                }
            ),
            sources = emptyList(),
            fetchedAt = Instant.now()
        )
    }

    private fun refreshProduct() {
        loadProduct()
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val currentFavorite = _uiState.value.isFavorite
            _uiState.update { it.copy(isFavorite = !currentFavorite) }

            try {
                if (!currentFavorite) {
                    _events.emit(ProductDetailEvent.AddedToFavorites)
                } else {
                    _events.emit(ProductDetailEvent.RemovedFromFavorites)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFavorite = currentFavorite) }
                _events.emit(
                    ProductDetailEvent.ShowError(
                        e.toUiTextOr(UiText.StringResource(R.string.error_generic))
                    )
                )
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
        val scores = listOfNotNull(
            metrics.scores.durability?.value,
            metrics.scores.repairability?.value,
            metrics.scores.carbon?.value
        )

        val average = if (scores.isEmpty()) 0.0 else scores.average()

        val (letter, labelResId) = when {
            average >= 8.0 -> "A" to R.string.excellent
            average >= 6.5 -> "B" to R.string.product_score_very_good
            average >= 5.0 -> "C" to R.string.good
            average >= 3.5 -> "D" to R.string.fair
            average >= 2.0 -> "E" to R.string.poor
            else -> "F" to R.string.very_poor
        }

        return GlobalScore(
            numericValue = average * 10,
            letter = letter,
            label = appContext.getString(labelResId)
        )
    }
}
