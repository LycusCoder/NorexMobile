package com.minikasirpintarfree.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minikasirpintarfree.app.data.dao.NotifikasiDao
import com.minikasirpintarfree.app.data.dao.ProdukDao
import com.minikasirpintarfree.app.data.dao.TransaksiDao
import com.minikasirpintarfree.app.data.model.Notifikasi
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.model.Transaksi

@Database(
    entities = [Produk::class, Transaksi::class, Notifikasi::class],
    version = 3, // Versi dinaikkan dari 2 ke 3
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun produkDao(): ProdukDao
    abstract fun transaksiDao(): TransaksiDao
    abstract fun notifikasiDao(): NotifikasiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration dari versi 2 ke 3 (kosong karena tidak ada perubahan skema spesifik)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tidak ada perubahan skema yang perlu ditangani di sini
                // Jika ada perubahan, tambahkan query SQL di sini.
                // Contoh: database.execSQL("ALTER TABLE produk ADD COLUMN new_column TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minikasir_database"
                )
                    .addMigrations(MIGRATION_2_3) // Menambahkan migration ke Room
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
