package com.wattson.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Wattson Light Color Scheme
 * Based on the brand guidelines: Primary #16B4BD, Secondary #191A23
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = WattsonColors.Primary,
    onPrimary = WattsonColors.White,
    primaryContainer = WattsonColors.PrimaryContainer,
    onPrimaryContainer = WattsonColors.OnPrimaryContainer,

    // Secondary colors
    secondary = WattsonColors.Secondary,
    onSecondary = WattsonColors.White,
    secondaryContainer = WattsonColors.SecondaryContainer,
    onSecondaryContainer = WattsonColors.OnSecondaryContainer,

    // Tertiary colors (using Primary variant)
    tertiary = WattsonColors.PrimaryDark,
    onTertiary = WattsonColors.White,
    tertiaryContainer = WattsonColors.PrimaryContainer,
    onTertiaryContainer = WattsonColors.OnPrimaryContainer,

    // Background and surface
    background = WattsonColors.Background,
    onBackground = WattsonColors.OnBackground,
    surface = WattsonColors.Surface,
    onSurface = WattsonColors.OnSurface,
    surfaceVariant = WattsonColors.SurfaceVariant,
    onSurfaceVariant = WattsonColors.OnSurfaceVariant,

    // Error colors
    error = WattsonColors.Error,
    onError = WattsonColors.White,
    errorContainer = WattsonColors.Error.copy(alpha = 0.1f),
    onErrorContainer = WattsonColors.Error,

    // Outline
    outline = WattsonColors.Outline,
    outlineVariant = WattsonColors.OutlineVariant,

    // Inverse colors
    inverseSurface = WattsonColors.Secondary,
    inverseOnSurface = WattsonColors.OnBackgroundDark,
    inversePrimary = WattsonColors.PrimaryLight,

    // Scrim
    scrim = WattsonColors.Black.copy(alpha = 0.32f)
)

/**
 * Wattson Dark Color Scheme
 * Adapted for dark mode while maintaining brand identity
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = WattsonColors.PrimaryLight,
    onPrimary = WattsonColors.OnPrimaryContainer,
    primaryContainer = WattsonColors.PrimaryDark,
    onPrimaryContainer = WattsonColors.PrimaryContainer,

    // Secondary colors
    secondary = WattsonColors.SecondaryLight,
    onSecondary = WattsonColors.White,
    secondaryContainer = WattsonColors.Secondary,
    onSecondaryContainer = WattsonColors.OnSecondaryContainer,

    // Tertiary colors
    tertiary = WattsonColors.Primary,
    onTertiary = WattsonColors.OnPrimaryContainer,
    tertiaryContainer = WattsonColors.PrimaryDark,
    onTertiaryContainer = WattsonColors.PrimaryContainer,

    // Background and surface
    background = WattsonColors.BackgroundDark,
    onBackground = WattsonColors.OnBackgroundDark,
    surface = WattsonColors.SurfaceDark,
    onSurface = WattsonColors.OnSurfaceDark,
    surfaceVariant = WattsonColors.SurfaceVariantDark,
    onSurfaceVariant = WattsonColors.OnSurfaceVariant,

    // Error colors
    error = WattsonColors.Error,
    onError = WattsonColors.White,
    errorContainer = WattsonColors.Error.copy(alpha = 0.2f),
    onErrorContainer = WattsonColors.Error,

    // Outline
    outline = WattsonColors.OutlineDark,
    outlineVariant = WattsonColors.SecondaryContainer,

    // Inverse colors
    inverseSurface = WattsonColors.Surface,
    inverseOnSurface = WattsonColors.OnSurface,
    inversePrimary = WattsonColors.Primary,

    // Scrim
    scrim = WattsonColors.Black.copy(alpha = 0.5f)
)

/**
 * Main Wattson Theme composable
 * 
 * @param darkTheme Whether to use dark theme, defaults to system setting
 * @param dynamicColor Whether to use dynamic colors (Android 12+), disabled by default
 *                     to maintain brand consistency
 * @param content The content to be themed
 */
@Composable
fun WattsonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to maintain brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WattsonTypography,
        shapes = WattsonShapes,
        content = content
    )
}

/**
 * Preview-only theme for Compose previews
 * Does not modify system UI
 */
@Composable
fun WattsonPreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WattsonTypography,
        shapes = WattsonShapes,
        content = content
    )
}
