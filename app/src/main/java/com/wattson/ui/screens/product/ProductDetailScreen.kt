package com.wattson.ui.screens.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.wattson.R
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
import com.wattson.ui.components.EnergyClassBadge
import com.wattson.ui.components.EnergyBadgeSize
import com.wattson.ui.components.RepairabilityBadge
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import com.wattson.ui.theme.getRepairabilityColor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant

/**
 * Product Detail screen displaying comprehensive product information.
 */
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: ProductDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRepair: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ProductDetailScreen(
        uiState = uiState,
        events = viewModel.events,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        onNavigateToRepair = onNavigateToRepair,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    events: SharedFlow<ProductDetailEvent>,
    onIntent: (ProductDetailIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRepair: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle events
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is ProductDetailEvent.NavigateBack -> onNavigateBack()
                is ProductDetailEvent.NavigateToRepair -> onNavigateToRepair()
                is ProductDetailEvent.AddedToFavorites -> { /* Show snackbar */ }
                is ProductDetailEvent.RemovedFromFavorites -> { /* Show snackbar */ }
                is ProductDetailEvent.ShowError -> { /* Show snackbar */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_details)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ProductDetailIntent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(ProductDetailIntent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                            tint = if (uiState.isFavorite) WattsonColors.Error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            val product = uiState.product
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                product != null -> {
                    ProductDetailContent(
                        product = product,
                        metrics = uiState.metrics,
                        globalScore = uiState.globalScore,
                        userRating = uiState.userRating
                    )
                }
                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onIntent(ProductDetailIntent.LoadProduct) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    metrics: ProductMetrics?,
    globalScore: GlobalScore?,
    userRating: Double?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Product header with global score
        ProductHeader(
            product = product,
            globalScore = globalScore
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Energy Performance
        MetricCard(
            icon = Icons.Filled.EnergySavingsLeaf,
            title = stringResource(R.string.energy_performance),
            iconColor = WattsonColors.EnergyClassA
        ) {
            EnergyPerformanceContent(
                energyClass = product.energyLabel,
                energyKwhYear = metrics?.scores?.energy?.kwhPerYear
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Autonomy & Resistance
        MetricCard(
            icon = Icons.Filled.Battery5Bar,
            title = stringResource(R.string.autonomy_resistance),
            iconColor = WattsonColors.Primary
        ) {
            AutonomyResistanceContent(
                characteristics = product.characteristics
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Repairability
        MetricCard(
            icon = Icons.Filled.Build,
            title = stringResource(R.string.repairability),
            iconColor = getRepairabilityColor(product.repairabilityIndex ?: 0.0)
        ) {
            RepairabilityContent(
                repairabilityIndex = product.repairabilityIndex
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // User Reviews
        MetricCard(
            icon = Icons.Filled.People,
            title = stringResource(R.string.user_reviews),
            iconColor = Color(0xFF9C27B0)
        ) {
            UserReviewsContent(
                userRating = userRating
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProductHeader(
    product: Product,
    globalScore: GlobalScore?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                product.model?.let { model ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = model,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Global score gauge
        globalScore?.let { score ->
            GlobalScoreGauge(
                score = score,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
private fun GlobalScoreGauge(
    score: GlobalScore,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score.numericValue.toFloat() / 100f,
        animationSpec = tween(1000),
        label = "scoreProgress"
    )

    val scoreColor = when (score.letter) {
        "A" -> WattsonColors.EnergyClassA
        "B" -> WattsonColors.EnergyClassB
        "C" -> WattsonColors.EnergyClassC
        "D" -> WattsonColors.EnergyClassD
        "E" -> WattsonColors.EnergyClassE
        "F" -> WattsonColors.EnergyClassF
        else -> WattsonColors.OnSurfaceVariant
    }

    Box(
        modifier = modifier
            .drawBehind {
                // Background circle
                drawArc(
                    color = scoreColor.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Progress arc
                drawArc(
                    color = scoreColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.score_prefix),
                style = MaterialTheme.typography.labelSmall,
                color = WattsonColors.OnSurfaceVariant
            )
            Text(
                text = score.letter,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = scoreColor
            )
            Text(
                text = score.label,
                style = MaterialTheme.typography.labelSmall,
                color = WattsonColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun EnergyPerformanceContent(
    energyClass: EnergyClass?,
    energyKwhYear: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            energyKwhYear?.let {
                Text(
                    text = stringResource(R.string.energy_kwh_year, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        EnergyClassBadge(
            energyClass = energyClass,
            size = EnergyBadgeSize.LARGE,
            showScale = true
        )
    }
}

@Composable
private fun AutonomyResistanceContent(
    characteristics: ProductCharacteristics?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Endurance
        characteristics?.enduranceHours?.let { hours ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Battery5Bar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.endurance_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.hours_suffix, hours.toInt()),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Drop Resistance
        characteristics?.dropResistanceClass?.let { resistanceClass ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.drop_resistance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Resistance class badge
                Box(
                    modifier = Modifier
                        .background(
                            color = WattsonColors.Primary.copy(alpha = 0.1f),
                            shape = WattsonCorners.Small
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = resistanceClass.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = WattsonColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RepairabilityContent(
    repairabilityIndex: Double?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val classLetter = when {
                repairabilityIndex == null -> "N/A"
                repairabilityIndex >= 8.0 -> "A"
                repairabilityIndex >= 6.0 -> "B"
                repairabilityIndex >= 4.0 -> "C"
                repairabilityIndex >= 2.0 -> "D"
                else -> "E"
            }

            Text(
                text = stringResource(R.string.class_prefix, classLetter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            repairabilityIndex?.let {
                Text(
                    text = stringResource(R.string.index_prefix, String.format("%.2f", it)),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = getRepairabilityColor(it)
                )
            }
        }

        RepairabilityBadge(
            score = repairabilityIndex,
            showLabel = false
        )
    }
}

@Composable
private fun UserReviewsContent(
    userRating: Double?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.wattson_user_rating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        userRating?.let {
            Text(
                text = "${it.toInt()}/10",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF9C27B0)
            )
        } ?: Text(
            text = stringResource(R.string.not_rated_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = WattsonColors.Primary)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Erreur",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = WattsonColors.Error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ===== PREVIEWS =====

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductDetailContentPreview() {
    WattsonPreviewTheme {
        ProductDetailContent(
            product = Product(
                id = "prod_1",
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
                updatedAt = Instant.now()
            ),
            metrics = ProductMetrics(
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
            ),
            globalScore = GlobalScore(numericValue = 45.0, letter = "C", label = "Bon"),
            userRating = 5.0
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GlobalScoreGaugePreview() {
    WattsonPreviewTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlobalScoreGauge(
                score = GlobalScore(numericValue = 85.0, letter = "A", label = "Excellent"),
                modifier = Modifier.size(80.dp)
            )
            GlobalScoreGauge(
                score = GlobalScore(numericValue = 45.0, letter = "C", label = "Bon"),
                modifier = Modifier.size(80.dp)
            )
            GlobalScoreGauge(
                score = GlobalScore(numericValue = 20.0, letter = "E", label = "Passable"),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}
