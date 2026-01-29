package com.wattson.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.HistoryEntry
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.Scan
import com.wattson.domain.model.ScanSnapshot
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import java.time.Instant

/**
 * Product card for History list
 * Displays brand, model, repairability index, and energy class
 * Based on SFD maquette "Page Historique [Mobile]"
 */
@Composable
fun ProductCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image or category icon
            ProductThumbnail(
                imageUrl = entry.scan.snapshotData.imageUrl,
                category = entry.scan.snapshotData.category,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Product info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.displayBrand,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val displayModel = entry.displayModel
                if (!displayModel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                RepairabilityBadge(
                    score = entry.displayRepairabilityIndex,
                    showLabel = true
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Energy class badge
            EnergyClassBadge(
                energyClass = entry.displayEnergyClass,
                size = EnergyBadgeSize.MEDIUM,
                showScale = true
            )
        }
    }
}

/**
 * Compact product card variant
 * For smaller displays or denser lists
 */
@Composable
fun ProductCardCompact(
    brand: String,
    model: String?,
    energyClass: EnergyClass?,
    repairabilityIndex: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = WattsonCorners.CardSmall,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = brand,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                model?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                repairabilityIndex?.let {
                    Text(
                        text = "Réparabilité: ${String.format("%.1f", it)}/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            EnergyClassBadge(
                energyClass = energyClass,
                size = EnergyBadgeSize.SMALL,
                showScale = false
            )
        }
    }
}

/**
 * Product thumbnail with fallback to category icon
 */
@Composable
fun ProductThumbnail(
    imageUrl: String?,
    category: ProductCategory,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(WattsonCorners.Medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image produit",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // Fallback to category icon
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = category.name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Get appropriate icon for product category
 */
fun getCategoryIcon(category: ProductCategory): ImageVector {
    return when (category) {
        ProductCategory.ELECTRONIQUE -> Icons.Outlined.PhoneAndroid
        ProductCategory.ELECTROMENAGER -> Icons.Outlined.Kitchen
        ProductCategory.GAMING -> Icons.Outlined.SportsEsports
        ProductCategory.INFORMATIQUE -> Icons.Outlined.Computer
        ProductCategory.AUDIO_VIDEO -> Icons.Outlined.Tv
        ProductCategory.TELEPHONIE -> Icons.Outlined.PhoneAndroid
        else -> Icons.Outlined.Computer
    }
}

/**
 * Empty state for product list
 */
@Composable
fun ProductListEmptyState(
    message: String = "Aucun produit scanné",
    subMessage: String = "Scannez un produit pour commencer",
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
            imageVector = Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ===== PREVIEWS =====

@Preview(showBackground = true)
@Composable
private fun ProductCardPreview() {
    WattsonPreviewTheme {
        val mockEntry = HistoryEntry(
            scan = Scan(
                id = "1",
                userId = "user1",
                gtin = "3760000000001",
                scannedAt = Instant.now(),
                snapshotData = ScanSnapshot(
                    productName = "iPhone 15 Pro",
                    brand = "Apple",
                    model = "A3517",
                    category = ProductCategory.ELECTRONIQUE,
                    energyClass = EnergyClass.A,
                    repairabilityIndex = 8.2,
                    imageUrl = null
                )
            ),
            product = null
        )

        ProductCard(
            entry = mockEntry,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductCardEnergyEPreview() {
    WattsonPreviewTheme {
        val mockEntry = HistoryEntry(
            scan = Scan(
                id = "2",
                userId = "user1",
                gtin = "3760000000002",
                scannedAt = Instant.now(),
                snapshotData = ScanSnapshot(
                    productName = "Beatsonic Sarl",
                    brand = "Beatsonic Sarl.",
                    model = "GB167",
                    category = ProductCategory.ELECTROMENAGER,
                    energyClass = EnergyClass.E,
                    repairabilityIndex = 5.2,
                    imageUrl = null
                )
            ),
            product = null
        )

        ProductCard(
            entry = mockEntry,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductListEmptyStatePreview() {
    WattsonPreviewTheme {
        ProductListEmptyState()
    }
}
