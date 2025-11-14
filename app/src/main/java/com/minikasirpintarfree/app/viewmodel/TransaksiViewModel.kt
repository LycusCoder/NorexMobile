package com.minikasirpintarfree.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.model.TransaksiItem
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class TransaksiViewModel(
    private val transaksiRepository: TransaksiRepository,
    private val produkRepository: ProdukRepository
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<TransaksiItem>>(emptyList())
    val cartItems: StateFlow<List<TransaksiItem>> = _cartItems.asStateFlow()

    private val _totalHarga = MutableLiveData<Double>()
    val totalHarga: LiveData<Double> = _totalHarga

    private val _uangDiterima = MutableLiveData<Double>()
    val uangDiterima: LiveData<Double> = _uangDiterima

    private val _kembalian = MutableLiveData<Double>()
    val kembalian: LiveData<Double> = _kembalian

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val _productNotFound = MutableLiveData<String>()
    val productNotFound: LiveData<String> = _productNotFound

    private val _searchResults = MutableLiveData<List<Produk>>()
    val searchResults: LiveData<List<Produk>> = _searchResults

    private val gson = Gson()

    init {
        calculateTotal()
    }

    fun addProductToCart(produk: Produk) {
        val item = TransaksiItem(
            produkId = produk.id,
            namaProduk = produk.nama,
            harga = produk.harga,
            quantity = 1,
            subtotal = produk.harga
        )
        addItemToCart(item)
        _successMessage.postValue("Produk ditambahkan: ${produk.nama}")
    }

    fun addItemToCart(item: TransaksiItem) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst { it.produkId == item.produkId }

        if (existingItemIndex >= 0) {
            val existingItem = currentItems[existingItemIndex]
            val newQuantity = existingItem.quantity + item.quantity
            val newSubtotal = existingItem.harga * newQuantity
            currentItems[existingItemIndex] = existingItem.copy(
                quantity = newQuantity,
                subtotal = newSubtotal
            )
        } else {
            currentItems.add(item)
        }

        _cartItems.value = currentItems
        calculateTotal()
    }

    fun removeItemFromCart(item: TransaksiItem) {
        val currentItems = _cartItems.value.toMutableList()
        currentItems.remove(item)
        _cartItems.value = currentItems
        calculateTotal()
    }

    fun updateItemQuantity(item: TransaksiItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItemFromCart(item)
            return
        }

        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.produkId == item.produkId }
        if (index >= 0) {
            val updatedItem = currentItems[index].copy(
                quantity = newQuantity,
                subtotal = item.harga * newQuantity
            )
            currentItems[index] = updatedItem
            _cartItems.value = currentItems
            calculateTotal()
        }
    }

    fun setUangDiterima(amount: Double) {
        _uangDiterima.value = amount
        calculateKembalian()
    }

    private fun calculateTotal() {
        val total = _cartItems.value.sumOf { it.subtotal }
        _totalHarga.value = total
        calculateKembalian()
    }

    private fun calculateKembalian() {
        val total = _totalHarga.value ?: 0.0
        val uang = _uangDiterima.value ?: 0.0
        val kembalian = uang - total
        _kembalian.value = if (kembalian >= 0) kembalian else 0.0
    }

    fun processTransaksi(onSuccess: (Transaksi) -> Unit) {
        viewModelScope.launch {
            try {
                val total = _totalHarga.value ?: 0.0
                val uang = _uangDiterima.value ?: 0.0

                if (total == 0.0) {
                    _errorMessage.postValue("Keranjang kosong")
                    return@launch
                }

                if (uang < total) {
                    _errorMessage.postValue("Uang tidak mencukupi")
                    return@launch
                }

                _cartItems.value.forEach { item ->
                    val rowsUpdated = produkRepository.decrementStok(item.produkId, item.quantity)
                    if (rowsUpdated == 0) {
                        val produk = produkRepository.getProdukById(item.produkId)
                        val produkNama = produk?.nama ?: "Produk ID ${item.produkId}"
                        throw Exception("Stok $produkNama tidak mencukupi")
                    }
                }

                val itemsJson = gson.toJson(_cartItems.value)
                val transaksi = Transaksi(
                    tanggal = Date(),
                    totalHarga = total,
                    uangDiterima = uang,
                    kembalian = uang - total,
                    items = itemsJson
                )

                val transaksiId = transaksiRepository.insertTransaksi(transaksi)
                val savedTransaksi = transaksiRepository.getTransaksiById(transaksiId)

                if (savedTransaksi != null) {
                    _successMessage.postValue("Transaksi berhasil")
                    clearCart()
                    onSuccess(savedTransaksi)
                } else {
                    _errorMessage.postValue("Gagal menyimpan transaksi")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error: ${e.message}")
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _uangDiterima.value = 0.0
        _kembalian.value = 0.0
        calculateTotal()
    }

    fun getTransaksiItems(transaksi: Transaksi): List<TransaksiItem> {
        val type = object : TypeToken<List<TransaksiItem>>() {}.type
        return gson.fromJson(transaksi.items, type)
    }

    fun addProdukByBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                val produk = produkRepository.getProdukByBarcode(barcode)
                if (produk != null) {
                    addProductToCart(produk)
                } else {
                    _productNotFound.postValue(barcode)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error mencari produk: ${e.message}")
            }
        }
    }

    fun searchProduk(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.postValue(emptyList())
                return@launch
            }
            try {
                val produkList = produkRepository.searchProduk(query).first()
                _searchResults.postValue(produkList)
            } catch (e: Exception) {
                _errorMessage.postValue("Error mencari produk: ${e.message}")
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    suspend fun insertProdukAndAddToCart(produk: Produk): Boolean {
        return try {
            produkRepository.insertProduk(produk)
            addProductToCart(produk)
            _successMessage.postValue("Produk berhasil ditambahkan!")
            true
        } catch (e: Exception) {
            _errorMessage.postValue("Gagal menambahkan produk: ${e.message}")
            false
        }
    }
}
