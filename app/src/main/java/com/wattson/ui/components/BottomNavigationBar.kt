package com.wattson.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.wattson.R
import com.wattson.ui.navigation.WattsonRoute
import com.wattson.ui.navigation.toBottomBarRoute
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme

/**
 * Navigation item model for the bottom bar.
 */
private data class NavItem(
    val route: WattsonRoute,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescriptionResId: Int
)

private val bottomNavItems = listOf(
    NavItem(
        route = WattsonRoute.History,
        labelResId = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        contentDescriptionResId = R.string.nav_history_desc
    ),
    NavItem(
        route = WattsonRoute.Repair,
        labelResId = R.string.nav_repair,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        contentDescriptionResId = R.string.nav_repair_desc
    ),
    NavItem(
        route = WattsonRoute.Documents,
        labelResId = R.string.nav_documents,
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        contentDescriptionResId = R.string.nav_documents_desc
    ),
    NavItem(
        route = WattsonRoute.Account,
        labelResId = R.string.nav_account,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        contentDescriptionResId = R.string.nav_account_desc
    )
)

/**
 * Bottom navigation bar that works with NavHostController.
 * Provides navigation between main tabs.
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val selectedRoute = currentRoute.toBottomBarRoute()

    WattsonBottomNavigationBar(
        currentRoute = selectedRoute,
        onNavigate = { destination ->
            navController.navigate(destination) {
                // Pop up to the start destination to avoid building up a large back stack
                popUpTo(WattsonRoute.History) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
        },
        modifier = modifier
    )
}

/**
 * Custom bottom navigation bar with Wattson styling and teal accent.
 */
@Composable
fun WattsonBottomNavigationBar(
    currentRoute: WattsonRoute?,
    onNavigate: (WattsonRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WattsonColors.Secondary,
        shape = WattsonCorners.BottomBar,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route

                BottomNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

/**
 * Individual navigation item with animation
 */
@Composable
private fun BottomNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_item_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) WattsonColors.Primary else WattsonColors.White.copy(alpha = 0.7f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "nav_item_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) WattsonColors.Primary else WattsonColors.White.copy(alpha = 0.7f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "nav_text_color"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(WattsonCorners.Medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Indicator for selected state
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 28.dp)
                    .clip(WattsonCorners.Full)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                WattsonColors.Primary.copy(alpha = 0.3f),
                                WattsonColors.Primary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.selectedIcon,
                    contentDescription = stringResource(id = item.contentDescriptionResId),
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = item.unselectedIcon,
                contentDescription = stringResource(id = item.contentDescriptionResId),
                tint = iconColor,
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(id = item.labelResId),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * Wattson logo displayed at the center of the bottom bar.
 */
@Composable
fun WattsonLogoNavItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(48.dp)
            .clip(WattsonCorners.Medium)
            .background(
                if (isSelected) WattsonColors.Primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.wattson_logo),
            contentDescription = "Wattson Logo",
            modifier = Modifier.size(32.dp)
        )
    }
}

// ===== PREVIEWS =====

@Preview(showBackground = true, backgroundColor = 0xFF191A23)
@Composable
private fun WattsonBottomNavigationBarPreview() {
    WattsonPreviewTheme {
        WattsonBottomNavigationBar(
            currentRoute = WattsonRoute.History,
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191A23)
@Composable
private fun WattsonBottomNavigationBarDocumentsPreview() {
    WattsonPreviewTheme {
        WattsonBottomNavigationBar(
            currentRoute = WattsonRoute.Documents,
            onNavigate = {}
        )
    }
}
