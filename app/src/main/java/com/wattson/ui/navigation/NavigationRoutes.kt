package com.wattson.ui.navigation

import com.wattson.R
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
     * Initial authentication screen with options.
     */
    @Serializable
    data object Auth : WattsonRoute

    /**
     * Login screen with email/password.
     */
    @Serializable
    data object Login : WattsonRoute

    /**
     * Registration screen.
     */
    @Serializable
    data object Register : WattsonRoute

    // ===== MAIN TABS (Level 1) =====

    /**
     * History tab showing scanned products.
     */
    @Serializable
    data object History : WattsonRoute

    /**
     * Repair tab for the repair assistant entry point.
     */
    @Serializable
    data object Repair : WattsonRoute

    /**
     * Camera-based scan screen.
     */
    @Serializable
    data object Scan : WattsonRoute

    /**
     * Documents tab for the concierge feature.
     */
    @Serializable
    data object Documents : WattsonRoute

    /**
     * Account tab for user profile and settings.
     */
    @Serializable
    data object Account : WattsonRoute

    // ===== DETAIL SCREENS (Level 2) =====

    /**
     * Product detail screen.
     *
     * @param productId Product identifier or GTIN.
     */
    @Serializable
    data class ProductDetail(val productId: String) : WattsonRoute

    /**
     * Premium subscription screen.
     */
    @Serializable
    data object Premium : WattsonRoute

    /**
     * Document detail and preview screen.
     *
     * @param documentId Document identifier.
     */
    @Serializable
    data class DocumentDetail(val documentId: String) : WattsonRoute

    /**
     * Repair chat screen.
     *
     * @param conversationId Conversation identifier.
     */
    @Serializable
    data class RepairChat(val conversationId: String) : WattsonRoute
}

/**
 * Maps the current generated route string to the bottom bar destination it represents.
 *
 * Typed navigation encodes destinations as generated route strings. The UI only needs marker-based
 * matching for top-level tabs, so this helper centralizes the mapping instead of repeating
 * `contains(...)` checks across multiple files.
 */
fun String?.toBottomBarRoute(): WattsonRoute? {
    if (this == null) {
        return null
    }

    return when {
        contains("History") -> WattsonRoute.History
        contains("Repair") -> WattsonRoute.Repair
        contains("Documents") -> WattsonRoute.Documents
        contains("Account") -> WattsonRoute.Account
        else -> null
    }
}

enum class BottomNavItem(
    val route: WattsonRoute,
    val labelRes: Int, // String resource ID
    val iconRes: Int,  // Drawable resource ID
    val contentDescriptionRes: Int
) {
    HISTORY(
        route = WattsonRoute.History,
        labelRes = R.string.nav_history,
        iconRes = 0,
        contentDescriptionRes = R.string.nav_history_desc
    ),
    REPAIR(
        route = WattsonRoute.Repair,
        labelRes = R.string.nav_repair,
        iconRes = 0,
        contentDescriptionRes = R.string.nav_repair_desc
    ),
    DOCUMENTS(
        route = WattsonRoute.Documents,
        labelRes = R.string.nav_documents,
        iconRes = 0,
        contentDescriptionRes = R.string.nav_documents_desc
    ),
    ACCOUNT(
        route = WattsonRoute.Account,
        labelRes = R.string.nav_account,
        iconRes = 0,
        contentDescriptionRes = R.string.nav_account_desc
    )
}

/**
 * Returns `true` when the current route belongs to the public authentication flow.
 */
fun String?.isPublicAuthRoute(): Boolean =
    this != null && (
        contains("Auth") ||
            contains("Login") ||
            contains("Register")
        )

/**
 * Returns `true` when the current route should be protected behind authentication.
 */
fun String?.isProtectedRoute(): Boolean = this != null && !isPublicAuthRoute()
