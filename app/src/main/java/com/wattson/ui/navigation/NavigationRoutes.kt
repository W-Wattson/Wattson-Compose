package com.wattson.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the Wattson app across auth, main tabs, and detail screens.
 *
 * Hierarchy:
 * - Level 0: Auth (Login/Register)
 * - Level 1: Main tabs (History, Repair, Scan, Documents, Account)
 * - Level 2: Detail screens (ProductDetail, Premium)
 */
sealed interface WattsonRoute {
    
    // ===== AUTHENTICATION (Level 0) =====
    
    /**
     * Initial authentication screen with options
     */
    @Serializable
    data object Auth : WattsonRoute
    
    /**
     * Login screen with email/password
     */
    @Serializable
    data object Login : WattsonRoute
    
    /**
     * Registration screen
     */
    @Serializable
    data object Register : WattsonRoute
    
    // ===== MAIN TABS (Level 1) =====
    
    /**
     * History tab - List of scanned products
     */
    @Serializable
    data object History : WattsonRoute
    
    /**
     * Repair tab - Repair assistance
     */
    @Serializable
    data object Repair : WattsonRoute
    
    /**
     * Scan screen - Camera barcode scanner
     */
    @Serializable
    data object Scan : WattsonRoute
    
    /**
     * Documents tab - Conciergerie
     */
    @Serializable
    data object Documents : WattsonRoute
    
    /**
     * Account tab - User profile and settings
     */
    @Serializable
    data object Account : WattsonRoute
    
    // ===== DETAIL SCREENS (Level 2) =====
    
    /**
     * Product detail screen
     * @param productId The product ID or GTIN
     */
    @Serializable
    data class ProductDetail(val productId: String) : WattsonRoute
    
    /**
     * Premium subscription screen
     */
    @Serializable
    data object Premium : WattsonRoute
    
    /**
     * Document detail/viewer screen
     * @param documentId The document ID
     */
    @Serializable
    data class DocumentDetail(val documentId: String) : WattsonRoute
}

/**
 * Bottom navigation items for the main tab bar.
 */
enum class BottomNavItem(
    val route: WattsonRoute,
    val labelRes: Int, // String resource ID
    val iconRes: Int,  // Drawable resource ID
    val contentDescription: String
) {
    HISTORY(
        route = WattsonRoute.History,
        labelRes = 0, // Will be replaced with actual resource
        iconRes = 0,
        contentDescription = "Historique"
    ),
    REPAIR(
        route = WattsonRoute.Repair,
        labelRes = 0,
        iconRes = 0,
        contentDescription = "Réparation"
    ),
    DOCUMENTS(
        route = WattsonRoute.Documents,
        labelRes = 0,
        iconRes = 0,
        contentDescription = "Documents"
    ),
    ACCOUNT(
        route = WattsonRoute.Account,
        labelRes = 0,
        iconRes = 0,
        contentDescription = "Compte"
    )
}

/**
 * Navigation graph names
 */
object NavGraphs {
    const val AUTH = "auth_graph"
    const val MAIN = "main_graph"
}

/**
 * Check if route is a main tab route
 */
fun WattsonRoute.isMainTab(): Boolean {
    return this is WattsonRoute.History ||
            this is WattsonRoute.Repair ||
            this is WattsonRoute.Documents ||
            this is WattsonRoute.Account
}

/**
 * Check if route requires authentication
 */
fun WattsonRoute.requiresAuth(): Boolean {
    return this !is WattsonRoute.Auth &&
            this !is WattsonRoute.Login &&
            this !is WattsonRoute.Register
}
