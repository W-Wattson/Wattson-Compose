package com.wattson.ui.i18n

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class StringResource(
        @StringRes val resId: Int,
        vararg args: Any
    ) : UiText {
        val args: List<Any> = args.toList()
    }

    class PluralResource(
        @PluralsRes val resId: Int,
        val count: Int,
        vararg args: Any
    ) : UiText {
        val args: List<Any> = args.toList()
    }

    data object Empty : UiText
}

class UserFacingException(
    val uiText: UiText,
    cause: Throwable? = null
) : Exception(null, cause)

fun UiText.asString(context: Context): String = when (this) {
    UiText.Empty -> ""
    is UiText.DynamicString -> value
    is UiText.StringResource -> context.getString(
        resId,
        *args.resolveArguments(context).toTypedArray()
    )

    is UiText.PluralResource -> context.resources.getQuantityString(
        resId,
        count,
        *args.resolveArguments(context).toTypedArray()
    )
}

@Composable
fun UiText.asString(): String = asString(LocalContext.current)

fun Throwable.toUiTextOr(defaultText: UiText): UiText {
    return when (this) {
        is UserFacingException -> uiText
        else -> defaultText
    }
}

private fun List<Any>.resolveArguments(context: Context): List<Any> {
    return map { argument ->
        when (argument) {
            is UiText -> argument.asString(context)
            else -> argument
        }
    }
}
