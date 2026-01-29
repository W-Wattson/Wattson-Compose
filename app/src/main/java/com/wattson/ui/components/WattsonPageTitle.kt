package com.wattson.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonPreviewTheme

@Composable
fun WattsonPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            accentColor,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            accentColor
        ),
        startX = animatedOffset * 500f,
        endX = animatedOffset * 500f + 800f
    )

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = title,
                style = TextStyle(
                    brush = gradientBrush,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            )
        }

        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WattsonPageTitlePreview() {
    WattsonPreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WattsonPageTitle(
                title = "Historique",
                subtitle = "Vos produits scannés"
            )
        }
    }
}
