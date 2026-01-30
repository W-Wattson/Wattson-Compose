package com.wattson.ui.screens.repair

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.ProductCategory
import com.wattson.ui.components.WattsonPageTitle
import com.wattson.ui.theme.WattsonTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repair screen displaying repair cases, tips, and AI-assisted repair guidance.
 *
 * @param viewModel The ViewModel managing repair state
 * @param onNavigateToScan Callback to navigate to scan screen for new repair case
 * @param onNavigateToCaseDetail Callback to navigate to case detail
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairScreen(
    viewModel: RepairViewModel = hiltViewModel(),
    onNavigateToScan: () -> Unit = {},
    onNavigateToCaseDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Handle one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RepairEvent.NavigateToScan -> onNavigateToScan()
                is RepairEvent.NavigateToCaseDetail -> onNavigateToCaseDetail(event.caseId)
                is RepairEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is RepairEvent.CaseCreated -> snackbarHostState.showSnackbar(context.getString(R.string.repair_case_created))
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onIntent(RepairIntent.StartNewCase) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.new_repair)) }
            )
        }
    ) { paddingValues ->
        RepairContent(
            uiState = uiState,
            onIntent = viewModel::onIntent,
            modifier = Modifier.padding(paddingValues)
        )
    }

    // New case dialog
    if (uiState.showNewCaseDialog) {
        NewRepairCaseDialog(
            onDismiss = { viewModel.onIntent(RepairIntent.DismissNewCaseDialog) },
            onScanProduct = { viewModel.onIntent(RepairIntent.NavigateToScan) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepairContent(
    uiState: RepairUiState,
    onIntent: (RepairIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { onIntent(RepairIntent.RefreshRepairData) },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                RepairHeader()
            }

            // Error banner
            if (uiState.errorMessage != null) {
                item {
                    ErrorBanner(
                        message = uiState.errorMessage,
                        onDismiss = { onIntent(RepairIntent.DismissError) },
                        onRetry = { onIntent(RepairIntent.RefreshRepairData) }
                    )
                }
            }

            // Active Cases Section
            if (uiState.activeCases.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.active_repairs),
                        icon = Icons.Default.Build
                    )
                }

                itemsIndexed(
                    items = uiState.activeCases,
                    key = { _, case -> case.id }
                ) { index, repairCase ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        RepairCaseCard(
                            repairCase = repairCase,
                            onClick = { onIntent(RepairIntent.OpenCase(repairCase.id)) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Completed Cases Section
            if (uiState.completedCases.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.completed_repairs),
                        icon = Icons.Default.CheckCircle
                    )
                }

                itemsIndexed(
                    items = uiState.completedCases,
                    key = { _, case -> case.id }
                ) { index, repairCase ->
                    RepairCaseCard(
                        repairCase = repairCase,
                        onClick = { onIntent(RepairIntent.OpenCase(repairCase.id)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Empty state for cases
            if (uiState.activeCases.isEmpty() && uiState.completedCases.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyRepairCasesCard(
                        onStartNewCase = { onIntent(RepairIntent.StartNewCase) }
                    )
                }
            }

            // Repair Tips Section
            if (uiState.repairTips.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.repair_tips),
                        icon = Icons.Outlined.Lightbulb
                    )
                }

                item {
                    RepairTipsCarousel(tips = uiState.repairTips)
                }
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun RepairHeader() {
    WattsonPageTitle(
        title = stringResource(R.string.repair_title),
        subtitle = stringResource(R.string.repair_subtitle)
    )
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun RepairCaseCard(
    repairCase: RepairCase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repairCase.product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = repairCase.product.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RepairStatusBadge(status = repairCase.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Symptoms
            if (repairCase.symptoms.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.symptoms_label, repairCase.symptoms.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Diagnosis if available
            if (!repairCase.diagnosis.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = repairCase.diagnosis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer with date and estimated cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(repairCase.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (repairCase.estimatedCost != null) {
                    Text(
                        text = "~${repairCase.estimatedCost.toInt()}€",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RepairStatusBadge(status: RepairCaseStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        RepairCaseStatus.OPEN -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            stringResource(R.string.status_open)
        )
        RepairCaseStatus.DIAGNOSING -> Triple(
            Color(0xFFFFF3E0), // Orange light
            Color(0xFFE65100), // Orange dark
            stringResource(R.string.status_diagnosing)
        )
        RepairCaseStatus.WAITING_PARTS -> Triple(
            Color(0xFFF3E5F5), // Purple light
            Color(0xFF7B1FA2), // Purple
            stringResource(R.string.status_waiting_parts)
        )
        RepairCaseStatus.IN_REPAIR -> Triple(
            Color(0xFFE3F2FD), // Blue light
            Color(0xFF1565C0), // Blue
            stringResource(R.string.status_in_repair)
        )
        RepairCaseStatus.COMPLETED -> Triple(
            Color(0xFFE8F5E9), // Green light
            Color(0xFF2E7D32), // Green
            stringResource(R.string.status_completed)
        )
        RepairCaseStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.status_cancelled)
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyRepairCasesCard(
    onStartNewCase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.no_active_repairs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_repair_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onStartNewCase) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_product))
            }
        }
    }
}

@Composable
private fun RepairTipsCarousel(tips: List<RepairTip>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 20.dp)
    ) {
        items(tips, key = { it.id }) { tip ->
            RepairTipCard(tip = tip)
        }
    }
}

@Composable
private fun RepairTipCard(tip: RepairTip) {
    val categoryInfo = getCategoryInfo(tip.category)

    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    categoryInfo.color.copy(alpha = 0.2f),
                                    categoryInfo.color.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryInfo.icon,
                        contentDescription = null,
                        tint = categoryInfo.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = categoryInfo.color.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = categoryInfo.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryInfo.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tip.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tip.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.retry),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NewRepairCaseDialog(
    onDismiss: () -> Unit,
    onScanProduct: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.new_repair),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.start_repair_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onScanProduct) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_product))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Helper data class for category styling
private data class CategoryInfo(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

@Composable
private fun getCategoryInfo(category: ProductCategory): CategoryInfo {
    return when (category) {
        ProductCategory.ELECTRONIQUE -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_electronics),
            color = Color(0xFF2196F3)
        )
        ProductCategory.ELECTROMENAGER -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_appliances),
            color = Color(0xFF4CAF50)
        )
        ProductCategory.GAMING -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_gaming),
            color = Color(0xFF9C27B0)
        )
        ProductCategory.CLIMATISATION -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_climatisation),
            color = Color(0xFF00BCD4)
        )
        ProductCategory.INFORMATIQUE -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_it),
            color = Color(0xFF607D8B)
        )
        ProductCategory.TELEPHONIE -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_telephony),
            color = Color(0xFFFF5722)
        )
        ProductCategory.AUDIO_VIDEO -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_audio_video),
            color = Color(0xFFE91E63)
        )
        ProductCategory.OTHER -> CategoryInfo(
            icon = Icons.Default.Build,
            label = stringResource(R.string.cat_other),
            color = Color(0xFF795548)
        )
    }
}

private fun formatDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun RepairScreenPreview() {
    WattsonTheme {
        RepairContent(
            uiState = RepairUiState(
                repairTips = listOf(
                    RepairTip(
                        id = "1",
                        title = "Nettoyez vos filtres",
                        description = "Un nettoyage mensuel prolonge la durée de vie",
                        category = ProductCategory.ELECTROMENAGER
                    ),
                    RepairTip(
                        id = "2",
                        title = "Évitez les charges à 100%",
                        description = "Préservez la batterie en limitant la charge",
                        category = ProductCategory.ELECTRONIQUE
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RepairStatusBadgePreview() {
    WattsonTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            RepairCaseStatus.entries.forEach { status ->
                RepairStatusBadge(status = status)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewRepairCaseDialogPreview() {
    WattsonTheme {
        NewRepairCaseDialog(
            onDismiss = {},
            onScanProduct = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyRepairCasesCardPreview() {
    WattsonTheme {
        EmptyRepairCasesCard(onStartNewCase = {})
    }
}
