package com.wattson.ui.screens.premium

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.SubscriptionType
import com.wattson.ui.components.WattsonButton
import com.wattson.ui.components.WattsonPageTitle
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Premium subscription screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    uiState: PremiumUiState,
    events: SharedFlow<PremiumEvent>,
    onIntent: (PremiumIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is PremiumEvent.NavigateBack -> onNavigateBack()
                is PremiumEvent.SubscriptionSuccess -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.premium_success))
                }
                is PremiumEvent.ShowError -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_prefix, event.message))
                }
                is PremiumEvent.OpenPaymentSheet -> { /* Handle payment sheet */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(PremiumIntent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = WattsonColors.Primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                else -> {
                    PremiumContent(
                        plans = uiState.availablePlans,
                        selectedPlan = uiState.selectedPlan,
                        currentSubscription = uiState.currentSubscription,
                        isProcessingPayment = uiState.isProcessingPayment,
                        onPlanSelected = { onIntent(PremiumIntent.SelectPlan(it)) },
                        onConfirmSubscription = { onIntent(PremiumIntent.ConfirmSubscription) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumContent(
    plans: List<SubscriptionPlan>,
    selectedPlan: SubscriptionPlan?,
    currentSubscription: SubscriptionType,
    isProcessingPayment: Boolean,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    onConfirmSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WattsonPageTitle(
            title = stringResource(R.string.go_premium_title),
            subtitle = stringResource(R.string.go_premium_subtitle),
            accentColor = WattsonColors.PremiumGradientStart
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Plan cards
        plans.forEach { plan ->
            val isSelected = selectedPlan == plan
            val isCurrentPlan = currentSubscription == plan.type
            
            PlanCard(
                plan = plan,
                isSelected = isSelected,
                isCurrentPlan = isCurrentPlan,
                onClick = { if (!isCurrentPlan) onPlanSelected(plan) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Subscribe button
        if (selectedPlan != null && currentSubscription != selectedPlan.type) {
            WattsonButton(
                text = if (isProcessingPayment) stringResource(R.string.processing) else stringResource(R.string.select),
                onClick = onConfirmSubscription,
                isLoading = isProcessingPayment,
                enabled = !isProcessingPayment,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms
        Text(
            text = "En souscrivant, vous acceptez nos conditions d'utilisation et notre politique de confidentialité. L'abonnement se renouvelle automatiquement.",
            style = MaterialTheme.typography.labelSmall,
            color = WattsonColors.OnSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrentPlan -> WattsonColors.Success
            isSelected -> WattsonColors.Primary
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "borderColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isCurrentPlan) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(
                        width = 2.dp,
                        color = borderColor,
                        shape = WattsonCorners.Card
                    )
                } else Modifier
            )
            .clickable(enabled = !isCurrentPlan, onClick = onClick),
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && !isCurrentPlan) 
                WattsonColors.Primary.copy(alpha = 0.05f) 
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header row with popular badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wattson logo placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    WattsonColors.PremiumGradientStart,
                                    WattsonColors.PremiumGradientEnd
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.wattson_logo),
                        contentDescription = "Wattson Logo",
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (plan.isPopular) {
                    PopularBadge()
                }

                if (isCurrentPlan) {
                    CurrentPlanBadge()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price
            Text(
                text = plan.price,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            plan.features.forEach { feature ->
                FeatureItem(text = feature)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PopularBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        WattsonColors.PremiumGradientStart,
                        WattsonColors.PremiumGradientEnd
                    )
                ),
                shape = WattsonCorners.Small
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Populaire",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun CurrentPlanBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = WattsonColors.Success.copy(alpha = 0.1f),
                shape = WattsonCorners.Small
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Plan actuel",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = WattsonColors.Success
        )
    }
}

@Composable
private fun FeatureItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(WattsonColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = WattsonColors.Primary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = WattsonColors.OnSurfaceVariant
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

// ===== PREVIEWS =====

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PremiumContentPreview() {
    WattsonPreviewTheme {
        PremiumContent(
            plans = listOf(
                SubscriptionPlan(
                    type = SubscriptionType.PREMIUM,
                    name = "Premium",
                    price = "5,99 €/mois",
                    priceValue = 5.99,
                    features = listOf(
                        "Conciergerie 15 documents",
                        "Conseil de réparabilité basique",
                        "Historique illimité"
                    )
                ),
                SubscriptionPlan(
                    type = SubscriptionType.PREMIUM_UNLIMITED,
                    name = "Premium Illimité",
                    price = "9,95 €/mois",
                    priceValue = 9.95,
                    features = listOf(
                        "Conciergerie 1000 documents",
                        "Conseil de réparabilité avancé",
                        "Historique illimité",
                        "Support prioritaire"
                    ),
                    isPopular = true
                )
            ),
            selectedPlan = SubscriptionPlan(
                type = SubscriptionType.PREMIUM_UNLIMITED,
                name = "Premium Illimité",
                price = "9,95 €/mois",
                priceValue = 9.95,
                features = listOf(),
                isPopular = true
            ),
            currentSubscription = SubscriptionType.FREE,
            isProcessingPayment = false,
            onPlanSelected = {},
            onConfirmSubscription = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanCardPreview() {
    WattsonPreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PlanCard(
                plan = SubscriptionPlan(
                    type = SubscriptionType.PREMIUM_UNLIMITED,
                    name = "Premium Illimité",
                    price = "9,95 €/mois",
                    priceValue = 9.95,
                    features = listOf(
                        "Conciergerie 1000 documents",
                        "Conseil de réparabilité avancé"
                    ),
                    isPopular = true
                ),
                isSelected = true,
                isCurrentPlan = false,
                onClick = {}
            )
        }
    }
}
