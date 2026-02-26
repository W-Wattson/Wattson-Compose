package com.wattson.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.HistoryEntry
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.Scan
import com.wattson.domain.model.ScanSnapshot
import com.wattson.ui.components.ProductCard
import com.wattson.ui.components.ProductListEmptyState
import com.wattson.ui.components.WattsonPageTitle
import com.wattson.ui.components.WattsonScanFab
import com.wattson.ui.components.WattsonSearchBar
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonPreviewTheme
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * History screen displaying all scanned products.
 */
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onProductClick: (String) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation state for staggered list items
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Scaffold(
        floatingActionButton = {
            WattsonScanFab(
                onClick = onScanClick,
                text = stringResource(R.string.scan_button)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header section
                HistoryHeader(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onClearSearch = onClearSearch,
                    resultCount = uiState.filteredEntries.size
                )
                
                // Content
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }
                    uiState.historyEntries.isEmpty() -> {
                        EmptyHistoryState(onScanClick = onScanClick)
                    }
                    uiState.filteredEntries.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                        NoSearchResultsState(searchQuery = uiState.searchQuery)
                    }
                    else -> {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn()
                        ) {
                            HistoryList(
                                entries = uiState.filteredEntries,
                                onProductClick = onProductClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        WattsonPageTitle(
            title = stringResource(R.string.history_title),
            subtitle = stringResource(R.string.history_subtitle)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        WattsonSearchBar(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_placeholder),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Results count
        if (searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val resultsText = if (resultCount <= 1) {
                stringResource(R.string.results_count_singular, resultCount)
            } else {
                stringResource(R.string.results_count_plural, resultCount)
            }
            Text(
                text = resultsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryList(
    entries: List<HistoryEntry>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 100.dp // Extra padding for FAB
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.scan.id }
        ) { index, entry ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it * (index + 1) / 3 }
                ) + fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
            ) {
                ProductCard(
                    entry = entry,
                    onClick = { onProductClick("scan:${entry.scan.id}") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyHistoryState(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ProductListEmptyState()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        com.wattson.ui.components.WattsonButton(
            text = stringResource(R.string.scan_button),
            onClick = onScanClick,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun NoSearchResultsState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.displayMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.no_results),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.no_results_description, searchQuery),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Preview helpers
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenPreview() {
    WattsonPreviewTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                filteredEntries = getMockEntries()
            ),
            onSearchQueryChange = {},
            onClearSearch = {},
            onProductClick = {},
            onScanClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    WattsonPreviewTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                historyEntries = emptyList(),
                filteredEntries = emptyList(),
                isEmpty = true
            ),
            onSearchQueryChange = {},
            onClearSearch = {},
            onProductClick = {},
            onScanClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenLoadingPreview() {
    WattsonPreviewTheme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = true),
            onSearchQueryChange = {},
            onClearSearch = {},
            onProductClick = {},
            onScanClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenSearchPreview() {
    WattsonPreviewTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                searchQuery = "Apple",
                historyEntries = getMockEntries(),
                filteredEntries = getMockEntries().filter { 
                    it.displayBrand.contains("Apple", ignoreCase = true) 
                }
            ),
            onSearchQueryChange = {},
            onClearSearch = {},
            onProductClick = {},
            onScanClick = {}
        )
    }
}

private fun getMockEntries(): List<HistoryEntry> {
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
        )
    )
}
