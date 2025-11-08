package com.minikasirpintarfree.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "produk")
data class Produk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val kategori: String,
    val harga: Double,
    var stok: Int,
    val barcode: String? = null,
    val gambar: String? = null,
    val deskripsi: String? = null,
    val notifikasiStokTerkirim: Boolean = false // Flag to prevent spam
) : Parcelable

