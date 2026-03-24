package com.wattson.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.ProductRepository
import com.wattson.domain.model.HistoryEntry
import com.wattson.domain.model.HistoryFilter
import com.wattson.domain.model.HistorySortOrder
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val historyEntries: List<HistoryEntry> = emptyList(),
    val filteredEntries: List<HistoryEntry> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    val sortOrder: HistorySortOrder = HistorySortOrder.DATE_DESC,
    val errorMessage: UiText? = null,
    val isEmpty: Boolean = false,
    val currentPage: Int = 0,
    val hasMorePages: Boolean = false,
    val totalScans: Long = 0
)

sealed interface HistoryEvent {
    data class NavigateToProductDetail(val productId: String) : HistoryEvent
    data object NavigateToScan : HistoryEvent
    data class ShowError(val message: UiText) : HistoryEvent
    data class ShowSnackbar(val message: UiText) : HistoryEvent
}

sealed interface HistoryIntent {
    data class UpdateSearchQuery(val query: String) : HistoryIntent
    data object ClearSearch : HistoryIntent
    data class SelectProduct(val productId: String) : HistoryIntent
    data object NavigateToScan : HistoryIntent
    data object RefreshHistory : HistoryIntent
    data object LoadMoreHistory : HistoryIntent
    data class UpdateFilter(val filter: HistoryFilter) : HistoryIntent
    data class UpdateSortOrder(val sortOrder: HistorySortOrder) : HistoryIntent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    private val pageSize = 20

    init {
        loadHistory()
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is HistoryIntent.ClearSearch -> clearSearch()
            is HistoryIntent.SelectProduct -> selectProduct(intent.productId)
            is HistoryIntent.NavigateToScan -> navigateToScan()
            is HistoryIntent.RefreshHistory -> loadHistory()
            is HistoryIntent.LoadMoreHistory -> loadMoreHistory()
            is HistoryIntent.UpdateFilter -> updateFilter(intent.filter)
            is HistoryIntent.UpdateSortOrder -> updateSortOrder(intent.sortOrder)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = authRepository.getCurrentUserId()
                android.util.Log.d("HistoryViewModel", "Loading history for user: $userId")

                val result = productRepository.getScanHistory(
                    userId = userId,
                    page = 0,
                    size = pageSize
                )

                result.fold(
                    onSuccess = { scans ->
                        android.util.Log.d("HistoryViewModel", "Loaded ${scans.size} scans from API")
                        val entries = scans.map { scan -> HistoryEntry(scan = scan, product = null) }

                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                historyEntries = entries,
                                filteredEntries = applyFilters(
                                    entries,
                                    state.searchQuery,
                                    state.filter,
                                    state.sortOrder
                                ),
                                isEmpty = entries.isEmpty(),
                                currentPage = 0,
                                hasMorePages = scans.size >= pageSize,
                                totalScans = scans.size.toLong()
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                historyEntries = emptyList(),
                                filteredEntries = emptyList(),
                                isEmpty = true,
                                errorMessage = null
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Exception loading history", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmpty = true,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun loadMoreHistory() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMorePages) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            try {
                val userId = authRepository.getCurrentUserId()
                val nextPage = currentState.currentPage + 1

                val result = productRepository.getScanHistory(
                    userId = userId,
                    page = nextPage,
                    size = pageSize
                )

                result.fold(
                    onSuccess = { scans ->
                        val newEntries = scans.map { scan -> HistoryEntry(scan = scan, product = null) }
                        val allEntries = currentState.historyEntries + newEntries

                        _uiState.update { state ->
                            state.copy(
                                isLoadingMore = false,
                                historyEntries = allEntries,
                                filteredEntries = applyFilters(
                                    allEntries,
                                    state.searchQuery,
                                    state.filter,
                                    state.sortOrder
                                ),
                                currentPage = nextPage,
                                hasMorePages = scans.size >= pageSize
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoadingMore = false) }
                        _events.emit(
                            HistoryEvent.ShowError(
                                error.toUiTextOr(UiText.StringResource(R.string.error_loading_generic))
                            )
                        )
                    }
                )
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
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
            state.copy(filter = filter, filteredEntries = filtered)
        }
    }

    private fun updateSortOrder(sortOrder: HistorySortOrder) {
        _uiState.update { state ->
            val filtered = applyFilters(state.historyEntries, state.searchQuery, state.filter, sortOrder)
            state.copy(sortOrder = sortOrder, filteredEntries = filtered)
        }
    }

    private fun applyFilters(
        entries: List<HistoryEntry>,
        query: String,
        filter: HistoryFilter,
        sortOrder: HistorySortOrder
    ): List<HistoryEntry> {
        var result = entries

        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            result = result.filter { entry ->
                entry.displayName.lowercase().contains(lowerQuery) ||
                    entry.displayBrand.lowercase().contains(lowerQuery) ||
                    entry.displayModel?.lowercase()?.contains(lowerQuery) == true ||
                    entry.scan.gtin.contains(lowerQuery)
            }
        }

        if (filter.categories.isNotEmpty()) {
            result = result.filter { it.scan.snapshotData.category in filter.categories }
        }

        if (filter.energyClasses.isNotEmpty()) {
            result = result.filter { entry ->
                entry.displayEnergyClass?.let { it in filter.energyClasses } ?: false
            }
        }

        return when (sortOrder) {
            HistorySortOrder.DATE_DESC -> result.sortedByDescending { it.scannedAt }
            HistorySortOrder.DATE_ASC -> result.sortedBy { it.scannedAt }
            HistorySortOrder.NAME_ASC -> result.sortedBy { it.displayName }
            HistorySortOrder.NAME_DESC -> result.sortedByDescending { it.displayName }
            HistorySortOrder.ENERGY_BEST -> result.sortedBy { it.displayEnergyClass?.ordinal ?: Int.MAX_VALUE }
            HistorySortOrder.REPAIRABILITY -> result.sortedByDescending { it.displayRepairabilityIndex ?: 0.0 }
        }
    }
}
