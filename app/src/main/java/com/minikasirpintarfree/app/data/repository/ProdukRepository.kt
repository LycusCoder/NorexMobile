package com.minikasirpintarfree.app.data.repository

import com.minikasirpintarfree.app.data.dao.ProdukDao
import com.minikasirpintarfree.app.data.model.Produk
import kotlinx.coroutines.flow.Flow

class ProdukRepository(private val produkDao: ProdukDao) {
    fun getAllProduk(): Flow<List<Produk>> = produkDao.getAllProduk()

    suspend fun getProdukById(id: Long): Produk? = produkDao.getProdukById(id)

    suspend fun getProdukByBarcode(barcode: String): Produk? = produkDao.getProdukByBarcode(barcode)

    fun searchProduk(query: String): Flow<List<Produk>> = produkDao.searchProduk(query)

    fun getProdukStokMenipis(threshold: Int = 10): Flow<List<Produk>> = produkDao.getProdukStokMenipis(threshold)

    suspend fun insertProduk(produk: Produk): Long = produkDao.insertProduk(produk)

    suspend fun insertAll(produkList: List<Produk>) = produkDao.insertAll(produkList)

    suspend fun updateProduk(produk: Produk) = produkDao.updateProduk(produk)

    suspend fun deleteProduk(produk: Produk) = produkDao.deleteProduk(produk)

    suspend fun deleteAllProduk() = produkDao.deleteAllProduk()

    fun getTotalProduk(): Flow<Int> = produkDao.getTotalProduk()

    /**
     * Atomic operation untuk mengurangi stok produk
     * @return jumlah row yang ter-update (0 jika gagal, 1 jika berhasil)
     */
    suspend fun decrementStok(productId: Long, quantity: Int): Int = produkDao.decrementStok(productId, quantity)
}
