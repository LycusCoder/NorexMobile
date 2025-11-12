package com.minikasirpintarfree.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.R

object ThemeHelper {

    private const val PREF_THEME = "app_theme"

    // Theme constants
    const val THEME_SPRING = "spring"
    const val THEME_SUMMER = "summer"
    const val THEME_AUTUMN = "autumn"
    const val THEME_WINTER = "winter"

    // Default theme
    private const val DEFAULT_THEME = THEME_WINTER

    fun applyTheme(activity: AppCompatActivity) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val theme = prefs.getString(PREF_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        val isDarkMode = isDarkMode(activity)

        val themeResId = if (isDarkMode) {
            getDarkThemeResId(theme)
        } else {
            getLightThemeResId(theme)
        }
        activity.setTheme(themeResId)
    }

    private fun getLightThemeResId(theme: String): Int {
        return when (theme) {
            THEME_SPRING -> R.style.Theme_MiniKasir_Spring
            THEME_SUMMER -> R.style.Theme_MiniKasir_Summer
            THEME_AUTUMN -> R.style.Theme_MiniKasir_Autumn
            THEME_WINTER -> R.style.Theme_MiniKasir_Winter
            else -> R.style.Theme_MiniKasir_Winter
        }
    }

    private fun getDarkThemeResId(theme: String): Int {
        return when (theme) {
            THEME_SPRING -> R.style.Theme_MiniKasir_Spring_Dark
            THEME_SUMMER -> R.style.Theme_MiniKasir_Summer_Dark
            THEME_AUTUMN -> R.style.Theme_MiniKasir_Autumn_Dark
            THEME_WINTER -> R.style.Theme_MiniKasir_Winter_Dark
            else -> R.style.Theme_MiniKasir_Winter_Dark
        }
    }

    fun saveTheme(context: Context, theme: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_THEME, theme).apply()
    }

    fun getCurrentTheme(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            THEME_SPRING -> "Spring"
            THEME_SUMMER -> "Summer"
            THEME_AUTUMN -> "Autumn"
            THEME_WINTER -> "Winter"
            else -> "Winter"
        }
    }

    private fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
