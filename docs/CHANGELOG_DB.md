# 📊 Database Schema Changelog

Dokumen ini mencatat semua perubahan skema database untuk aplikasi Mini Kasir Pintar.

---

## 🎯 Tujuan Dokumen Ini

- **Tracking**: Mencatat setiap perubahan struktur database
- **Migration**: Panduan untuk membuat migration code yang aman
- **History**: Audit trail perubahan database dari waktu ke waktu
- **Safety**: Memastikan data user tidak hilang saat update aplikasi

---

## 📝 Database Schema Versions

### Version 2 (Current - Stable) ✅

**Release Date**: 2025
**Status**: PRODUCTION

#### Entities

**1. Produk**
- `id: Long` (Primary Key, Auto-generated)
- `nama: String` (Nama produk)
- `kategori: String` (Kategori produk)
- `harga: Double` (Harga jual)
- `stok: Int` (Jumlah stok)
- `barcode: String?` (Barcode produk, nullable)
- `deskripsi: String?` (Deskripsi produk, nullable)

**2. Transaksi**
- `id: Long` (Primary Key, Auto-generated)
- `tanggal: Date` (Tanggal transaksi)
- `totalHarga: Double` (Total harga transaksi)
- `uangDiterima: Double` (Uang yang diterima dari customer)
- `kembalian: Double` (Kembalian untuk customer)
- `items: String` (JSON string berisi list TransaksiItem)

**3. Notifikasi**
- `id: Long` (Primary Key, Auto-generated)
- `judul: String` (Judul notifikasi)
- `pesan: String` (Isi pesan notifikasi)
- `tanggal: Date` (Tanggal notifikasi dibuat)
- `isRead: Boolean` (Status sudah dibaca atau belum)
- `tipe: String` (Tipe notifikasi: STOK_HABIS, TRANSAKSI_SUKSES, dll)

#### Indices
- **Produk**: Index pada `barcode` untuk pencarian cepat
- **Transaksi**: Index pada `tanggal` untuk query laporan
- **Notifikasi**: Index pada `isRead` dan `tanggal`

#### Changes from Version 1
- Perubahan tidak terdokumentasi (version 1 tidak ada dokumentasi)
- Version 2 adalah baseline untuk dokumentasi ini

---

## 🔄 Migration Guide

### Cara Membuat Migration Baru

Saat Anda perlu mengubah skema database (tambah tabel, kolom, atau constraint), ikuti langkah berikut:

#### Step 1: Update Entity Class
```kotlin
@Entity(tableName = "produk")
data class Produk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val kategori: String,
    val harga: Double,
    val stok: Int,
    val barcode: String?,
    val deskripsi: String?,
    val supplier: String? = null  // ⬅️ CONTOH: Kolom baru
)
```

#### Step 2: Increment Database Version
```kotlin
@Database(
    entities = [Produk::class, Transaksi::class, Notifikasi::class],
    version = 3,  // ⬅️ Increment dari 2 ke 3
    exportSchema = false
)
```

#### Step 3: Create Migration Code
```kotlin
// Di AppDatabase.kt, tambahkan di companion object:
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQL untuk menambah kolom baru
        database.execSQL("ALTER TABLE produk ADD COLUMN supplier TEXT")
    }
}
```

#### Step 4: Register Migration
```kotlin
fun getDatabase(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "minikasir_database"
        )
            .addMigrations(MIGRATION_2_3)  // ⬅️ Register migration
            .build()
        INSTANCE = instance
        instance
    }
}
```

#### Step 5: Update Dokumentasi
Tambahkan entry baru di dokumen ini untuk Version 3

---

## 🚨 Migration Best Practices

### DO ✅
- **ALWAYS** create migration untuk setiap perubahan skema
- **ALWAYS** test migration di device real sebelum release
- **ALWAYS** dokumentasikan perubahan di file ini
- **BACKUP** database sebelum testing migration
- **TEST** migration dari setiap versi lama ke versi baru
- **USE** `ALTER TABLE ADD COLUMN` untuk tambah kolom
- **SET** default value untuk kolom baru yang NOT NULL

### DON'T ❌
- **NEVER** gunakan `.fallbackToDestructiveMigration()` di production
- **NEVER** ubah nama kolom tanpa migration path
- **NEVER** hapus kolom tanpa migration strategy
- **NEVER** lupa increment version number
- **NEVER** skip version number (misal: 2 → 4, harus 2 → 3 → 4)

---

## 📋 Contoh Migration Scenarios

### Scenario 1: Tambah Kolom Baru (Nullable)
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE produk ADD COLUMN foto TEXT")
    }
}
```

### Scenario 2: Tambah Kolom Baru (NOT NULL dengan Default)
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE produk ADD COLUMN aktif INTEGER NOT NULL DEFAULT 1")
    }
}
```

### Scenario 3: Buat Tabel Baru
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS pelanggan (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nama TEXT NOT NULL,
                telepon TEXT,
                alamat TEXT
            )
        """)
    }
}
```

### Scenario 4: Rename Kolom (Complex)
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Room tidak support RENAME COLUMN langsung di SQLite lama
        // Harus: Create new table → Copy data → Drop old → Rename new
        
        database.execSQL("""
            CREATE TABLE produk_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nama_produk TEXT NOT NULL,
                kategori TEXT NOT NULL,
                harga REAL NOT NULL,
                stok INTEGER NOT NULL,
                barcode TEXT,
                deskripsi TEXT
            )
        """)
        
        database.execSQL("""
            INSERT INTO produk_new (id, nama_produk, kategori, harga, stok, barcode, deskripsi)
            SELECT id, nama, kategori, harga, stok, barcode, deskripsi FROM produk
        """)
        
        database.execSQL("DROP TABLE produk")
        database.execSQL("ALTER TABLE produk_new RENAME TO produk")
    }
}
```

### Scenario 5: Multiple Migrations (Chain)
```kotlin
// Jika user skip beberapa version, Room akan run semua migration secara berurutan
// Contoh: User di version 2, update ke version 5
// Room akan run: MIGRATION_2_3 → MIGRATION_3_4 → MIGRATION_4_5

fun getDatabase(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(...)
            .addMigrations(
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5
            )
            .build()
        INSTANCE = instance
        instance
    }
}
```

---

## 🧪 Testing Migrations

### Manual Testing Checklist
1. ✅ Install versi lama aplikasi (version X)
2. ✅ Buat data dummy (produk, transaksi, notifikasi)
3. ✅ Install versi baru aplikasi (version Y) via APK
4. ✅ Buka aplikasi dan verify:
   - App tidak crash
   - Data lama masih ada dan utuh
   - Fitur baru berfungsi dengan baik
   - Tidak ada error di Logcat

### Automated Testing (Advanced)
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )
    
    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        // Create database version 2
        var db = helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO produk (nama, kategori, harga, stok) VALUES ('Test', 'Makanan', 10000, 10)")
            close()
        }
        
        // Run migration to version 3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        
        // Verify data
        val cursor = db.query("SELECT * FROM produk")
        cursor.moveToFirst()
        assert(cursor.getString(cursor.getColumnIndex("nama")) == "Test")
    }
}
```

---

## 📈 Future Planned Changes

Berikut adalah rencana perubahan skema untuk versi mendatang:

### Version 3 (Planned)
- [ ] **Produk**: Tambah kolom `supplier: String?` untuk tracking supplier
- [ ] **Produk**: Tambah kolom `foto: String?` untuk URL/path foto produk
- [ ] **Transaksi**: Tambah kolom `metodePembayaran: String` (cash/transfer/qris)
- [ ] **New Table**: `Pelanggan` untuk fitur loyalitas customer

### Version 4 (Ideas)
- [ ] **Produk**: Tambah kolom `berat: Double?` untuk produk yang dijual per kg
- [ ] **Produk**: Tambah kolom `expired: Date?` untuk tracking produk kadaluarsa
- [ ] **New Table**: `User` untuk multi-user/multi-kasir
- [ ] **New Table**: `AuditLog` untuk tracking perubahan stok

---

## 🔗 References

- [Room Migration Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [SQLite ALTER TABLE](https://www.sqlite.org/lang_altertable.html)
- [Room Testing](https://developer.android.com/training/data-storage/room/testing-db)

---

## 📞 Support

Jika ada pertanyaan tentang database migration:
1. Baca dokumentasi ini terlebih dahulu
2. Check contoh migration di atas
3. Test di emulator sebelum test di device real
4. Backup database sebelum testing

---

**Last Updated**: Fase 2 - 2025
**Maintained By**: Development Team
**Current Version**: 2 (Stable)
