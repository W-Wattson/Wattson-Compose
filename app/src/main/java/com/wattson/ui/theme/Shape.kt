package com.wattson.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Wattson Shape System
 * Defines consistent corner radii throughout the application
 * Based on modern Material 3 design principles
 */
val WattsonShapes = Shapes(
    // Extra small - for chips, badges, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for text fields, small cards
    small = RoundedCornerShape(8.dp),
    
    // Medium - for cards, buttons, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - for bottom sheets, large cards
    large = RoundedCornerShape(16.dp),
    
    // Extra large - for modal sheets, containers
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Custom shape constants for specific use cases
 */
object WattsonCorners {
    val None = RoundedCornerShape(0.dp)
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    val Full = RoundedCornerShape(50)
    
    // Button shapes
    val Button = RoundedCornerShape(12.dp)
    val ButtonSmall = RoundedCornerShape(8.dp)
    val ButtonFull = RoundedCornerShape(50)
    
    // Card shapes
    val Card = RoundedCornerShape(16.dp)
    val CardSmall = RoundedCornerShape(12.dp)
    
    // Input shapes
    val TextField = RoundedCornerShape(12.dp)
    val SearchBar = RoundedCornerShape(24.dp)
    
    // Bottom navigation
    val BottomBar = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    
    // Bottom sheet
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    
    // Energy badge
    val EnergyBadge = RoundedCornerShape(4.dp)
    
    // Tag/Chip
    val Tag = RoundedCornerShape(6.dp)
    
    // FAB (Floating Action Button)
    val Fab = RoundedCornerShape(16.dp)
    val FabExtended = RoundedCornerShape(16.dp)
}
