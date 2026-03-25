package com.wattson.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wattson.R
import com.wattson.ui.components.WattsonButton
import com.wattson.ui.components.WattsonOAuthButton
import com.wattson.ui.components.TermsAndConditionsText
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonTheme
import kotlinx.coroutines.delay

/**
 * Main authentication entry screen.
 * Displays the Wattson logo and authentication options.
 */
@Composable
fun AuthScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    AuthScreen(
        uiState = AuthUiState(),
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToRegister = onNavigateToRegister,
        onLoginWithGoogle = onLoginWithGoogle,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Animation states for staggered entrance
    var showLogo by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    var showOAuth by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }
    
    // Logo scale animation
    val logoScale by animateFloatAsState(
        targetValue = if (showLogo) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    
    // Trigger staggered animations
    LaunchedEffect(Unit) {
        delay(100)
        showLogo = true
        delay(400)
        showButtons = true
        delay(150)
        showOAuth = true
        delay(150)
        showLegal = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FloatingGlowBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                // Logo with animation
                AnimatedVisibility(
                    visible = showLogo,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    WattsonLogo(
                        modifier = Modifier.scale(logoScale)
                    )
                }
                
                Spacer(modifier = Modifier.height(60.dp))

                // Main action buttons
                AnimatedVisibility(
                    visible = showButtons,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(initialOffsetY = { it / 3 })
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WattsonButton(
                            text = stringResource(id = R.string.sign_in),
                            onClick = onNavigateToLogin,
                            isLoading = uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        WattsonButton(
                            text = stringResource(id = R.string.sign_up),
                            onClick = onNavigateToRegister,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // OAuth buttons section
                AnimatedVisibility(
                    visible = showOAuth,
                    enter = fadeIn(animationSpec = tween(400))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Divider with "ou" text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = stringResource(id = R.string.or_divider),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // OAuth provider button
                        WattsonOAuthButton(
                            text = stringResource(id = R.string.sign_in_with_google),
                            icon = painterResource(id = R.drawable.ic_google),
                            onClick = onLoginWithGoogle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Legal text
                AnimatedVisibility(
                    visible = showLegal,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    TermsAndConditionsText()
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Wattson logo composable with custom styling.
 */
@Composable
private fun WattsonLogo(modifier: Modifier = Modifier) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            WattsonColors.Primary,
            WattsonColors.PrimaryLight,
            WattsonColors.Primary
        )
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.wattson_logo),
            contentDescription = "Wattson Logo",
            modifier = Modifier
                .size(64.dp)
                .offset(x = 8.dp)
        )
        
        Text(
            text = "attson",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                brush = gradientBrush
            ),
            modifier = Modifier.offset(x = (-6).dp)
        )
    }
}

/**
 * Legal disclaimer text with GDPR compliance mention.
 */
@Composable
private fun FloatingGlowBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val isDark = MaterialTheme.colorScheme.surface == WattsonColors.SurfaceDark
    val baseBackground = MaterialTheme.colorScheme.background

    val glow1X by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow1X"
    )

    val glow1Y by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow1Y"
    )

    val glow2X by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow2X"
    )

    val glow2Y by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow2Y"
    )

    val glow3X by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow3X"
    )

    val glow3Y by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow3Y"
    )

    val glow4X by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow4X"
    )

    val glow4Y by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow4Y"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawRect(baseBackground)

        val cyanLight = if (isDark) Color(0xFF006064) else Color(0xFF4DD0E1)
        val violetLight = if (isDark) Color(0xFF311B92) else Color(0xFF7E57C2)
        val tealLight = if (isDark) Color(0xFF004D40) else Color(0xFF26A69A)
        val blueLight = if (isDark) Color(0xFF0D47A1) else Color(0xFF42A5F5)

        val alphaMultiplier = if (isDark) 0.15f else 1f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    cyanLight.copy(alpha = pulseAlpha * 0.18f * alphaMultiplier),
                    cyanLight.copy(alpha = pulseAlpha * 0.06f * alphaMultiplier),
                    Color.Transparent
                ),
                center = Offset(size.width * glow1X, size.height * glow1Y),
                radius = size.width * 0.55f
            ),
            center = Offset(size.width * glow1X, size.height * glow1Y),
            radius = size.width * 0.55f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    violetLight.copy(alpha = pulseAlpha * 0.15f * alphaMultiplier),
                    violetLight.copy(alpha = pulseAlpha * 0.05f * alphaMultiplier),
                    Color.Transparent
                ),
                center = Offset(size.width * glow2X, size.height * glow2Y),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * glow2X, size.height * glow2Y),
            radius = size.width * 0.5f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tealLight.copy(alpha = pulseAlpha * 0.16f * alphaMultiplier),
                    tealLight.copy(alpha = pulseAlpha * 0.05f * alphaMultiplier),
                    Color.Transparent
                ),
                center = Offset(size.width * glow3X, size.height * glow3Y),
                radius = size.width * 0.45f
            ),
            center = Offset(size.width * glow3X, size.height * glow3Y),
            radius = size.width * 0.45f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    blueLight.copy(alpha = pulseAlpha * 0.14f * alphaMultiplier),
                    blueLight.copy(alpha = pulseAlpha * 0.04f * alphaMultiplier),
                    Color.Transparent
                ),
                center = Offset(size.width * glow4X, size.height * glow4Y),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * glow4X, size.height * glow4Y),
            radius = size.width * 0.5f
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AuthScreenPreview() {
    WattsonTheme {
        AuthScreen(
            uiState = AuthUiState(),
            onNavigateToLogin = {},
            onNavigateToRegister = {},
            onLoginWithGoogle = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
