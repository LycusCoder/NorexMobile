package com.minikasirpintarfree.app

import android.app.Application
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository

class MyApplication : Application() {

    // Buat database dan repository sebagai Singleton yang di-lazy
    // Ini baru akan dibuat sekali saat pertama kali diakses
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val produkRepository by lazy { ProdukRepository(database.produkDao()) }
    val transaksiRepository by lazy { TransaksiRepository(database.transaksiDao()) }
}
