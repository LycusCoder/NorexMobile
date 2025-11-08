package com.minikasirpintarfree.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minikasirpintarfree.app.MyApplication

class ProdukViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProdukViewModel::class.java)) {
            val app = application as MyApplication
            val produkRepo = app.produkRepository
            @Suppress("UNCHECKED_CAST")
            return ProdukViewModel(produkRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
