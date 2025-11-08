package com.minikasirpintarfree.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

data class StoreProfile(
    val name: String = "MiniKasir",
    val address: String = "",
    val phone: String = ""
)

object StoreProfileHelper {
    private const val PREF_STORE_NAME = "store_name"
    private const val PREF_STORE_ADDRESS = "store_address"
    private const val PREF_STORE_PHONE = "store_phone"
    
    fun saveStoreProfile(context: Context, profile: StoreProfile) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().apply {
            putString(PREF_STORE_NAME, profile.name)
            putString(PREF_STORE_ADDRESS, profile.address)
            putString(PREF_STORE_PHONE, profile.phone)
            apply()
        }
    }
    
    fun getStoreProfile(context: Context): StoreProfile {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return StoreProfile(
            name = prefs.getString(PREF_STORE_NAME, "MiniKasir") ?: "MiniKasir",
            address = prefs.getString(PREF_STORE_ADDRESS, "") ?: "",
            phone = prefs.getString(PREF_STORE_PHONE, "") ?: ""
        )
    }
}