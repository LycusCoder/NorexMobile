package com.minikasirpintarfree.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.minikasirpintarfree.app.data.repository.TransaksiRepository

class TransactionHistoryViewModel(private val repository: TransaksiRepository) : ViewModel() {

    val transactionHistory = repository.getAllTransaksi().asLiveData()

}