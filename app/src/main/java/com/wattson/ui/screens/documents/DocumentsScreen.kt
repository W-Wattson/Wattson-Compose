package com.wattson.ui.screens.documents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.Document
import com.wattson.ui.components.DocumentCard
import com.wattson.ui.components.DocumentsEmptyState
import com.wattson.ui.components.WattsonPageTitle
import com.wattson.ui.components.WattsonSearchBar
import com.wattson.ui.components.YearFilterChip
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment

/**
 * Documents screen for managing receipts and warranties.
 */
@Composable
fun DocumentsScreen(
    uiState: DocumentsUiState,
    events: SharedFlow<DocumentsEvent>,
    onIntent: (DocumentsIntent) -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = uri.lastPathSegment ?: "document"
            onIntent(DocumentsIntent.UploadDocument(uri.toString(), fileName))
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onIntent(DocumentsIntent.RefreshDocuments)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle events
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is DocumentsEvent.NavigateToDocumentDetail -> onNavigateToDocument(event.documentId)
                is DocumentsEvent.ShowUploadPicker -> {
                    filePickerLauncher.launch("*/*")
                }
                is DocumentsEvent.ShowUploadSuccess -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.document_added_success, event.fileName))
                }
                is DocumentsEvent.ShowUploadError -> {
                    snackbarHostState.showSnackbar("${context.getString(R.string.error)}: ${event.message}")
                }
                is DocumentsEvent.ShowDeleteSuccess -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.document_deleted_success, event.documentName))
                }
                is DocumentsEvent.NavigateToPremium -> onNavigateToPremium()
                is DocumentsEvent.ShowQuotaExceeded -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.quota_exceeded))
                }
                is DocumentsEvent.StartDownload -> {
                    try {
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                        val request = DownloadManager.Request(Uri.parse(event.url)).apply {
                            setTitle(event.filename)
                            setDescription("Wattson — Téléchargement")
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, event.filename)
                            setAllowedOverMetered(true)
                            setAllowedOverRoaming(true)
                        }

                        dm.enqueue(request)
                        snackbarHostState.showSnackbar("Téléchargement démarré")
                    } catch (e: Exception) {
                        // fallback navigateur
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        snackbarHostState.showSnackbar("Téléchargement démarré")
                    }
                }

                is DocumentsEvent.ShowDownloadError -> {
                    snackbarHostState.showSnackbar("${context.getString(R.string.error)}: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(DocumentsIntent.StartUpload) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_document),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_document)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with stats and search
                DocumentsHeader(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { onIntent(DocumentsIntent.SearchDocuments(it)) },
                    onClearSearch = { onIntent(DocumentsIntent.SearchDocuments("")) },
                    totalDocuments = uiState.totalDocuments,
                    totalDevices = uiState.totalDevices,
                    activeWarranties = uiState.activeWarranties
                )

                // Year filter chips
                YearFilters(
                    availableYears = uiState.availableYears,
                    selectedYear = uiState.selectedYear,
                    documentsByYear = uiState.documentsByYear,
                    onYearSelected = { onIntent(DocumentsIntent.FilterByYear(it)) }
                )

                // Content
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }
                    uiState.documents.isEmpty() -> {
                        EmptyState(onAddDocument = { onIntent(DocumentsIntent.StartUpload) })
                    }
                    uiState.filteredDocuments.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                        NoSearchResultsState(searchQuery = uiState.searchQuery)
                    }
                    else -> {
                        DocumentsList(
                            documentsByYear = if (uiState.selectedYear != null) {
                                mapOf(uiState.selectedYear to uiState.filteredDocuments)
                            } else {
                                uiState.documentsByYear.filterKeys { it in uiState.filteredDocuments.map { doc ->
                                    java.time.ZonedDateTime.ofInstant(doc.uploadedAt, java.time.ZoneId.systemDefault()).year
                                } }
                            },
                            expandedYears = uiState.expandedYears,
                            onToggleYear = { onIntent(DocumentsIntent.ToggleYearExpanded(it)) },
                            onDocumentClick = { onIntent(DocumentsIntent.OpenDocument(it)) },
                            onDocumentDelete = { onIntent(DocumentsIntent.DeleteDocument(it)) },
                            onDocumentDownload = { doc -> onIntent(DocumentsIntent.DownloadDocument(doc)) }
                        )
                    }
                }

                // Upload progress overlay
                AnimatedVisibility(
                    visible = uiState.isUploadInProgress,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    UploadProgressOverlay(progress = uiState.uploadProgress)
                }
            }
        }
    }

    // Delete confirmation dialog
    uiState.showDeleteConfirmation?.let { document ->
        DeleteConfirmationDialog(
            documentName = document.productName,
            onConfirm = { onIntent(DocumentsIntent.ConfirmDelete) },
            onDismiss = { onIntent(DocumentsIntent.CancelDelete) }
        )
    }
}

@Composable
private fun DocumentsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    totalDocuments: Int,
    totalDevices: Int,
    activeWarranties: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        WattsonPageTitle(
            title = stringResource(R.string.documents_title),
            subtitle = stringResource(R.string.documents_subtitle)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        WattsonSearchBar(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_placeholder),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = totalDocuments.toString(), label = stringResource(R.string.documents_label))
            StatItem(value = totalDevices.toString(), label = stringResource(R.string.devices_label))
            StatItem(value = activeWarranties.toString(), label = stringResource(R.string.active_warranties_label))
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun YearFilters(
    availableYears: List<Int>,
    selectedYear: Int?,
    documentsByYear: Map<Int, List<Document>>,
    onYearSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        item {
            YearFilterChip(
                year = null,
                count = documentsByYear.values.flatten().size,
                isSelected = selectedYear == null,
                onClick = { onYearSelected(null) }
            )
        }

        // Year chips
        items(availableYears) { year ->
            YearFilterChip(
                year = year,
                count = documentsByYear[year]?.size ?: 0,
                isSelected = selectedYear == year,
                onClick = { onYearSelected(year) }
            )
        }
    }
}

@Composable
private fun DocumentsList(
    documentsByYear: Map<Int, List<Document>>,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    onDocumentClick: (String) -> Unit,
    onDocumentDelete: (Document) -> Unit,
    onDocumentDownload: (Document) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        documentsByYear.forEach { (year, documents) ->
            // Year header
            item(key = "header_$year") {
                YearSectionHeader(
                    year = year,
                    count = documents.size,
                    isExpanded = expandedYears.contains(year),
                    onToggle = { onToggleYear(year) }
                )
            }

            // Documents for this year
            if (expandedYears.contains(year)) {
                itemsIndexed(
                    items = documents,
                    key = { _, doc -> doc.id }
                ) { _, document ->
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DocumentCard(
                            document = document,
                            onClick = { onDocumentClick(document.id) },
                            onDownload = { onDocumentDownload(document) },
                            onDelete = { onDocumentDelete(document) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearSectionHeader(
    year: Int,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        shape = WattsonCorners.Card,
        color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isExpanded) 0.dp else 1.dp,
        border = if (!isExpanded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Count badge
            Box(
                modifier = Modifier
                    .background(
                        color = (if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.1f),
                        shape = WattsonCorners.Small
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Réduire" else "Développer",
                tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(
    onAddDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocumentsEmptyState(
        onAddDocument = onAddDocument,
        modifier = modifier.fillMaxSize()
    )
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
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UploadProgressOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = WattsonCorners.Card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.uploading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    documentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_document_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.delete_document_confirmation, documentName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = WattsonColors.Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ===== PREVIEWS =====

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DocumentsHeaderPreview() {
    WattsonPreviewTheme {
        DocumentsHeader(
            searchQuery = "",
            onSearchQueryChange = {},
            onClearSearch = {},
            totalDocuments = 10,
            totalDevices = 6,
            activeWarranties = 3
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YearSectionHeaderExpandedPreview() {
    WattsonPreviewTheme {
        YearSectionHeader(
            year = 2025,
            count = 5,
            isExpanded = true,
            onToggle = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YearSectionHeaderCollapsedPreview() {
    WattsonPreviewTheme {
        YearSectionHeader(
            year = 2024,
            count = 8,
            isExpanded = false,
            onToggle = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UploadProgressOverlayPreview() {
    WattsonPreviewTheme {
        UploadProgressOverlay(progress = 0.65f)
    }
}
