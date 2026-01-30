package com.wattson.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wattson.R

/**
 * Localized legal disclaimer that highlights the key links (T&amp;C, privacy, GDPR)
 * with the same styling across all screens.
 */
@Composable
fun TermsAndConditionsText(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    val termsLabel = stringResource(id = R.string.terms_label)
    val privacyPolicyLabel = stringResource(id = R.string.privacy_policy_label)
    val gdprLabel = stringResource(id = R.string.gdpr_label)
    val template = stringResource(
        id = R.string.terms_and_conditions,
        termsLabel,
        privacyPolicyLabel,
        gdprLabel
    )

    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium
    )

    val annotated = remember(template, highlightStyle) {
        buildAnnotatedString {
            append(template)

            val keywords = listOf(termsLabel, privacyPolicyLabel, gdprLabel)
            keywords.forEach { keyword ->
                var start = template.indexOf(keyword)
                while (start >= 0) {
                    addStyle(
                        highlightStyle,
                        start = start,
                        end = start + keyword.length
                    )
                    start = template.indexOf(keyword, start + keyword.length)
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier.padding(horizontal = 8.dp)
    )
}
