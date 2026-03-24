package com.wattson.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentMetadata
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.ProductCategory
import com.wattson.ui.i18n.labelResId
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Card component displaying a document thumbnail, metadata, and quick actions for the documents list.
 */
@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document thumbnail/icon
            DocumentThumbnail(
                thumbnailUrl = document.thumbnailUrl,
                fileUrl = document.fileUrl,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Document info
            Column(modifier = Modifier.weight(1f)) {
                // Category label
                if (document.productCategory.name != "OTHER") {
                    Text(
                        text = stringResource(document.productCategory.labelResId()).uppercase(
                            Locale.getDefault()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Document type tag
                DocumentTypeTag(
                    type = document.type,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Product name
                Text(
                    text = document.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Date
                Text(
                    text = document.documentDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            .withLocale(Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.download)) },
                        onClick = {
                            showMenu = false
                            onDownload()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.delete),
                                color = WattsonColors.Error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Document type tag (Facture, Garantie, etc.) used to visually label document category.
 */
@Composable
fun DocumentTypeTag(
    type: DocumentType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (type) {
        DocumentType.FACTURE -> WattsonColors.TagFacture to WattsonColors.White
        DocumentType.GARANTIE -> WattsonColors.TagGarantie to WattsonColors.White
        DocumentType.MANUEL -> WattsonColors.Info to WattsonColors.White
        DocumentType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant to WattsonColors.White
    }

    val label = stringResource(type.labelResId())

    Surface(
        modifier = modifier,
        shape = WattsonCorners.Tag,
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Document thumbnail with fallback icon
 */
@Composable
fun DocumentThumbnail(
    thumbnailUrl: String?,
    fileUrl: String,
    modifier: Modifier = Modifier
) {
    val isPdf = fileUrl.lowercase().endsWith(".pdf")
    val isImage = fileUrl.lowercase().run {
        endsWith(".jpg") || endsWith(".jpeg") || endsWith(".png")
    }

    Box(
        modifier = modifier
            .clip(WattsonCorners.Small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.document_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector = when {
                    isPdf -> Icons.Outlined.PictureAsPdf
                    isImage -> Icons.Outlined.Image
                    else -> Icons.Outlined.Description
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Document year accordion header used in the documents list.
 */
@Composable
fun DocumentYearHeader(
    year: Int,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = WattsonCorners.Medium,
        color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )

                // Count badge
                Surface(
                    shape = WattsonCorners.Full,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WattsonColors.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Year filter chip for document filtering
 */
@Composable
fun YearFilterChip(
    year: Int?,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = WattsonCorners.Full,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                Text(
                    text = year?.toString() ?: stringResource(R.string.all_documents),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

            Surface(
                shape = WattsonCorners.Full,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Empty state for documents
 */
@Composable
fun DocumentsEmptyState(
    onAddDocument: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_documents),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.no_documents_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ===== PREVIEWS =====

@Preview(showBackground = true)
@Composable
private fun DocumentCardFacturePreview() {
    WattsonPreviewTheme {
        val mockDocument = Document(
            id = "1",
            userId = "user1",
            type = DocumentType.FACTURE,
            productName = "iPhone 15 Pro",
            productCategory = ProductCategory.ELECTRONIQUE,
            gtin = "3760000000001",
            fileUrl = "document.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2025, 1, 15)
        )

        DocumentCard(
            document = mockDocument,
            onClick = {},
            onDownload = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentCardGarantiePreview() {
    WattsonPreviewTheme {
        val mockDocument = Document(
            id = "2",
            userId = "user1",
            type = DocumentType.GARANTIE,
            productName = "MacBook Air M3",
            productCategory = ProductCategory.ELECTRONIQUE,
            gtin = "3760000000002",
            fileUrl = "warranty.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2024, 11, 22)
        )

        DocumentCard(
            document = mockDocument,
            onClick = {},
            onDownload = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentTypeTagsPreview() {
    WattsonPreviewTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            DocumentTypeTag(type = DocumentType.FACTURE)
            DocumentTypeTag(type = DocumentType.GARANTIE)
            DocumentTypeTag(type = DocumentType.MANUEL)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun YearFilterChipsPreview() {
    WattsonPreviewTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            YearFilterChip(year = 2025, count = 1, isSelected = true, onClick = {})
            YearFilterChip(year = 2024, count = 5, isSelected = false, onClick = {})
            YearFilterChip(year = 2023, count = 4, isSelected = false, onClick = {})
        }
    }
}
