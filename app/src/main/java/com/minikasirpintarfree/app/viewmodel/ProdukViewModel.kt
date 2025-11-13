package com.minikasirpintarfree.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProdukViewModel(private val produkRepository: ProdukRepository) : ViewModel() {
    
    private val _produkList = MutableStateFlow<List<Produk>>(emptyList())
    val produkList: StateFlow<List<Produk>> = _produkList.asStateFlow()
    
    private val _produkStokMenipis = MutableStateFlow<List<Produk>>(emptyList())
    val produkStokMenipis: StateFlow<List<Produk>> = _produkStokMenipis.asStateFlow()
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    init {
        loadAllProduk()
        loadProdukStokMenipis()
    }
    
    fun loadAllProduk() {
        viewModelScope.launch {
            try {
                produkRepository.getAllProduk().collect { list ->
                    _produkList.value = list
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Gagal memuat produk: ${e.message}")
                _produkList.value = emptyList()
            }
        }
    }
    
    fun searchProduk(query: String) {
        viewModelScope.launch {
            try {
                produkRepository.searchProduk(query).collect { list ->
                    _produkList.value = list
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Gagal mencari produk: ${e.message}")
                _produkList.value = emptyList()
            }
        }
    }
    
    fun loadProdukStokMenipis() {
        viewModelScope.launch {
            try {
                produkRepository.getProdukStokMenipis(10).collect { list ->
                    _produkStokMenipis.value = list
                }
            } catch (e: Exception) {
                _produkStokMenipis.value = emptyList()
            }
        }
    }
    
    fun getProdukByBarcode(barcode: String, onSuccess: (Produk) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val produk = produkRepository.getProdukByBarcode(barcode)
            if (produk != null) {
                onSuccess(produk)
            } else {
                onError()
            }
        }
    }
    
    fun insertProduk(produk: Produk) {
        viewModelScope.launch {
            try {
                produkRepository.insertProduk(produk)
                _successMessage.postValue("Produk berhasil ditambahkan")
                loadAllProduk()
                // Notifikasi akan dipanggil dari Activity/Fragment yang memiliki Context
            } catch (e: Exception) {
                _errorMessage.postValue("Gagal menambahkan produk: ${e.message}")
            }
        }
    }
    
    fun updateProduk(produk: Produk) {
        viewModelScope.launch {
            try {
                produkRepository.updateProduk(produk)
                _successMessage.postValue("Produk berhasil diupdate")
                loadAllProduk()
            } catch (e: Exception) {
                _errorMessage.postValue("Gagal mengupdate produk: ${e.message}")
            }
        }
    }
    
    fun deleteProduk(produk: Produk) {
        viewModelScope.launch {
            try {
                produkRepository.deleteProduk(produk)
                _successMessage.postValue("Produk berhasil dihapus")
                loadAllProduk()
            } catch (e: Exception) {
                _errorMessage.postValue("Gagal menghapus produk: ${e.message}")
            }
        }
    }
    
    suspend fun updateStok(produkId: Long, quantity: Int): Boolean {
        val produk = produkRepository.getProdukById(produkId)
        return produk?.let {
            val updatedProduk = it.copy(stok = it.stok - quantity)
            if (updatedProduk.stok >= 0) {
                updateProduk(updatedProduk)
                true
            } else {
                _errorMessage.postValue("Stok tidak mencukupi")
                false
            }
        } ?: false
    }
}

