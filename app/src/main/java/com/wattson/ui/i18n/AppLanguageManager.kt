package com.wattson.ui.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _currentLanguage = MutableStateFlow(resolveCurrentLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag)
        )
        _currentLanguage.value = language
    }

    private fun resolveCurrentLanguage(): AppLanguage {
        val appLanguageTag = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .substringBefore(',')
            .ifBlank {
                context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()
            }

        return AppLanguage.fromLanguageTag(appLanguageTag)
    }
}
