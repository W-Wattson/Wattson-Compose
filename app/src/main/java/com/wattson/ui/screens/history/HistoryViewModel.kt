package com.wattson.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.HistoryEntry
import com.wattson.domain.model.HistoryFilter
import com.wattson.domain.model.HistorySortOrder
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.Scan
import com.wattson.domain.model.ScanSnapshot
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
 * UI State for the History screen.
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val historyEntries: List<HistoryEntry> = emptyList(),
    val filteredEntries: List<HistoryEntry> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    val sortOrder: HistorySortOrder = HistorySortOrder.DATE_DESC,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)

/**
 * One-shot events for history screen.
 */
sealed interface HistoryEvent {
    data class NavigateToProductDetail(val productId: String) : HistoryEvent
    data object NavigateToScan : HistoryEvent
    data class ShowError(val message: String) : HistoryEvent
    data class ShowSnackbar(val message: String) : HistoryEvent
}

/**
 * User intents for history screen.
 */
sealed interface HistoryIntent {
    data class UpdateSearchQuery(val query: String) : HistoryIntent
    data object ClearSearch : HistoryIntent
    data class SelectProduct(val productId: String) : HistoryIntent
    data object NavigateToScan : HistoryIntent
    data object RefreshHistory : HistoryIntent
    data class UpdateFilter(val filter: HistoryFilter) : HistoryIntent
    data class UpdateSortOrder(val sortOrder: HistorySortOrder) : HistoryIntent
}

/**
 * ViewModel for the History screen.
 * Manages the list of scanned products and search/filter functionality.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val getHistoryUseCase: GetHistoryUseCase,
    // private val searchHistoryUseCase: SearchHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    init {
        loadHistory()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is HistoryIntent.ClearSearch -> clearSearch()
            is HistoryIntent.SelectProduct -> selectProduct(intent.productId)
            is HistoryIntent.NavigateToScan -> navigateToScan()
            is HistoryIntent.RefreshHistory -> loadHistory()
            is HistoryIntent.UpdateFilter -> updateFilter(intent.filter)
            is HistoryIntent.UpdateSortOrder -> updateSortOrder(intent.sortOrder)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // TODO: Replace with actual use case
                // val history = getHistoryUseCase()
                val history = getMockHistory()
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        historyEntries = history,
                        filteredEntries = applyFilters(history, state.searchQuery, state.filter, state.sortOrder),
                        isEmpty = history.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load history"
                    )
                }
                _events.emit(HistoryEvent.ShowError(e.message ?: "Failed to load history"))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.historyEntries, query, state.filter, state.sortOrder)
            state.copy(
                searchQuery = query,
                filteredEntries = filtered,
                isEmpty = filtered.isEmpty() && state.historyEntries.isNotEmpty()
            )
        }
    }

    private fun clearSearch() {
        updateSearchQuery("")
    }

    private fun selectProduct(productId: String) {
        viewModelScope.launch {
            _events.emit(HistoryEvent.NavigateToProductDetail(productId))
        }
    }

    private fun navigateToScan() {
        viewModelScope.launch {
            _events.emit(HistoryEvent.NavigateToScan)
        }
    }

    private fun updateFilter(filter: HistoryFilter) {
        _uiState.update { state ->
            val filtered = applyFilters(state.historyEntries, state.searchQuery, filter, state.sortOrder)
            state.copy(
                filter = filter,
                filteredEntries = filtered
            )
        }
    }

    private fun updateSortOrder(sortOrder: HistorySortOrder) {
        _uiState.update { state ->
            val filtered = applyFilters(state.historyEntries, state.searchQuery, state.filter, sortOrder)
            state.copy(
                sortOrder = sortOrder,
                filteredEntries = filtered
            )
        }
    }

    private fun applyFilters(
        entries: List<HistoryEntry>,
        query: String,
        filter: HistoryFilter,
        sortOrder: HistorySortOrder
    ): List<HistoryEntry> {
        var result = entries

        // Apply search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            result = result.filter { entry ->
                entry.displayName.lowercase().contains(lowerQuery) ||
                entry.displayBrand.lowercase().contains(lowerQuery) ||
                entry.displayModel?.lowercase()?.contains(lowerQuery) == true ||
                entry.scan.gtin.contains(lowerQuery)
            }
        }

        // Apply category filter
        if (filter.categories.isNotEmpty()) {
            result = result.filter { it.scan.snapshotData.category in filter.categories }
        }

        // Apply energy class filter
        if (filter.energyClasses.isNotEmpty()) {
            result = result.filter { entry ->
                entry.displayEnergyClass?.let { it in filter.energyClasses } ?: false
            }
        }

        // Apply date range filter
        // HistoryFilter does not contain fromDate/toDate according to Scan.kt
        
        // Apply sorting
        result = when (sortOrder) {
            HistorySortOrder.DATE_DESC -> result.sortedByDescending { it.scannedAt }
            HistorySortOrder.DATE_ASC -> result.sortedBy { it.scannedAt }
            HistorySortOrder.NAME_ASC -> result.sortedBy { it.displayName }
            HistorySortOrder.NAME_DESC -> result.sortedByDescending { it.displayName }
            HistorySortOrder.ENERGY_BEST -> result.sortedBy { it.displayEnergyClass?.ordinal ?: Int.MAX_VALUE }
            HistorySortOrder.REPAIRABILITY -> result.sortedByDescending { it.displayRepairabilityIndex ?: 0.0 }
        }

        return result
    }

    /**
     * Mock data for development/preview.
     * TODO: Remove when repository is implemented.
     */
    private fun getMockHistory(): List<HistoryEntry> {
        val now = Instant.now()
        return listOf(
            HistoryEntry(
                scan = Scan(
                    id = "scan_1",
                    userId = "user_1",
                    gtin = "3760000000001",
                    scannedAt = now.minusSeconds(86400),
                    snapshotData = ScanSnapshot(
                        productName = "iPhone 15 Pro",
                        brand = "Apple",
                        model = "A3517",
                        category = ProductCategory.ELECTRONIQUE,
                        energyClass = null,
                        repairabilityIndex = 7.2,
                        imageUrl = null
                    )
                ),
                product = null
            ),
            HistoryEntry(
                scan = Scan(
                    id = "scan_2",
                    userId = "user_1",
                    gtin = "3760000000002",
                    scannedAt = now.minusSeconds(86400 * 3),
                    snapshotData = ScanSnapshot(
                        productName = "MF205W80WB-14A30",
                        brand = "Midea",
                        model = "MF205W80WB",
                        category = ProductCategory.ELECTROMENAGER,
                        energyClass = EnergyClass.A,
                        repairabilityIndex = 8.2,
                        imageUrl = null
                    )
                ),
                product = null
            ),
            HistoryEntry(
                scan = Scan(
                    id = "scan_3",
                    userId = "user_1",
                    gtin = "3760000000003",
                    scannedAt = now.minusSeconds(86400 * 7),
                    snapshotData = ScanSnapshot(
                        productName = "GB167",
                        brand = "Beatsonic Sarl.",
                        model = "GB167",
                        category = ProductCategory.ELECTRONIQUE,
                        energyClass = EnergyClass.E,
                        repairabilityIndex = 5.2,
                        imageUrl = null
                    )
                ),
                product = null
            ),
            HistoryEntry(
                scan = Scan(
                    id = "scan_4",
                    userId = "user_1",
                    gtin = "3760000000004",
                    scannedAt = now.minusSeconds(86400 * 30),
                    snapshotData = ScanSnapshot(
                        productName = "PlayStation 5",
                        brand = "Sony",
                        model = "CFI-1216A",
                        category = ProductCategory.GAMING,
                        energyClass = EnergyClass.C,
                        repairabilityIndex = 6.5,
                        imageUrl = null
                    )
                ),
                product = null
            ),
            HistoryEntry(
                scan = Scan(
                    id = "scan_5",
                    userId = "user_1",
                    gtin = "3760000000005",
                    scannedAt = now.minusSeconds(86400 * 60),
                    snapshotData = ScanSnapshot(
                        productName = "Climatiseur Daikin",
                        brand = "Daikin",
                        model = "FTXM-R",
                        category = ProductCategory.CLIMATISATION,
                        energyClass = EnergyClass.A,
                        repairabilityIndex = 7.8,
                        imageUrl = null
                    )
                ),
                product = null
            )
        )
    }
}
