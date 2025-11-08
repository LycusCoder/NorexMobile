package com.minikasirpintarfree.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minikasirpintarfree.app.MyApplication // Import Application-mu
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository

// Factory-nya sekarang butuh Application
class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            
            // Ambil repository dari Application Singleton
            val app = application as MyApplication
            val produkRepo = app.produkRepository
            val transaksiRepo = app.transaksiRepository
            
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(produkRepo, transaksiRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
