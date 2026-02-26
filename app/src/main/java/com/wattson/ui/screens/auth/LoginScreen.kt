package com.wattson.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wattson.R
import com.wattson.ui.components.TermsAndConditionsText
import com.wattson.ui.components.WattsonButton
import com.wattson.ui.components.WattsonOAuthButton
import com.wattson.ui.components.WattsonPageTitle
import com.wattson.ui.components.WattsonPasswordField
import com.wattson.ui.components.WattsonTextField
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonTheme
import kotlinx.coroutines.delay

/**
 * Login screen for email/password authentication.
 */
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRememberMe: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onLoginWithApple: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onAutoFillTestUser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FloatingGlowOverlay()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(500)) +
                            slideInVertically(initialOffsetY = { -it / 4 })
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        WattsonPageTitle(
                            title = stringResource(id = R.string.login_title),
                            subtitle = stringResource(id = R.string.login_subtitle)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Email field
                        WattsonTextField(
                            value = uiState.email,
                            onValueChange = onEmailChange,
                            label = stringResource(id = R.string.email),
                            placeholder = "test@wattson.com",
                            isError = uiState.emailError != null,
                            errorMessage = uiState.emailError,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Password field
                        WattsonPasswordField(
                            value = uiState.password,
                            onValueChange = onPasswordChange,
                            label = stringResource(id = R.string.password),
                            placeholder = "Password123!",
                            isError = uiState.passwordError != null,
                            errorMessage = uiState.passwordError,
                            imeAction = ImeAction.Done,
                            onImeAction = onLogin,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Remember me & Forgot password row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = uiState.rememberMe,
                                    onCheckedChange = onToggleRememberMe,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Text(
                                    text = stringResource(id = R.string.remember_me),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = stringResource(id = R.string.forgot_password),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onForgotPassword)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Login button — disabled if fields are empty or have validation errors
                        WattsonButton(
                            text = stringResource(id = R.string.continue_button),
                            onClick = onLogin,
                            isLoading = uiState.isLoading,
                            enabled = uiState.email.isNotBlank() &&
                                    uiState.password.isNotBlank() &&
                                    uiState.emailError == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // OAuth divider
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
                        
                        // OAuth buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WattsonOAuthButton(
                                text = stringResource(id = R.string.google),
                                icon = painterResource(id = R.drawable.ic_google),
                                onClick = onLoginWithGoogle,
                                modifier = Modifier.weight(1f)
                            )
                            
                            WattsonOAuthButton(
                                text = stringResource(id = R.string.apple),
                                icon = painterResource(id = R.drawable.ic_apple),
                                tint = MaterialTheme.colorScheme.onSurface,
                                onClick = onLoginWithApple,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Legal text
                        TermsAndConditionsText()
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Back button
                        WattsonButton(
                            text = stringResource(id = R.string.back),
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Register link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_account),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.sign_up),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onNavigateToRegister)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingGlowOverlay() {
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
private fun LoginScreenPreview() {
    WattsonTheme {
        LoginScreen(
            uiState = AuthUiState(
                email = "test@example.com"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleRememberMe = {},
            onLogin = {},
            onForgotPassword = {},
            onLoginWithGoogle = {},
            onLoginWithApple = {},
            onNavigateBack = {},
            onNavigateToRegister = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenWithErrorPreview() {
    WattsonTheme {
        LoginScreen(
            uiState = AuthUiState(
                email = "invalid-email",
                emailError = "Invalid email format",
                password = "123",
                passwordError = "Password must be at least 8 characters"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleRememberMe = {},
            onLogin = {},
            onForgotPassword = {},
            onLoginWithGoogle = {},
            onLoginWithApple = {},
            onNavigateBack = {},
            onNavigateToRegister = {}
        )
    }
}
