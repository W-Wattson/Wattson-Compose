package com.wattson.ui.screens.account

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.wattson.R
import com.wattson.domain.model.AuthProvider
import com.wattson.domain.model.PreferenceType
import com.wattson.domain.model.SubscriptionType
import com.wattson.domain.model.User
import com.wattson.domain.model.UserPreferences
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant

/**
 * Account screen displaying user profile and preferences.
 */
@Composable
fun AccountScreen(
    uiState: AccountUiState,
    events: SharedFlow<AccountEvent>,
    onIntent: (AccountIntent) -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle events
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is AccountEvent.NavigateToPremium -> onNavigateToPremium()
                is AccountEvent.NavigateToLogin -> onNavigateToLogin()
                is AccountEvent.LogoutSuccess -> { /* Handled by NavigateToLogin */ }
                is AccountEvent.PreferencesUpdated -> { /* Could show snackbar */ }
                is AccountEvent.ShowError -> { /* Could show snackbar */ }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && uiState.user == null -> {
                LoadingState()
            }
            uiState.user != null -> {
                AccountContent(
                    user = uiState.user,
                    preferences = uiState.preferences,
                    isPremium = uiState.isPremium,
                    documentCount = uiState.documentCount,
                    documentLimit = uiState.documentLimit,
                    onPreferenceReorder = { onIntent(AccountIntent.UpdatePreferenceOrder(it)) },
                    onPremiumClick = { onIntent(AccountIntent.NavigateToPremium) },
                    onLogoutClick = { onIntent(AccountIntent.RequestLogout) }
                )
            }
        }
    }

    // Logout confirmation dialog
    if (uiState.showLogoutConfirmation) {
        LogoutConfirmationDialog(
            onConfirm = { onIntent(AccountIntent.ConfirmLogout) },
            onDismiss = { onIntent(AccountIntent.CancelLogout) }
        )
    }
}

@Composable
private fun AccountContent(
    user: User,
    preferences: UserPreferences?,
    isPremium: Boolean,
    documentCount: Int,
    documentLimit: Int?,
    onPreferenceReorder: (List<PreferenceType>) -> Unit,
    onPremiumClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Profile section
        ProfileSection(
            user = user,
            documentCount = documentCount,
            documentLimit = documentLimit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Preferences section
        PreferencesSection(
            preferences = preferences,
            onReorder = onPreferenceReorder
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Premium card
        PremiumCard(
            isPremium = isPremium,
            onClick = onPremiumClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign out button
        SignOutButton(onClick = onLogoutClick)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileSection(
    user: User,
    documentCount: Int,
    documentLimit: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = WattsonCorners.Large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Text(
                text = user.fullName ?: user.email.substringBefore("@"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Document quota
            if (documentLimit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$documentCount / $documentLimit ${stringResource(R.string.documents_suffix)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (documentCount >= documentLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    preferences: UserPreferences?,
    onReorder: (List<PreferenceType>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.preferences),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preference cards in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            preferences?.orderedPreferences?.forEachIndexed { index, pref ->
                PreferenceCard(
                    preferenceType = pref,
                    rank = index + 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Explanation text
        Text(
            text = stringResource(R.string.preferences_explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PreferenceCard(
    preferenceType: PreferenceType,
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when (preferenceType) {
        PreferenceType.ECOLOGY -> Triple(Icons.Filled.Eco, stringResource(R.string.ecology), WattsonColors.EnergyClassA)
        PreferenceType.ECONOMY -> Triple(Icons.Filled.AttachMoney, stringResource(R.string.economy), WattsonColors.Info)
        PreferenceType.REPAIRABILITY -> Triple(Icons.Filled.Build, stringResource(R.string.repairability_pref), WattsonColors.Warning)
    }

    val rankLabel = when (rank) {
        1 -> stringResource(R.string.first_choice)
        2 -> stringResource(R.string.second_choice)
        3 -> stringResource(R.string.third_choice)
        else -> stringResource(R.string.third_choice) // Fallback
    }

    Card(
        modifier = modifier,
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = rankLabel,
                style = MaterialTheme.typography.labelSmall,
                color = WattsonColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumCard(
    isPremium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isPremium) WattsonColors.Primary.copy(alpha = 0.1f) else WattsonColors.GradientStart.copy(alpha = 0.1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "premiumBgColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = WattsonCorners.Card,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                WattsonColors.GradientStart,
                                WattsonColors.GradientEnd
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) stringResource(R.string.premium_member) else stringResource(R.string.go_premium_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!isPremium) {
                    Text(
                        text = stringResource(R.string.go_premium_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = WattsonCorners.Card,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.sign_out),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.logout_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.logout_confirmation))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.logout), color = WattsonColors.Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ===== PREVIEWS =====

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AccountContentPreview() {
    WattsonPreviewTheme {
        AccountContent(
            user = User(
                id = "user_1",
                email = "amelie.brun@gmail.com",
                fullName = "Amélie Brun",
                authProvider = AuthProvider.EMAIL,
                subscriptionType = SubscriptionType.FREE,
                preferences = UserPreferences(
                    orderedPreferences = listOf(
                        PreferenceType.ECONOMY,
                        PreferenceType.ECOLOGY,
                        PreferenceType.REPAIRABILITY
                    )
                ),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            preferences = UserPreferences(
                orderedPreferences = listOf(
                    PreferenceType.ECONOMY,
                    PreferenceType.ECOLOGY,
                    PreferenceType.REPAIRABILITY
                )
            ),
            isPremium = false,
            documentCount = 3,
            documentLimit = 5,
            onPreferenceReorder = {},
            onPremiumClick = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreferenceCardPreview() {
    WattsonPreviewTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreferenceCard(
                preferenceType = PreferenceType.ECONOMY,
                rank = 1,
                modifier = Modifier.weight(1f)
            )
            PreferenceCard(
                preferenceType = PreferenceType.ECOLOGY,
                rank = 2,
                modifier = Modifier.weight(1f)
            )
            PreferenceCard(
                preferenceType = PreferenceType.REPAIRABILITY,
                rank = 3,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PremiumCardPreview() {
    WattsonPreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PremiumCard(isPremium = false, onClick = {})
            Spacer(modifier = Modifier.height(16.dp))
            PremiumCard(isPremium = true, onClick = {})
        }
    }
}
