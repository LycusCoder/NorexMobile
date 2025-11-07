package com.minikasirpintarfree.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(private val sharedPreferences: android.content.SharedPreferences) : ViewModel() {
    
    companion object {
        const val PREF_IS_LOGGED_IN = "is_logged_in"
        const val PREF_USERNAME = "username"
        const val KEY_CURRENT_PASSWORD = "current_password" // Key untuk menyimpan password
        const val DEFAULT_PASSWORD = "admin123" // Default password untuk first-time setup
        const val DEFAULT_USERNAME = "admin"
    }
    
    init {
        // Initialize default password jika belum ada
        initializeDefaultPassword()
    }
    
    private fun initializeDefaultPassword() {
        val currentPassword = sharedPreferences.getString(KEY_CURRENT_PASSWORD, null)
        if (currentPassword == null) {
            // First-time setup, set default password
            sharedPreferences.edit().apply {
                putString(KEY_CURRENT_PASSWORD, DEFAULT_PASSWORD)
                apply()
            }
        }
    }
    
    fun login(username: String, password: String): Boolean {
        // Baca password dari SharedPreferences
        val storedPassword = sharedPreferences.getString(KEY_CURRENT_PASSWORD, DEFAULT_PASSWORD)
        
        return if (username == DEFAULT_USERNAME && password == storedPassword) {
            viewModelScope.launch {
                sharedPreferences.edit().apply {
                    putBoolean(PREF_IS_LOGGED_IN, true)
                    putString(PREF_USERNAME, username)
                    apply()
                }
            }
            true
        } else {
            false
        }
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }
    
    fun logout() {
        viewModelScope.launch {
            sharedPreferences.edit().apply {
                putBoolean(PREF_IS_LOGGED_IN, false)
                apply()
            }
        }
    }
    
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        // Baca password saat ini dari SharedPreferences
        val currentPassword = sharedPreferences.getString(KEY_CURRENT_PASSWORD, DEFAULT_PASSWORD)
        
        return if (oldPassword == currentPassword) {
            // Password lama cocok, update ke password baru
            // Note: In real app, password should be hashed and stored securely
            sharedPreferences.edit().apply {
                putString(KEY_CURRENT_PASSWORD, newPassword)
                apply()
            }
            true
        } else {
            // Password lama salah
            false
        }
    }
}

