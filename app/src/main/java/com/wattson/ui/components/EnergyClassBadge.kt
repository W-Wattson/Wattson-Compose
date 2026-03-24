package com.wattson.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wattson.R
import com.wattson.domain.model.EnergyClass
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import com.wattson.ui.theme.getEnergyClassColor
import java.text.NumberFormat
import java.util.Locale

/**
 * Energy class badge showing A-G rating for products.
 */
@Composable
fun EnergyClassBadge(
    energyClass: EnergyClass?,
    modifier: Modifier = Modifier,
    size: EnergyBadgeSize = EnergyBadgeSize.MEDIUM,
    showScale: Boolean = true
) {
    if (energyClass == null) {
        // Show placeholder when no energy class available
        EnergyBadgePlaceholder(modifier = modifier, size = size)
        return
    }

    val badgeColor by animateColorAsState(
        targetValue = getEnergyClassColor(energyClass.name),
        label = "energy_badge_color"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main badge with letter
        Box(
            modifier = Modifier
                .size(width = size.badgeWidth, height = size.badgeHeight)
                .clip(WattsonCorners.EnergyBadge)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = energyClass.name,
                style = MaterialTheme.typography.labelLarge,
                fontSize = size.fontSize,
                fontWeight = FontWeight.Bold,
                color = if (energyClass == EnergyClass.D || energyClass == EnergyClass.C) {
                    WattsonColors.Secondary
                } else {
                    WattsonColors.White
                }
            )
        }

        // Scale indicator (A to G vertical)
        if (showScale) {
            EnergyScale(
                selectedClass = energyClass,
                size = size
            )
        }
    }
}

/**
 * Vertical energy scale indicator (A-G)
 */
@Composable
private fun EnergyScale(
    selectedClass: EnergyClass,
    modifier: Modifier = Modifier,
    size: EnergyBadgeSize = EnergyBadgeSize.MEDIUM
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        EnergyClass.entries.forEach { energyClass ->
            val isSelected = energyClass == selectedClass
            val color = if (isSelected) {
                getEnergyClassColor(energyClass.name)
            } else {
                WattsonColors.OnSurfaceVariant.copy(alpha = 0.4f)
            }

            Text(
                text = energyClass.name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = size.scaleTextSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = color
            )
        }
    }
}

/**
 * Placeholder when energy class is not available
 */
@Composable
private fun EnergyBadgePlaceholder(
    modifier: Modifier = Modifier,
    size: EnergyBadgeSize = EnergyBadgeSize.MEDIUM
) {
    Box(
        modifier = modifier
            .size(width = size.badgeWidth, height = size.badgeHeight)
            .clip(WattsonCorners.EnergyBadge)
            .background(WattsonColors.OnSurfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.energy_badge_placeholder),
            style = MaterialTheme.typography.labelLarge,
            fontSize = size.fontSize,
            fontWeight = FontWeight.Bold,
            color = WattsonColors.OnSurfaceVariant
        )
    }
}

/**
 * Size variants for energy badge
 */
enum class EnergyBadgeSize(
    val badgeWidth: Dp,
    val badgeHeight: Dp,
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val scaleTextSize: androidx.compose.ui.unit.TextUnit
) {
    SMALL(
        badgeWidth = 24.dp,
        badgeHeight = 16.dp,
        fontSize = 10.sp,
        scaleTextSize = 6.sp
    ),
    MEDIUM(
        badgeWidth = 32.dp,
        badgeHeight = 22.dp,
        fontSize = 14.sp,
        scaleTextSize = 8.sp
    ),
    LARGE(
        badgeWidth = 48.dp,
        badgeHeight = 32.dp,
        fontSize = 20.sp,
        scaleTextSize = 10.sp
    )
}

/**
 * Repairability score badge
 * Displays the 0-10 repairability index with color coding
 */
@Composable
fun RepairabilityBadge(
    score: Double?,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    if (score == null) {
        if (showLabel) {
            Text(
                text = stringResource(R.string.repairability_not_available),
                style = MaterialTheme.typography.bodySmall,
                color = WattsonColors.OnSurfaceVariant,
                modifier = modifier
            )
        }
        return
    }

    val color = when {
        score >= 7.0 -> WattsonColors.RepairabilityHigh
        score >= 4.0 -> WattsonColors.RepairabilityMedium
        else -> WattsonColors.RepairabilityLow
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showLabel) {
            Text(
                text = stringResource(R.string.repairability_label),
                style = MaterialTheme.typography.bodySmall,
                color = WattsonColors.OnSurfaceVariant
            )
        }

        val scoreText = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(score)

        Text(
            text = stringResource(R.string.score_out_of_ten, scoreText),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * Global score badge displayed as a circular gauge.
 */
@Composable
fun GlobalScoreBadge(
    letter: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val color = getEnergyClassColor(letter)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.global_score_label),
            style = MaterialTheme.typography.labelSmall,
            color = WattsonColors.OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(size)
                .clip(WattsonCorners.Full)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (letter == "D" || letter == "C") {
                    WattsonColors.Secondary
                } else {
                    WattsonColors.White
                }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

// ===== PREVIEWS =====

@Preview(showBackground = true)
@Composable
private fun EnergyClassBadgeAPreview() {
    WattsonPreviewTheme {
        EnergyClassBadge(energyClass = EnergyClass.A)
    }
}

@Preview(showBackground = true)
@Composable
private fun EnergyClassBadgeEPreview() {
    WattsonPreviewTheme {
        EnergyClassBadge(energyClass = EnergyClass.E)
    }
}

@Preview(showBackground = true)
@Composable
private fun EnergyClassBadgeLargePreview() {
    WattsonPreviewTheme {
        EnergyClassBadge(
            energyClass = EnergyClass.B,
            size = EnergyBadgeSize.LARGE
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RepairabilityBadgePreview() {
    WattsonPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RepairabilityBadge(score = 8.2)
            RepairabilityBadge(score = 5.5)
            RepairabilityBadge(score = 2.3)
            RepairabilityBadge(score = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GlobalScoreBadgePreview() {
    WattsonPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlobalScoreBadge(letter = "A", label = "Excellent")
            GlobalScoreBadge(letter = "C", label = "Bon")
            GlobalScoreBadge(letter = "E", label = "Passable")
        }
    }
}
