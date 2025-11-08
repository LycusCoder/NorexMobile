package com.minikasirpintarfree.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minikasirpintarfree.app.MyApplication
import com.minikasirpintarfree.app.data.repository.TransaksiRepository

class LaporanViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LaporanViewModel::class.java)) {
            val app = application as MyApplication
            val transaksiRepo = app.transaksiRepository
            @Suppress("UNCHECKED_CAST")
            return LaporanViewModel(transaksiRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
