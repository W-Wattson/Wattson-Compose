package com.wattson.ui.screens.product

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.annotation.StringRes
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.GlobalScore
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.ProductMetrics
import com.wattson.ui.components.EnergyClassBadge
import com.wattson.ui.components.EnergyBadgeSize
import com.wattson.ui.components.RepairabilityBadge
import com.wattson.ui.i18n.asString
import com.wattson.ui.i18n.labelResId
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import com.wattson.ui.theme.getRepairabilityColor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

/**
 * Product Detail screen displaying comprehensive product information.
 * Shows conditional environmental blocks — hidden when data is unavailable.
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
                        globalScore = uiState.globalScore
                    )
                }
                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage.asString(),
                        onRetry = { onIntent(ProductDetailIntent.LoadProduct) }
                    )
                }
            }
        }
    }
}

// ===== MAIN CONTENT =====

@Composable
private fun ProductDetailContent(
    product: Product,
    metrics: ProductMetrics?,
    globalScore: GlobalScore?,
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

        // === CONDITIONAL BLOCKS — only shown when data is available ===

        // 1. Energy Performance (shown if energyLabel OR kwhPerYear is available)
        val hasEnergyData = product.energyLabel != null || product.kwhPerYear != null
        if (hasEnergyData) {
            MetricCard(
                icon = Icons.Filled.EnergySavingsLeaf,
                title = stringResource(R.string.energy_performance),
                iconColor = WattsonColors.EnergyClassA
            ) {
                EnergyPerformanceContent(product = product)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Consumption Details (shown if EEI, standby or off power available)
        val hasConsumptionData = product.energyEfficiencyIndex != null ||
                product.powerStandbyMode != null || product.powerOffMode != null
        if (hasConsumptionData) {
            MetricCard(
                icon = Icons.Filled.EnergySavingsLeaf,
                title = stringResource(R.string.consumption_details),
                iconColor = Color(0xFF00897B)
            ) {
                ConsumptionDetailsContent(product = product)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2b. EPREL Product Details (shown if eprelDetails is non-empty)
        if (product.eprelDetails.isNotEmpty()) {
            EprelDetailsSection(
                productGroup = product.eprelProductGroup,
                details = product.eprelDetails
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Noise Emission (shown if noiseDecibels OR noiseClass)
        val hasNoiseData = product.noiseDecibels != null || product.noiseClass != null
        if (hasNoiseData) {
            MetricCard(
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.noise_emission),
                iconColor = Color(0xFFFF8F00)
            ) {
                NoiseEmissionContent(product = product)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Wet Grip (shown if wetGripClass is available — tyres)
        if (product.wetGripClass != null) {
            MetricCard(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.wet_grip),
                iconColor = Color(0xFF1565C0)
            ) {
                WetGripContent(gripClass = product.wetGripClass)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 5. Repairability (shown if repairabilityIndex is available)
        if (product.repairabilityIndex != null) {
            MetricCard(
                icon = Icons.Filled.Build,
                title = stringResource(R.string.repairability),
                iconColor = getRepairabilityColor(product.repairabilityIndex)
            ) {
                RepairabilityContent(repairabilityIndex = product.repairabilityIndex)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 6. Regulation Info (shown if implementingAct, onMarketStartYear, or productFicheUrl)
        val hasRegulationData = product.implementingAct != null ||
                product.onMarketStartYear != null || product.productFicheUrl != null
        if (hasRegulationData) {
            MetricCard(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.regulation_info),
                iconColor = Color(0xFF5E35B1)
            ) {
                RegulationInfoContent(product = product)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 7. Data Source (shown if sourceName or sourceUrl)
        val hasSourceData = product.sourceName != null || product.sourceUrl != null
        if (hasSourceData) {
            MetricCard(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.data_source),
                iconColor = Color(0xFF546E7A)
            ) {
                DataSourceContent(product = product)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 8. Product Identification (EAN barcode — always shown)
        ProductIdentificationCard(product = product)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ===== PRODUCT HEADER =====

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

            // Commercial name if different from product name
            product.commercialName?.let { commercial ->
                if (commercial != product.name) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = commercial,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(product.category.labelResId()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
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

// ===== ENERGY PERFORMANCE =====

@Composable
private fun EnergyPerformanceContent(
    product: Product,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            product.kwhPerYear?.let { kwh ->
                Text(
                    text = stringResource(R.string.kwh_per_year_value, kwh),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        EnergyClassBadge(
            energyClass = product.energyLabel,
            size = EnergyBadgeSize.LARGE,
            showScale = true
        )
    }
}

// ===== CONSUMPTION DETAILS =====

@Composable
private fun ConsumptionDetailsContent(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        product.energyEfficiencyIndex?.let { eei ->
            DetailRow(
                label = stringResource(R.string.energy_efficiency_index_label),
                value = formatDecimal(eei, minFractionDigits = 1, maxFractionDigits = 1)
            )
        }
        product.powerStandbyMode?.let { standby ->
            DetailRow(
                label = stringResource(R.string.power_standby_label),
                value = stringResource(R.string.power_standby_value, standby)
            )
        }
        product.powerOffMode?.let { off ->
            DetailRow(
                label = stringResource(R.string.power_off_label),
                value = stringResource(R.string.power_off_value, off)
            )
        }
    }
}

// ===== NOISE EMISSION =====

@Composable
private fun NoiseEmissionContent(
    product: Product,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            product.noiseDecibels?.let { db ->
                Text(
                    text = stringResource(R.string.noise_decibels_value, db),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = getNoiseColor(db)
                )
            }
        }

        product.noiseClass?.let { noiseClass ->
            ClassBadge(
                letter = noiseClass,
                color = getNoiseClassColor(noiseClass)
            )
        }
    }
}

// ===== WET GRIP =====

@Composable
private fun WetGripContent(
    gripClass: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.wet_grip_class_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ClassBadge(
            letter = gripClass,
            color = getGripClassColor(gripClass)
        )
    }
}

// ===== REPAIRABILITY =====

@Composable
private fun RepairabilityContent(
    repairabilityIndex: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val classLetter = when {
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

            Text(
                text = stringResource(
                    R.string.index_prefix,
                    formatDecimal(repairabilityIndex, minFractionDigits = 2, maxFractionDigits = 2)
                ),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = getRepairabilityColor(repairabilityIndex)
            )
        }

        RepairabilityBadge(
            score = repairabilityIndex,
            showLabel = false
        )
    }
}

// ===== REGULATION INFO =====

@Composable
private fun RegulationInfoContent(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        product.implementingAct?.let { act ->
            DetailRow(
                label = stringResource(R.string.implementing_act_label),
                value = act
            )
        }
        product.onMarketStartYear?.let { year ->
            DetailRow(
                label = stringResource(R.string.market_year_label),
                value = year.toString()
            )
        }
        product.productFicheUrl?.let {
            DetailRow(
                label = stringResource(R.string.product_fiche_label),
                value = stringResource(R.string.product_fiche_link),
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ===== DATA SOURCE =====

@Composable
private fun DataSourceContent(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        product.sourceName?.let { name ->
            DetailRow(
                label = stringResource(R.string.data_source),
                value = name
            )
        }
        product.sourceUrl?.let {
            DetailRow(
                label = "",
                value = stringResource(R.string.source_link),
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ===== PRODUCT IDENTIFICATION =====

@Composable
private fun ProductIdentificationCard(
    product: Product,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            DetailRow(
                label = stringResource(R.string.gtin_label),
                value = product.gtin
            )
        }
    }
}

// ===== REUSABLE COMPONENTS =====

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

/**
 * Reusable row for label + value pairs.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}

/**
 * Class badge (A-G) used for noise, wet grip, etc.
 */
@Composable
private fun ClassBadge(
    letter: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = WattsonCorners.Small
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
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
            val scoreLabel = when (score.letter) {
                "A" -> stringResource(R.string.excellent)
                "B" -> stringResource(R.string.good)
                "C" -> stringResource(R.string.fair)
                "D" -> stringResource(R.string.poor)
                "E" -> stringResource(R.string.very_poor)
                else -> score.label
            }
            Text(
                text = scoreLabel,
                style = MaterialTheme.typography.labelSmall,
                color = WattsonColors.OnSurfaceVariant
            )
        }
    }
}

// ===== UTILITY / STATE SCREENS =====

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
            text = stringResource(R.string.error),
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

// ===== EPREL DETAILS SECTION =====

/**
 * Mapping of EPREL JSON keys to user-friendly French labels.
 * Grouped by category section.
 */
private val EPREL_FIELD_LABELS: Map<String, Int> = mapOf(
    // Light source
    "lightingTechnology" to R.string.eprel_field_lighting_technology,
    "directional" to R.string.eprel_field_directional,
    "capType" to R.string.eprel_field_cap_type,
    "mains" to R.string.eprel_field_mains,
    "connectedLightSource" to R.string.eprel_field_connected_light_source,
    "colourTuneableLightSource" to R.string.eprel_field_colour_tuneable_light_source,
    "highLuminanceLightSource" to R.string.eprel_field_high_luminance_light_source,
    "antiGlareShield" to R.string.eprel_field_anti_glare_shield,
    "dimmable" to R.string.eprel_field_dimmable,
    "luminousFlux" to R.string.eprel_field_luminous_flux,
    "powerOnMode" to R.string.eprel_field_power_on_mode,
    "energyConsOnMode" to R.string.eprel_field_energy_cons_on_mode,
    "powerStandby" to R.string.eprel_field_power_standby,
    "powerStandbyNetworked" to R.string.eprel_field_power_standby_networked,
    "beamAngle" to R.string.eprel_field_beam_angle,
    "beamAngleCorrespondence" to R.string.eprel_field_beam_angle_correspondence,
    "peakLuminousIntensity" to R.string.eprel_field_peak_luminous_intensity,
    "correlatedColourTempMax" to R.string.eprel_field_correlated_colour_temp_max,
    "correlatedColourTempMin" to R.string.eprel_field_correlated_colour_temp_min,
    "colourRenderingIndex" to R.string.eprel_field_colour_rendering_index,
    "minColourRenderingIndex" to R.string.eprel_field_min_colour_rendering_index,
    "maxColourRenderingIndex" to R.string.eprel_field_max_colour_rendering_index,
    "r9ColourRenderingIndex" to R.string.eprel_field_r9_colour_rendering_index,
    "colourConsistency" to R.string.eprel_field_colour_consistency,
    "survivalFactor" to R.string.eprel_field_survival_factor,
    "lumenMaintenanceFactor" to R.string.eprel_field_lumen_maintenance_factor,
    "displacementFactor" to R.string.eprel_field_displacement_factor,
    "flickerMetric" to R.string.eprel_field_flicker_metric,
    "stroboscopicEffectMetric" to R.string.eprel_field_stroboscopic_effect_metric,
    "equivalentPower" to R.string.eprel_field_equivalent_power,
    "claimEquivalentPower" to R.string.eprel_field_claim_equivalent_power,
    "claimLedReplaceFluorescent" to R.string.eprel_field_claim_led_replace_fluorescent,
    "envelope" to R.string.eprel_field_envelope,
    // Dimensions
    "dimensionWidth" to R.string.eprel_field_dimension_width,
    "dimensionHeight" to R.string.eprel_field_dimension_height,
    "dimensionDepth" to R.string.eprel_field_dimension_depth,
    // Electronic display
    "diagonalCm" to R.string.eprel_field_diagonal_cm,
    "diagonalInch" to R.string.eprel_field_diagonal_inch,
    "panelTechnology" to R.string.eprel_field_panel_technology,
    "resolutionHorizontalPixels" to R.string.eprel_field_resolution_horizontal_pixels,
    "resolutionVerticalPixels" to R.string.eprel_field_resolution_vertical_pixels,
    "displayCategory" to R.string.eprel_field_display_category,
    "powerOnModeSDR" to R.string.eprel_field_power_on_mode_sdr,
    "energyClassHDR" to R.string.eprel_field_energy_class_hdr,
    // Washing machine / dishwasher
    "ratedCapacity" to R.string.eprel_field_rated_capacity,
    "spinDryingEfficiencyClass" to R.string.eprel_field_spin_drying_efficiency_class,
    "dryingEfficiencyClass" to R.string.eprel_field_drying_efficiency_class,
    "waterConsumption" to R.string.eprel_field_water_consumption,
    "programmeDurationRated" to R.string.eprel_field_programme_duration_rated,
    // Refrigerating appliance
    "totalVolume" to R.string.eprel_field_total_volume,
    "freezerVolume" to R.string.eprel_field_freezer_volume,
    "energyConsAnnual" to R.string.eprel_field_energy_cons_annual,
    "applianceType" to R.string.eprel_field_appliance_type,
    "cabinetFamilyCode" to R.string.eprel_field_cabinet_family_code,
    // Tyre
    "tyreDesignation" to R.string.eprel_field_tyre_designation,
    "tyreClass" to R.string.eprel_field_tyre_class,
    "severeSnowTyre" to R.string.eprel_field_severe_snow_tyre,
    "iceTyre" to R.string.eprel_field_ice_tyre,
    // General
    "guaranteeDuration" to R.string.eprel_field_guarantee_duration,
    "onMarketEndDate" to R.string.eprel_field_on_market_end_date,
    "supplierOrTrademark" to R.string.eprel_field_supplier_or_trademark,
    // Contact
    "contactDetails" to R.string.eprel_field_contact_details
)

/** Maps EPREL values to localized labels. */
@Composable
private fun formatEprelValue(key: String, value: Any?): String {
    if (value == null) return stringResource(R.string.eprel_value_not_available)
    return when {
        value is Boolean -> stringResource(
            if (value) R.string.eprel_value_yes else R.string.eprel_value_no
        )
        value is Number && value.toDouble() == value.toDouble().toLong().toDouble() ->
            NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())
        value is Number -> formatDecimal(value.toDouble(), maxFractionDigits = 2)
        key == "directional" -> when (value.toString()) {
            "DLS" -> stringResource(R.string.eprel_value_directional_directed)
            "NDLS" -> stringResource(R.string.eprel_value_directional_non_directed)
            else -> value.toString()
        }
        key == "mains" -> when (value.toString()) {
            "MLS" -> stringResource(R.string.eprel_value_mains_mains)
            "NMLS" -> stringResource(R.string.eprel_value_mains_non_mains)
            else -> value.toString()
        }
        key == "dimmable" -> when (value.toString()) {
            "NO" -> stringResource(R.string.eprel_value_no)
            "YES" -> stringResource(R.string.eprel_value_yes)
            "SPECIFIC" -> stringResource(R.string.eprel_value_specific)
            else -> value.toString()
        }
        key == "beamAngleCorrespondence" -> when (value.toString()) {
            "SPHERE_360" -> stringResource(R.string.eprel_value_beam_sphere)
            "WIDE_CONE_120" -> stringResource(R.string.eprel_value_beam_wide)
            "NARROW_CONE_90" -> stringResource(R.string.eprel_value_beam_narrow)
            else -> value.toString()
        }
        key == "applianceType" -> when (value.toString()) {
            "BEVERAGE_COOLER" -> stringResource(R.string.eprel_appliance_beverage_cooler)
            "WINE_STORAGE" -> stringResource(R.string.eprel_appliance_wine_storage)
            "REFRIGERATOR" -> stringResource(R.string.eprel_appliance_refrigerator)
            "FREEZER" -> stringResource(R.string.eprel_appliance_freezer)
            else -> value.toString().replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
        }
        value is Map<*, *> -> {
            val parts = mutableListOf<String>()
            (value["serviceName"] as? String)?.let { parts.add(it) }
            (value["email"] as? String)?.let { parts.add(it) }
            (value["phone"] as? String)?.let { parts.add(it) }
            (value["webSiteURL"] as? String)?.let { parts.add(it) }
            val address = buildString {
                (value["addressBloc"] as? String)?.let { append(it) }
                if (isEmpty()) {
                    val street = (value["street"] as? String).orEmpty()
                    val streetNumber = (value["streetNumber"] as? String).orEmpty()
                    val city = (value["city"] as? String)?.trim().orEmpty()
                    val postalCode = (value["postalCode"] as? String).orEmpty()
                    if (street.isNotBlank() || city.isNotBlank()) {
                        append(
                            stringResource(
                                R.string.eprel_contact_address_format,
                                street,
                                streetNumber,
                                postalCode,
                                city
                            )
                                .replace(Regex("\\s+,\\s+"), ", ")
                                .replace(Regex("\\s{2,}"), " ")
                                .trim()
                        )
                    }
                }
            }
            if (address.isNotBlank()) parts.add(address)
            parts.joinToString("\n")
        }
        value is List<*> -> {
            value.joinToString(", ") { item ->
                if (item is Map<*, *>) {
                    item.values.filterNotNull()
                        .filter { it.toString() != "null" }
                        .joinToString(" ")
                } else {
                    item.toString()
                }
            }
        }
        else -> value.toString()
    }
}

/**
 * Renders all EPREL detail fields grouped by category section.
 * Dynamically shows whatever fields are available from the EPREL API.
 */
@Composable
private fun EprelDetailsSection(
    productGroup: String?,
    details: Map<String, Any?>,
    modifier: Modifier = Modifier
) {
    val groupTitle = stringResource(getEprelGroupTitleResId(productGroup))
    val notAvailableValue = stringResource(R.string.eprel_value_not_available)

    MetricCard(
        icon = Icons.Filled.Info,
        title = stringResource(R.string.eprel_details_title, groupTitle),
        iconColor = Color(0xFF1976D2)
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Sort fields: known labels first, then unknown keys
            val sortedEntries = details.entries.sortedBy { entry ->
                if (EPREL_FIELD_LABELS.containsKey(entry.key)) 0 else 1
            }

            for ((key, value) in sortedEntries) {
                // Skip spectral images and internal objects
                if (key.contains("Image") || key.contains("image")) continue
                if (key.contains("spectral")) continue

                val label = EPREL_FIELD_LABELS[key]?.let { stringResource(it) }
                    ?: formatEprelFallbackLabel(key)

                val formattedValue = formatEprelValue(key, value)

                if (formattedValue.isNotBlank() && formattedValue != notAvailableValue) {
                    // For multi-line values (contactDetails), use a column layout
                    if (formattedValue.contains("\n")) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedValue,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        DetailRow(label = label, value = formattedValue)
                    }
                }
            }
        }
    }
}

// ===== COLOR HELPERS =====

@StringRes
private fun getEprelGroupTitleResId(productGroup: String?): Int {
    return when (productGroup) {
        "lightsources" -> R.string.eprel_group_light_source
        "electronicdisplays", "electronicdisplays20232766" -> R.string.eprel_group_electronic_display
        "washingmachines", "washingmachines2019" -> R.string.eprel_group_washing_machine
        "dishwashers2019" -> R.string.eprel_group_dishwasher
        "refrigeratingappliances", "refrigeratingappliances2019",
        "refrigeratingappliancesdirectsalesfunction" -> R.string.eprel_group_refrigerator
        "tumbledryers", "tumbledryers20232534" -> R.string.eprel_group_tumble_dryer
        "tyres" -> R.string.eprel_group_tyre
        "smartphonestablets20231669" -> R.string.eprel_group_smartphone_tablet
        "airconditioners" -> R.string.eprel_group_air_conditioner
        "spaceheaters", "localspaceheaters" -> R.string.eprel_group_space_heater
        "waterheaters" -> R.string.eprel_group_water_heater
        "ovens" -> R.string.eprel_group_oven
        "rangehoods" -> R.string.eprel_group_range_hood
        "washerdryers" -> R.string.eprel_group_washer_dryer
        else -> R.string.eprel_group_product_details
    }
}

private fun formatDecimal(
    value: Double,
    minFractionDigits: Int = 0,
    maxFractionDigits: Int = minFractionDigits
): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).run {
        minimumFractionDigits = minFractionDigits
        maximumFractionDigits = maxFractionDigits
        format(value)
    }
}

private fun formatEprelFallbackLabel(key: String): String {
    return key.replace(Regex("([A-Z])"), " $1")
        .trim()
        .replaceFirstChar { it.uppercase() }
}

private fun getNoiseColor(db: Int): Color {
    return when {
        db <= 50 -> Color(0xFF4CAF50) // Green - quiet
        db <= 65 -> Color(0xFFFFC107) // Amber - moderate
        db <= 75 -> Color(0xFFFF9800) // Orange - loud
        else -> Color(0xFFF44336)     // Red - very loud
    }
}

private fun getNoiseClassColor(noiseClass: String): Color {
    return when (noiseClass.uppercase()) {
        "A" -> Color(0xFF1B5E20)
        "B" -> Color(0xFF4CAF50)
        "C" -> Color(0xFFFFC107)
        "D" -> Color(0xFFFF9800)
        "E" -> Color(0xFFF44336)
        else -> Color(0xFF757575)
    }
}

private fun getGripClassColor(gripClass: String): Color {
    return when (gripClass.uppercase()) {
        "A" -> Color(0xFF1B5E20)
        "B" -> Color(0xFF4CAF50)
        "C" -> Color(0xFFFFC107)
        "D" -> Color(0xFFFF9800)
        "E" -> Color(0xFFF44336)
        else -> Color(0xFF757575)
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
                gtin = "4019238032055",
                name = "EcoContact 6",
                brand = "Continental",
                model = "0311050",
                category = ProductCategory.OTHER,
                commercialName = "EcoContact 6 195/65 R15",
                energyLabel = EnergyClass.A,
                noiseDecibels = 71,
                noiseClass = "B",
                wetGripClass = "A",
                implementingAct = "EU_2020_740",
                onMarketStartYear = 2020,
                productFicheUrl = "https://eprel.ec.europa.eu/",
                sourceName = "EPREL",
                sourceUrl = "https://eprel.ec.europa.eu/",
                updatedAt = Instant.now()
            ),
            metrics = null,
            globalScore = GlobalScore(numericValue = 75.0, letter = "B", label = "Bon")
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductDetailElectronicsPreview() {
    WattsonPreviewTheme {
        ProductDetailContent(
            product = Product(
                id = "prod_2",
                gtin = "0885909950805",
                name = "Apple iPhone 6",
                brand = "Apple",
                model = "iPhone 6",
                category = ProductCategory.TELEPHONIE,
                energyLabel = null,
                sourceName = "UPCitemdb",
                sourceUrl = "https://api.upcitemdb.com/",
                updatedAt = Instant.now()
            ),
            metrics = null,
            globalScore = null
        )
    }
}
