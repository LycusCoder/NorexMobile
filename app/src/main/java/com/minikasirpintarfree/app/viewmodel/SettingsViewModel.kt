package com.minikasirpintarfree.app.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val produkRepository: ProdukRepository,
    private val transaksiRepository: TransaksiRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    fun getAllProduk(): Flow<List<Produk>> {
        return produkRepository.getAllProduk()
    }

    suspend fun getProdukByBarcode(barcode: String): Produk? {
        return produkRepository.getProdukByBarcode(barcode)
    }

    fun insertAllProduk(produkList: List<Produk>) {
        viewModelScope.launch {
            produkRepository.insertAll(produkList)
        }
    }

    fun resetDataProduk(onComplete: () -> Unit) {
        viewModelScope.launch {
            produkRepository.deleteAllProduk()
            onComplete()
        }
    }

    fun resetDataTransaksi(onComplete: () -> Unit) {
        viewModelScope.launch {
            transaksiRepository.deleteAllTransaksi()
            onComplete()
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            produkRepository.deleteAllProduk()
            transaksiRepository.deleteAllTransaksi()
            onComplete()
        }
    }

    fun getTheme(): String {
        return sharedPreferences.getString("theme", "light") ?: "light"
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            sharedPreferences.edit().putString("theme", theme).apply()
        }
    }
}
