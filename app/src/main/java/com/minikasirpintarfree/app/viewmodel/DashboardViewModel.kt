package com.minikasirpintarfree.app.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.data.model.BestSellingProduct
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.utils.NotificationHelper
import com.minikasirpintarfree.app.utils.StoreProfileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardViewModel(
    private val produkRepository: ProdukRepository,
    private val transaksiRepository: TransaksiRepository
) : ViewModel() {

    private val _greetingText = MutableLiveData<String>()
    val greetingText: LiveData<String> = _greetingText

    private val _totalProduk = MutableLiveData<Int>()
    val totalProduk: LiveData<Int> = _totalProduk

    private val _totalTransaksiHariIni = MutableLiveData<Int>()
    val totalTransaksiHariIni: LiveData<Int> = _totalTransaksiHariIni

    private val _totalPendapatanHariIni = MutableLiveData<Double>()
    val totalPendapatanHariIni: LiveData<Double> = _totalPendapatanHariIni

    private val _stokMenipis = MutableLiveData<Int>()
    val stokMenipis: LiveData<Int> = _stokMenipis

    private val _bestSellingProducts = MutableLiveData<List<BestSellingProduct>>()
    val bestSellingProducts: LiveData<List<BestSellingProduct>> = _bestSellingProducts

    private val _recentTransaksi = MutableLiveData<List<Transaksi>>()
    val recentTransaksi: LiveData<List<Transaksi>> = _recentTransaksi

    init {
        loadDashboardData()
    }

    fun loadGreeting(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val storeProfile = StoreProfileHelper.getStoreProfile(context)
            val namaToko = storeProfile.name
            val greeting = getGreetingMessage()
            withContext(Dispatchers.Main) {
                _greetingText.value = "$greeting, $namaToko 👋"
            }
        }
    }

    private fun getGreetingMessage(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            in 18..23 -> "Selamat Malam"
            else -> "Halo"
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                produkRepository.getTotalProduk().collect { total ->
                    _totalProduk.postValue(total)
                }
            } catch (e: Exception) {
                _totalProduk.postValue(0)
            }
        }

        viewModelScope.launch {
            try {
                transaksiRepository.getTotalTransaksiHariIni().collect { total ->
                    _totalTransaksiHariIni.postValue(total)
                }
            } catch (e: Exception) {
                _totalTransaksiHariIni.postValue(0)
            }
        }

        viewModelScope.launch {
            try {
                transaksiRepository.getTotalPendapatanHariIni().collect { total ->
                    _totalPendapatanHariIni.postValue(total ?: 0.0)
                }
            } catch (e: Exception) {
                _totalPendapatanHariIni.postValue(0.0)
            }
        }

        viewModelScope.launch {
            try {
                produkRepository.getProdukStokMenipis(10).collect { list ->
                    _stokMenipis.postValue(list.size)
                }
            } catch (e: Exception) {
                _stokMenipis.postValue(0)
            }
        }

        viewModelScope.launch {
            try {
                transaksiRepository.getBestSellingProducts(5).collect { list ->
                    _bestSellingProducts.postValue(list)
                }
            } catch (e: Exception) {
                _bestSellingProducts.postValue(emptyList())
            }
        }

        viewModelScope.launch {
            try {
                transaksiRepository.getRecentTransaksi(5).collect { list ->
                    _recentTransaksi.postValue(list)
                }
            } catch (e: Exception) {
                _recentTransaksi.postValue(emptyList())
            }
        }
    }

    fun checkLowStockNotifications(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val isNotificationEnabled = prefs.getBoolean("notifikasi_stok_harian", false)

            if (!isNotificationEnabled) return@launch

            val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val lastNotifDay = prefs.getInt("last_low_stock_notif_day", -1)

            if (today == lastNotifDay) return@launch

            try {
                val produkList = produkRepository.getProdukStokMenipis(10).first()
                var notificationSent = false
                produkList.forEach { produk ->
                    if (!NotificationHelper.hasLowStockNotificationToday(context, produk.nama)) {
                        NotificationHelper.showLowStockNotification(context, produk.nama, produk.stok)
                        notificationSent = true
                    }
                }
                if (notificationSent) {
                    prefs.edit().putInt("last_low_stock_notif_day", today).apply()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}