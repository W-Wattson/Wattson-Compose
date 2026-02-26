package com.wattson.ui.screens.documents.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentMetadata
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.WarrantyType
import com.wattson.ui.theme.WattsonTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Document detail screen showing full document information and actions.
 *
 * @param documentId The ID of the document to display
 * @param viewModel The ViewModel managing document detail state
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    viewModel: DocumentDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DocumentDetailEvent.NavigateBack -> onNavigateBack()
                is DocumentDetailEvent.OpenDocument -> {
                    // TODO: Open document with intent
                    snackbarHostState.showSnackbar("Ouverture du document...")
                }
                is DocumentDetailEvent.ShareDocument -> {
                    // TODO: Share document
                    snackbarHostState.showSnackbar("Partage du document...")
                }
                is DocumentDetailEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DocumentDetailEvent.DocumentDeleted -> {
                    snackbarHostState.showSnackbar("Document supprimé")
                }
                is DocumentDetailEvent.DownloadStarted -> {
                    snackbarHostState.showSnackbar("Téléchargement démarré")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Détails du document",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onIntent(DocumentDetailIntent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(DocumentDetailIntent.ShareDocument) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager"
                        )
                    }
                    IconButton(onClick = { viewModel.onIntent(DocumentDetailIntent.RequestDelete) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(paddingValues))
            }
            uiState.document != null -> {
                DocumentDetailContent(
                    document = uiState.document!!,
                    isWarrantyActive = uiState.isWarrantyActive,
                    daysUntilExpiry = uiState.daysUntilWarrantyExpiry,
                    onOpenDocument = { viewModel.onIntent(DocumentDetailIntent.OpenDocument) },
                    onDownload = { viewModel.onIntent(DocumentDetailIntent.DownloadDocument) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onIntent(DocumentDetailIntent.LoadDocument) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            documentName = uiState.document?.productName ?: "ce document",
            onConfirm = { viewModel.onIntent(DocumentDetailIntent.ConfirmDelete) },
            onDismiss = { viewModel.onIntent(DocumentDetailIntent.DismissDeleteDialog) }
        )
    }
}

@Composable
private fun DocumentDetailContent(
    document: Document,
    isWarrantyActive: Boolean,
    daysUntilExpiry: Int?,
    onOpenDocument: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document Preview Card
        item {
            DocumentPreviewCard(
                document = document,
                onOpen = onOpenDocument,
                onDownload = onDownload
            )
        }

        // Product Info Card
        item {
            ProductInfoCard(document = document)
        }

        // Warranty Status Card (if applicable)
        if (document.type == DocumentType.GARANTIE || document.metadata.warrantyEndDate != null) {
            item {
                WarrantyStatusCard(
                    isActive = isWarrantyActive,
                    daysRemaining = daysUntilExpiry,
                    warrantyType = document.metadata.warrantyType,
                    startDate = document.metadata.warrantyStartDate,
                    endDate = document.metadata.warrantyEndDate
                )
            }
        }

        // Purchase Details Card
        if (document.metadata.merchant != null || document.metadata.totalAmount != null) {
            item {
                PurchaseDetailsCard(metadata = document.metadata)
            }
        }

        // Document Info Card
        item {
            DocumentInfoCard(document = document)
        }
    }
}

@Composable
private fun DocumentPreviewCard(
    document: Document,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getFileExtension(document.fileUrl).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    onClick = onOpen,
                    icon = Icons.Default.OpenInNew,
                    text = "Ouvrir",
                    modifier = Modifier.weight(1f)
                )
                
                ActionButton(
                    onClick = onDownload,
                    icon = Icons.Default.Download,
                    text = "Télécharger",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ProductInfoCard(document: Document) {
    InfoCard(title = "Produit") {
        InfoRow(
            icon = Icons.Default.ShoppingBag,
            label = "Nom",
            value = document.productName
        )

        if (document.gtin != null) {
            InfoRow(
                icon = Icons.Default.Description,
                label = "Code EAN",
                value = document.gtin
            )
        }

        InfoRow(
            icon = Icons.Default.Description,
            label = "Catégorie",
            value = getCategoryLabel(document.productCategory)
        )
    }
}

@Composable
private fun WarrantyStatusCard(
    isActive: Boolean,
    daysRemaining: Int?,
    warrantyType: WarrantyType?,
    startDate: LocalDate?,
    endDate: LocalDate?
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            !isActive -> MaterialTheme.colorScheme.error
            daysRemaining != null && daysRemaining <= 30 -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        },
        animationSpec = tween(300),
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            !isActive -> Icons.Outlined.Error
                            daysRemaining != null && daysRemaining <= 30 -> Icons.Outlined.Schedule
                            else -> Icons.Outlined.CheckCircle
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = when {
                            !isActive -> "Garantie expirée"
                            daysRemaining != null && daysRemaining <= 30 -> "Expire bientôt"
                            else -> "Garantie active"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )

                    if (daysRemaining != null && isActive) {
                        Text(
                            text = "$daysRemaining jours restants",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (daysRemaining != null && isActive) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress bar
                val progress = if (startDate != null && endDate != null) {
                    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toFloat()
                    val elapsed = totalDays - daysRemaining
                    (elapsed / totalDays).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Warranty details
            if (warrantyType != null) {
                DetailRow(
                    label = "Type",
                    value = getWarrantyTypeLabel(warrantyType)
                )
            }

            if (startDate != null) {
                DetailRow(
                    label = "Début",
                    value = formatDate(startDate)
                )
            }

            if (endDate != null) {
                DetailRow(
                    label = "Fin",
                    value = formatDate(endDate)
                )
            }
        }
    }
}

@Composable
private fun PurchaseDetailsCard(metadata: DocumentMetadata) {
    InfoCard(title = "Détails d'achat") {
        if (metadata.merchant != null) {
            InfoRow(
                icon = Icons.Default.Store,
                label = "Marchand",
                value = metadata.merchant
            )
        }

        if (metadata.purchaseDate != null) {
            InfoRow(
                icon = Icons.Default.CalendarMonth,
                label = "Date d'achat",
                value = formatDate(metadata.purchaseDate)
            )
        }

        if (metadata.totalAmount != null) {
            InfoRow(
                icon = Icons.Default.ShoppingBag,
                label = "Montant",
                value = "${String.format(Locale.FRANCE, "%.2f", metadata.totalAmount)} ${metadata.currency}"
            )
        }
    }
}

@Composable
private fun DocumentInfoCard(document: Document) {
    InfoCard(title = "Informations") {
        InfoRow(
            icon = Icons.Default.Description,
            label = "Type",
            value = getDocumentTypeLabel(document.type)
        )

        InfoRow(
            icon = Icons.Default.CalendarMonth,
            label = "Date du document",
            value = formatDate(document.documentDate)
        )

        InfoRow(
            icon = Icons.Default.Description,
            label = "Format",
            value = getFileExtension(document.fileUrl).uppercase()
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
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
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.delete_document_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_document_confirmation_detail, documentName),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Helper functions

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE)
    return date.format(formatter)
}

private fun getFileExtension(url: String): String {
    return url.substringAfterLast('.', "pdf")
}

private fun getCategoryLabel(category: ProductCategory): String {
    return when (category) {
        ProductCategory.ELECTRONIQUE -> "Électronique"
        ProductCategory.ELECTROMENAGER -> "Électroménager"
        ProductCategory.ECLAIRAGE -> "Éclairage"
        ProductCategory.GAMING -> "Gaming"
        ProductCategory.CLIMATISATION -> "Climatisation"
        ProductCategory.INFORMATIQUE -> "Informatique"
        ProductCategory.TELEPHONIE -> "Téléphonie"
        ProductCategory.AUDIO_VIDEO -> "Audio/Vidéo"
        ProductCategory.OTHER -> "Autre"
    }
}

private fun getDocumentTypeLabel(type: DocumentType): String {
    return when (type) {
        DocumentType.FACTURE -> "Facture"
        DocumentType.GARANTIE -> "Garantie"
        DocumentType.MANUEL -> "Manuel"
        DocumentType.OTHER -> "Autre"
    }
}

private fun getWarrantyTypeLabel(type: WarrantyType): String {
    return when (type) {
        WarrantyType.LEGAL -> "Garantie légale"
        WarrantyType.MANUFACTURER -> "Garantie constructeur"
        WarrantyType.EXTENDED -> "Extension de garantie"
        WarrantyType.COMMERCIAL -> "Garantie commerciale"
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun DocumentDetailContentPreview() {
    WattsonTheme {
        DocumentDetailContent(
            document = Document(
                id = "doc_1",
                userId = "user_123",
                type = DocumentType.GARANTIE,
                productName = "iPhone 15 Pro",
                productCategory = ProductCategory.ELECTRONIQUE,
                gtin = "0194253401148",
                fileUrl = "https://example.com/doc.pdf",
                thumbnailUrl = null,
                documentDate = LocalDate.now(),
                metadata = DocumentMetadata(
                    merchant = "Apple Store",
                    purchaseDate = LocalDate.now().minusMonths(3),
                    totalAmount = 1229.0,
                    warrantyStartDate = LocalDate.now().minusMonths(3),
                    warrantyEndDate = LocalDate.now().plusMonths(21),
                    warrantyType = WarrantyType.MANUFACTURER
                )
            ),
            isWarrantyActive = true,
            daysUntilExpiry = 640,
            onOpenDocument = {},
            onDownload = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WarrantyExpiringPreview() {
    WattsonTheme {
        WarrantyStatusCard(
            isActive = true,
            daysRemaining = 15,
            warrantyType = WarrantyType.MANUFACTURER,
            startDate = LocalDate.now().minusYears(2),
            endDate = LocalDate.now().plusDays(15)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WarrantyExpiredPreview() {
    WattsonTheme {
        WarrantyStatusCard(
            isActive = false,
            daysRemaining = null,
            warrantyType = WarrantyType.LEGAL,
            startDate = LocalDate.now().minusYears(3),
            endDate = LocalDate.now().minusYears(1)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteConfirmationDialogPreview() {
    WattsonTheme {
        DeleteConfirmationDialog(
            documentName = "iPhone 15 Pro",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
