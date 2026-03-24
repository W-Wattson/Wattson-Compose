package com.wattson.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import com.wattson.ui.i18n.asString
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonTheme
import kotlinx.coroutines.delay

/**
 * Registration screen for new user account creation.
 */
@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegister: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
            FloatingGlowBackground()

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
                        // Logo
                        RegisterLogo()
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        // Title and subtitle
                        WattsonPageTitle(
                            title = stringResource(id = R.string.register_title),
                            subtitle = stringResource(id = R.string.register_subtitle)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Email field
                        WattsonTextField(
                            value = uiState.email,
                            onValueChange = onEmailChange,
                            label = stringResource(id = R.string.email),
                            placeholder = stringResource(R.string.placeholder_email_example),
                            isError = uiState.emailError != null,
                            errorMessage = uiState.emailError?.asString(),
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
                            placeholder = stringResource(id = R.string.password),
                            isError = uiState.passwordError != null,
                            errorMessage = uiState.passwordError?.asString(),
                            imeAction = ImeAction.Next,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Confirm password field
                        WattsonPasswordField(
                            value = uiState.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = stringResource(id = R.string.confirm_password),
                            placeholder = stringResource(id = R.string.confirm_password),
                            isError = uiState.confirmPasswordError != null,
                            errorMessage = uiState.confirmPasswordError?.asString(),
                            imeAction = ImeAction.Done,
                            onImeAction = onRegister,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Password requirements hint
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        PasswordRequirementsHint(
                            password = uiState.password,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Register button
                        WattsonButton(
                            text = stringResource(id = R.string.continue_button),
                            onClick = onRegister,
                            isLoading = uiState.isLoading,
                            enabled = isFormValid(uiState),
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
                        
                        // OAuth button
                        WattsonOAuthButton(
                            text = stringResource(id = R.string.sign_in_with_google),
                            icon = painterResource(id = R.drawable.ic_google),
                            onClick = onLoginWithGoogle,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
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
                        
                        // Login link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.already_have_account),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.sign_in),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onNavigateToLogin)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Password requirements indicator with strength bar and requirement chips.
 * Provides real-time visual feedback as the user types.
 */
@Composable
private fun PasswordRequirementsHint(
    password: String,
    modifier: Modifier = Modifier
) {
    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    // Calculate strength score (0-4)
    val strengthScore = listOf(hasMinLength, hasUppercase, hasDigit, hasSpecial).count { it }

    // Strength label and color
    val strengthLabel = when {
        password.isBlank() -> ""
        strengthScore <= 1 -> stringResource(R.string.password_strength_weak)
        strengthScore == 2 -> stringResource(R.string.password_strength_medium)
        strengthScore == 3 -> stringResource(R.string.password_strength_good)
        else -> stringResource(R.string.password_strength_strong)
    }

    val strengthColor = when {
        password.isBlank() -> MaterialTheme.colorScheme.outlineVariant
        strengthScore <= 1 -> Color(0xFFE53935) // Red
        strengthScore == 2 -> Color(0xFFFFA726) // Orange
        strengthScore == 3 -> Color(0xFFFDD835) // Yellow
        else -> Color(0xFF66BB6A) // Green
    }

    // Animated strength progress
    val animatedProgress by animateFloatAsState(
        targetValue = if (password.isBlank()) 0f else strengthScore / 4f,
        animationSpec = tween(300),
        label = "strengthProgress"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Strength bar
        if (password.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress bar segments
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    color = if (index < strengthScore) strengthColor
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Strength label
                Text(
                    text = strengthLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = strengthColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
        }

        // Requirements chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PasswordRequirementChip(
                text = stringResource(id = R.string.req_length),
                isMet = hasMinLength
            )
            PasswordRequirementChip(
                text = stringResource(id = R.string.req_uppercase),
                isMet = hasUppercase
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PasswordRequirementChip(
                text = stringResource(id = R.string.req_digit),
                isMet = hasDigit
            )
            PasswordRequirementChip(
                text = stringResource(id = R.string.req_special),
                isMet = hasSpecial
            )
        }
    }
}

@Composable
private fun PasswordRequirementChip(
    text: String,
    isMet: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isMet) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isMet) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isMet) {
                    stringResource(R.string.password_requirement_met_symbol)
                } else {
                    stringResource(R.string.password_requirement_pending_symbol)
                },
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

@Composable
private fun RegisterLogo(modifier: Modifier = Modifier) {
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
            contentDescription = stringResource(R.string.logo_content_description),
            modifier = Modifier
                .size(48.dp)
                .offset(x = 6.dp)
        )
        
        Text(
            text = stringResource(R.string.app_name_tail),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                brush = gradientBrush
            ),
            modifier = Modifier.offset(x = (-4).dp)
        )
    }
}

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

private fun isFormValid(uiState: AuthUiState): Boolean {
    return uiState.email.isNotBlank() &&
            uiState.password.isNotBlank() &&
            uiState.confirmPassword.isNotBlank() &&
            uiState.emailError == null &&
            uiState.passwordError == null &&
            uiState.confirmPasswordError == null
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    WattsonTheme {
        RegisterScreen(
            uiState = AuthUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onRegister = {},
            onLoginWithGoogle = {},
            onNavigateBack = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenFilledPreview() {
    WattsonTheme {
        RegisterScreen(
            uiState = AuthUiState(
                email = "user@example.com",
                password = "MyPassword1!",
                confirmPassword = "MyPassword1!"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onRegister = {},
            onLoginWithGoogle = {},
            onNavigateBack = {},
            onNavigateToLogin = {}
        )
    }
}
