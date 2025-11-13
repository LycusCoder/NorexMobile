package com.minikasirpintarfree.app.data.repository

import com.minikasirpintarfree.app.data.dao.NotifikasiDao
import com.minikasirpintarfree.app.data.model.Notifikasi
import kotlinx.coroutines.flow.Flow

class NotifikasiRepository(private val notifikasiDao: NotifikasiDao) {
    fun getAllNotifikasi(): Flow<List<Notifikasi>> = notifikasiDao.getAllNotifikasi()
    
    fun getUnreadNotifikasi(): Flow<List<Notifikasi>> = notifikasiDao.getUnreadNotifikasi()
    
    fun getUnreadCount(): Flow<Int> = notifikasiDao.getUnreadCount()
    
    suspend fun insertNotifikasi(notifikasi: Notifikasi): Long = notifikasiDao.insertNotifikasi(notifikasi)
    
    suspend fun markAsRead(id: Long) = notifikasiDao.markAsRead(id)
    
    suspend fun markAllAsRead() = notifikasiDao.markAllAsRead()
    
    suspend fun deleteNotifikasi(id: Long) = notifikasiDao.deleteNotifikasi(id)
    
    suspend fun deleteAllNotifikasi() = notifikasiDao.deleteAllNotifikasi()
    
    suspend fun hasLowStockNotificationToday(productName: String, startOfDay: Long, endOfDay: Long): Int {
        return notifikasiDao.hasLowStockNotificationToday(productName, startOfDay, endOfDay)
    }
}

