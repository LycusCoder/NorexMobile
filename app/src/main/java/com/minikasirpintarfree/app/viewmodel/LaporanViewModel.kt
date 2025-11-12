package com.minikasirpintarfree.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minikasirpintarfree.app.data.model.BestSellingProduct
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class LaporanViewModel(private val transaksiRepository: TransaksiRepository) : ViewModel() {

    private val _transaksiList = MutableStateFlow<List<Transaksi>>(emptyList())
    val transaksiList: StateFlow<List<Transaksi>> = _transaksiList.asStateFlow()

    private val _produkTerlaris = MutableStateFlow<List<BestSellingProduct>>(emptyList())
    val produkTerlaris: StateFlow<List<BestSellingProduct>> = _produkTerlaris.asStateFlow()

    private val _selectedPeriod = MutableLiveData<String>("Hari Ini")
    val selectedPeriod: LiveData<String> = _selectedPeriod

    init {
        loadProdukTerlaris()
    }

    fun loadTransaksiHariIni() {
        viewModelScope.launch {
            transaksiRepository.getTransaksiHariIni().collect { list ->
                _transaksiList.value = list
                _selectedPeriod.postValue("Hari Ini")
            }
        }
    }

    fun loadTransaksiMingguIni() {
        viewModelScope.launch {
            transaksiRepository.getTransaksiMingguIni().collect { list ->
                _transaksiList.value = list
                _selectedPeriod.postValue("Minggu Ini")
            }
        }
    }

    fun loadTransaksiBulanIni() {
        viewModelScope.launch {
            transaksiRepository.getTransaksiBulanIni().collect { list ->
                _transaksiList.value = list
                _selectedPeriod.postValue("Bulan Ini")
            }
        }
    }

    fun loadTransaksiByDateRange(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            transaksiRepository.getTransaksiByDateRange(startDate, endDate).collect { list ->
                _transaksiList.value = list
                _selectedPeriod.postValue("Custom Range")
            }
        }
    }

    private fun loadProdukTerlaris() {
        viewModelScope.launch {
            transaksiRepository.getBestSellingProducts().collect { produkList ->
                _produkTerlaris.value = produkList
            }
        }
    }

    fun getTotalPendapatan(): Double {
        return _transaksiList.value.sumOf { it.totalHarga }
    }

    fun getTotalTransaksi(): Int {
        return _transaksiList.value.size
    }

    fun getChartData(): List<Pair<String, Double>> {
        val calendar = Calendar.getInstance()
        val dataMap = mutableMapOf<String, Double>()

        _transaksiList.value.forEach { transaksi ->
            calendar.time = transaksi.tanggal
            val key = when (_selectedPeriod.value) {
                "Hari Ini" -> "${calendar.get(Calendar.HOUR_OF_DAY)}:00"
                "Minggu Ini" -> when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> "Minggu"
                    Calendar.MONDAY -> "Senin"
                    Calendar.TUESDAY -> "Selasa"
                    Calendar.WEDNESDAY -> "Rabu"
                    Calendar.THURSDAY -> "Kamis"
                    Calendar.FRIDAY -> "Jumat"
                    Calendar.SATURDAY -> "Sabtu"
                    else -> ""
                }
                "Bulan Ini" -> "${calendar.get(Calendar.DAY_OF_MONTH)}"
                else -> "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}"
            }

            dataMap[key] = (dataMap[key] ?: 0.0) + transaksi.totalHarga
        }

        return dataMap.toList().sortedBy { it.first }
    }
}
