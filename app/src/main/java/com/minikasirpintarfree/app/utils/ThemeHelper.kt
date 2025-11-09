package com.minikasirpintarfree.app.utils

import androidx.appcompat.app.AppCompatActivity
import com.minikasirpintarfree.app.R

/**
 * Helper class untuk mengelola tema aplikasi
 */
object ThemeHelper {
    
    /**
     * Apply theme to activity
     */
    fun applyTheme(activity: AppCompatActivity) {
        activity.setTheme(R.style.Theme_MiniKasir)
    }
}
