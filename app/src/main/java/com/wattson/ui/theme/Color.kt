package com.wattson.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Wattson color palette following brand guidelines.
 *
 * Primary: #16B4BD (Teal) - CTAs, links, accents
 * Secondary: #191A23 (Dark) - Texts, titles
 * Neutral: #F3F3F3 (Light Gray) - Backgrounds, alternance
 */
object WattsonColors {
    // Primary Brand Colors
    val Primary = Color(0xFF16B4BD)
    val PrimaryLight = Color(0xFF4DD4DB)
    val PrimaryDark = Color(0xFF0D8A91)
    val PrimaryContainer = Color(0xFFE0F7F8)
    val OnPrimaryContainer = Color(0xFF002022)

    // Secondary Brand Colors
    val Secondary = Color(0xFF191A23)
    val SecondaryLight = Color(0xFF2E2F3D)
    val SecondaryDark = Color(0xFF0D0E14)
    val SecondaryContainer = Color(0xFF3D3E4D)
    val OnSecondaryContainer = Color(0xFFE8E8EC)

    // Neutral Colors
    val Background = Color(0xFFF3F3F3)
    val BackgroundDark = Color(0xFF121218)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1C1C22)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val SurfaceVariantDark = Color(0xFF2A2A32)

    // Text Colors
    val OnBackground = Color(0xFF191A23)
    val OnBackgroundDark = Color(0xFFE8E8EC)
    val OnSurface = Color(0xFF191A23)
    val OnSurfaceDark = Color(0xFFE8E8EC)
    val OnSurfaceVariant = Color(0xFF6B6B7A)

    // Energy Class Colors (A to G)
    val EnergyClassA = Color(0xFF00A651)
    val EnergyClassB = Color(0xFF50B848)
    val EnergyClassC = Color(0xFFBED630)
    val EnergyClassD = Color(0xFFFFF200)
    val EnergyClassE = Color(0xFFFDB913)
    val EnergyClassF = Color(0xFFF7941D)
    val EnergyClassG = Color(0xFFED1C24)

    // Repairability Score Colors
    val RepairabilityHigh = Color(0xFF00A651)
    val RepairabilityMedium = Color(0xFFFDB913)
    val RepairabilityLow = Color(0xFFED1C24)

    // Status Colors
    val Success = Color(0xFF00A651)
    val Warning = Color(0xFFFDB913)
    val Error = Color(0xFFED1C24)
    val Info = Color(0xFF16B4BD)

    // Outline Colors
    val Outline = Color(0xFFDDDDDD)
    val OutlineDark = Color(0xFF3D3E4D)
    val OutlineVariant = Color(0xFFEEEEEE)

    // Gradient Colors for Premium
    val GradientStart = Color(0xFF16B4BD)
    val GradientEnd = Color(0xFF0D8A91)

    // Tag Colors
    val TagFacture = Color(0xFFFF6B6B)
    val TagGarantie = Color(0xFF16B4BD)
    
    // Legacy aliases for backward compatibility
    val WattsonPrimary = Primary
    val WattsonSecondary = Secondary
    val EcologyGreen = EnergyClassA
    val EconomyBlue = Info
    val RepairabilityOrange = Warning
    val PremiumGradientStart = GradientStart
    val PremiumGradientEnd = GradientEnd

    // White and Black
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}

/**
 * Returns the appropriate color for an energy class label (A to G)
 */
fun getEnergyClassColor(energyClass: String): Color {
    return when (energyClass.uppercase()) {
        "A" -> WattsonColors.EnergyClassA
        "B" -> WattsonColors.EnergyClassB
        "C" -> WattsonColors.EnergyClassC
        "D" -> WattsonColors.EnergyClassD
        "E" -> WattsonColors.EnergyClassE
        "F" -> WattsonColors.EnergyClassF
        "G" -> WattsonColors.EnergyClassG
        else -> WattsonColors.OnSurfaceVariant
    }
}

/**
 * Returns the appropriate color for a repairability score (0-10)
 */
fun getRepairabilityColor(score: Double): Color {
    return when {
        score >= 7.0 -> WattsonColors.RepairabilityHigh
        score >= 4.0 -> WattsonColors.RepairabilityMedium
        else -> WattsonColors.RepairabilityLow
    }
}
