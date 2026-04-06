package com.example.unimarket.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS_NAME = "language_preferences"
    private const val KEY_LANGUAGE_TAG = "selected_language_tag"

    fun applySavedLocale(context: Context) {
        val savedLanguage = context.languagePrefs()
            .getString(KEY_LANGUAGE_TAG, null)
            .orEmpty()

        if (savedLanguage.isNotBlank()) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(savedLanguage)
            )
        }
    }

    fun getSelectedLanguage(context: Context): LanguageOption {
        val appCompatLanguage = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .substringBefore(',')
            .takeIf { it.isNotBlank() }
        if (appCompatLanguage != null) {
            return LanguageOption.fromLanguageTag(appCompatLanguage)
        }

        val savedLanguage = context.languagePrefs()
            .getString(KEY_LANGUAGE_TAG, null)
            .orEmpty()

        if (savedLanguage.isNotBlank()) {
            return LanguageOption.fromLanguageTag(savedLanguage)
        }

        val currentLanguage = context.resources.configuration.locales[0]?.language.orEmpty()
        return LanguageOption.fromLanguageTag(currentLanguage)
    }

    fun updateLanguage(context: Context, language: LanguageOption) {
        context.languagePrefs()
            .edit()
            .putString(KEY_LANGUAGE_TAG, language.languageTag)
            .apply()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag)
        )
    }

    private fun Context.languagePrefs() =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

enum class LanguageOption(val languageTag: String) {
    ENGLISH("en"),
    VIETNAMESE("vi");

    companion object {
        fun fromLanguageTag(languageTag: String?): LanguageOption {
            return when (languageTag?.lowercase().orEmpty()) {
                "vi" -> VIETNAMESE
                else -> ENGLISH
            }
        }
    }
}
