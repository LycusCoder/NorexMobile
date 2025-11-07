package com.minikasirpintarfree.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.R

/**
 * Helper class untuk mengelola tema aplikasi
 */
object ThemeHelper {
    
    private const val PREF_THEME = "app_theme"
    
    // Theme constants
    const val THEME_OCEAN = "ocean"
    const val THEME_FOREST = "forest"
    const val THEME_ROYAL = "royal"
    const val THEME_SUNSET = "sunset"
    const val THEME_CRIMSON = "crimson"
    const val THEME_DARK = "dark"
    
    // Default theme
    private const val DEFAULT_THEME = THEME_SUNSET
    
    /**
     * Apply theme to activity
     */
    fun applyTheme(activity: AppCompatActivity) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val theme = prefs.getString(PREF_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        
        val themeResId = getThemeResId(theme)
        activity.setTheme(themeResId)
    }
    
    /**
     * Get theme resource ID
     */
    private fun getThemeResId(theme: String): Int {
        return when (theme) {
            THEME_OCEAN -> R.style.Theme_MiniKasir_Ocean
            THEME_FOREST -> R.style.Theme_MiniKasir_Forest
            THEME_ROYAL -> R.style.Theme_MiniKasir_Royal
            THEME_SUNSET -> R.style.Theme_MiniKasir_Sunset
            THEME_CRIMSON -> R.style.Theme_MiniKasir_Crimson
            THEME_DARK -> R.style.Theme_MiniKasir_Dark
            else -> R.style.Theme_MiniKasir_Sunset
        }
    }
    
    /**
     * Save theme preference
     */
    fun saveTheme(context: Context, theme: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_THEME, theme).apply()
    }
    
    /**
     * Get current theme
     */
    fun getCurrentTheme(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }
    
    /**
     * Get theme display name
     */
    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            THEME_OCEAN -> "Ocean Blue"
            THEME_FOREST -> "Forest Green"
            THEME_ROYAL -> "Royal Purple"
            THEME_SUNSET -> "Sunset Orange"
            THEME_CRIMSON -> "Crimson Red"
            THEME_DARK -> "Dark Mode"
            else -> "Sunset Orange"
        }
    }
    
    /**
     * Get toolbar color for current theme
     */
    fun getToolbarColor(context: Context): Int {
        val theme = getCurrentTheme(context)
        return when (theme) {
            THEME_OCEAN -> context.getColor(R.color.ocean_primary)
            THEME_FOREST -> context.getColor(R.color.forest_primary)
            THEME_ROYAL -> context.getColor(R.color.royal_primary)
            THEME_SUNSET -> context.getColor(R.color.sunset_primary)
            THEME_CRIMSON -> context.getColor(R.color.crimson_primary)
            THEME_DARK -> context.getColor(R.color.dark_primary)
            else -> context.getColor(R.color.sunset_primary)
        }
    }
}
