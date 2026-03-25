package com.wattson.ui.i18n

import androidx.annotation.StringRes
import com.wattson.R

enum class AppLanguage(
    val languageTag: String,
    @StringRes val labelResId: Int
) {
    ENGLISH(languageTag = "en", labelResId = R.string.language_name_english),
    FRENCH(languageTag = "fr", labelResId = R.string.language_name_french);

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage {
            return if (languageTag?.startsWith("fr", ignoreCase = true) == true) {
                FRENCH
            } else {
                ENGLISH
            }
        }
    }
}
