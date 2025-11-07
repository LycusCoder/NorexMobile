# 📋 Dokumentasi Perbaikan Aplikasi Mini Kasir Pintar

## 📌 Overview
Dokumen ini berisi rencana perbaikan aplikasi Mini Kasir Pintar dari masalah kritis hingga technical debt. Perbaikan dibagi menjadi 3 fase berdasarkan prioritas.

---

## ✅ FASE 1: KRITIS (Fungsional & Integritas Data) - **SELESAI**

### 1.1 ✅ Fix Password Change yang "Bohongan"

**Status: SELESAI ✅**

#### Masalah
- Fitur "Ubah Password" di `SettingsActivity.kt` hanya menampilkan Toast tanpa benar-benar mengubah password
- Password tersimpan sebagai konstanta hardcoded `"admin123"`, bukan di SharedPreferences
- Fungsi `changePassword()` di `LoginViewModel.kt` tidak terhubung dengan UI
- Tidak ada sinkronisasi antara login dan change password

#### Solusi yang Diimplementasikan

**File yang Diubah:**
1. **LoginViewModel.kt**
   - Ubah `PREF_PASSWORD` dari value konstanta menjadi key `KEY_CURRENT_PASSWORD`
   - Tambah fungsi `initializeDefaultPassword()` untuk first-time setup
   - Update fungsi `login()` untuk membaca password dari SharedPreferences
   - Perbaiki fungsi `changePassword()` untuk:
     - Membaca password saat ini dari SharedPreferences
     - Membandingkan dengan oldPassword input
     - Menyimpan newPassword ke SharedPreferences jika cocok

2. **SettingsActivity.kt**
   - Import dan inject `LoginViewModel` menggunakan `LoginViewModelFactory`
   - Update `showChangePasswordDialog()` untuk memanggil `loginViewModel.changePassword()`
   - Tambah handling response: Toast sukses jika berhasil, Toast error jika password lama salah

#### Cara Kerja
1. Default password: `admin123` (disimpan di SharedPreferences saat first-time setup)
2. User bisa ubah password melalui Settings → Ubah Password
3. Password baru akan disimpan di SharedPreferences dengan key `"current_password"`
4. Login akan membaca password dari SharedPreferences

#### Catatan
- Password disimpan plain text (untuk production, harus di-hash dengan bcrypt/SHA-256)
- Sistem masih single-user (username: admin)
- Fitur multi-user/register akan diimplementasikan di fase berikutnya

---

### 1.2 ✅ Fix Race Condition Stok (FATAL)

**Status: SELESAI ✅**

#### Masalah
- Logic di `TransaksiViewModel.kt` menggunakan pattern READ → MODIFY → WRITE yang tidak atomic
- Flow: `getProdukById()` → hitung stok di Kotlin → `updateProduk()`
- Jika 2 transaksi diproses bersamaan, bisa terjadi:
  - Stok menjadi minus
  - Data corrupt
  - Race condition

#### Solusi yang Diimplementasikan

**File yang Diubah:**
1. **ProdukDao.kt**
   ```kotlin
   @Query("UPDATE produk SET stok = stok - :quantity WHERE id = :productId AND stok >= :quantity")
   suspend fun decrementStok(productId: Long, quantity: Int): Int
   ```
   - Query atomic di database level
   - Hanya update jika `stok >= quantity` (kondisi dalam WHERE clause)
   - Return jumlah row yang ter-update (0 = gagal, 1 = berhasil)

2. **ProdukRepository.kt**
   ```kotlin
   suspend fun decrementStok(productId: Long, quantity: Int): Int = produkDao.decrementStok(productId, quantity)
   ```
   - Expose fungsi atomic ke layer repository

3. **TransaksiViewModel.kt**
   ```kotlin
   _cartItems.value.forEach { item ->
       val rowsUpdated = produkRepository.decrementStok(item.produkId, item.quantity)
       
       if (rowsUpdated == 0) {
           val produk = produkRepository.getProdukById(item.produkId)
           val produkNama = produk?.nama ?: "Produk ID ${item.produkId}"
           throw Exception("Stok $produkNama tidak mencukupi")
       }
   }
   ```
   - Ganti logic `getProdukById + updateProduk` dengan `decrementStok()`
   - Check return value untuk validasi stok
   - Throw exception jika stok tidak cukup

#### Keuntungan
- ✅ **Thread-safe**: Operasi dilakukan di database level
- ✅ **Atomic**: Tidak ada gap antara read-modify-write
- ✅ **Efficient**: Satu query untuk update stok
- ✅ **Safe**: Tidak akan pernah menghasilkan stok minus

#### Cara Kerja
1. Saat transaksi diproses, setiap item di keranjang akan:
   - Memanggil `decrementStok(productId, quantity)` ke database
   - Database akan check apakah `stok >= quantity`
   - Jika YA: update stok dan return 1
   - Jika TIDAK: tidak update dan return 0
2. ViewModel akan throw error jika ada item yang stoknya tidak cukup
3. Transaksi akan gagal jika ada error (tidak akan tersimpan)

---

## 🔄 FASE 2: FUNGSIONALITAS INTI (UX Scan & Database)

**Status: PENDING - Menunggu Konfirmasi**

### 2.1 Fix Alur Kerja (UX) Scan

#### Masalah
- Alur scan barcode tidak user-friendly
- Saat produk tidak ditemukan, hanya muncul Toast
- Tidak ada opsi untuk langsung menambah produk baru dengan barcode tersebut

#### Solusi yang Akan Diimplementasikan

**UX 1: Scan dari Dialog Tambah Produk**
- **File**: `dialog_add_edit_produk.xml`, `AddEditProdukDialogFragment.kt`
- **Tindakan**:
  1. Tambah ImageButton (ikon scan) di sebelah field `etBarcode`
  2. Setup listener untuk trigger scanner dari dialog
  3. Butuh refactor: scanner dipanggil dari Activity, perlu interface dari Dialog ke Activity

**UX 2: Scan dari Kasir (PRIORITAS)**
- **File**: `TransaksiActivity.kt`
- **Tindakan**:
  1. Dalam fungsi `addProdukByBarcode()`, bagian `else` (produk null)
  2. Ganti Toast dengan `AlertDialog`:
     ```kotlin
     AlertDialog.Builder(this)
         .setTitle("Produk Tidak Ditemukan")
         .setMessage("Produk dengan barcode [$barcode] tidak ditemukan. Tambah produk baru?")
         .setPositiveButton("Ya") { _, _ ->
             // Open AddEditProdukDialogFragment
             // Pass barcode ke field etBarcode
         }
         .setNegativeButton("Tidak", null)
         .show()
     ```
  3. Passing barcode ke `AddEditProdukDialogFragment` untuk auto-fill field barcode

#### Benefit
- User bisa langsung tambah produk baru saat scan barcode yang belum terdaftar
- Mengurangi friction dalam workflow kasir
- UX lebih smooth dan produktif

---

### 2.2 Fix Bom Waktu Database (Migrasi)

#### Masalah
- **BAHAYA**: `.fallbackToDestructiveMigration()` di `AppDatabase.kt`
- Saat update aplikasi dan skema database berubah, SEMUA DATA USER AKAN HILANG
- Ini fatal untuk aplikasi production

#### Solusi yang Akan Diimplementasikan

**File**: `AppDatabase.kt`

**Tindakan**:
1. **Hapus** `.fallbackToDestructiveMigration()`
2. **Tambah** `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)`
3. **Buat** Migration class untuk setiap perubahan skema:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Contoh: Tambah kolom baru
        database.execSQL("ALTER TABLE produk ADD COLUMN supplier TEXT")
    }
}
```

4. **Current version**: 2, siapkan migrasi untuk versi berikutnya (2→3)
5. **Dokumentasikan** setiap perubahan skema di file CHANGELOG_DB.md

#### Benefit
- ✅ Data user aman saat update aplikasi
- ✅ Upgrade database smooth tanpa data loss
- ✅ Production-ready
- ✅ Maintainable untuk jangka panjang

#### Prioritas
**KRITIS** - Harus diimplementasikan sebelum rilis ke production atau sebelum ada update skema

---

## 🧹 FASE 3: BERSIH-BERSIH (Technical Debt)

**Status: PENDING - Low Priority**

### 3.1 Modernisasi Scanner API

#### Masalah
- Masih menggunakan `onActivityResult()` yang deprecated
- Code tidak lifecycle-aware
- Potensi crash saat configuration change

#### Solusi yang Akan Diimplementasikan

**File**: `ProdukActivity.kt`, `TransaksiActivity.kt`

**Tindakan**:
```kotlin
// OLD (Deprecated)
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // ...
}

// NEW (Modern)
private val scannerLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val barcode = result.data?.getStringExtra("barcode")
        // Handle barcode
    }
}
```

#### Benefit
- ✅ Lifecycle-aware
- ✅ No deprecation warning
- ✅ Cleaner code
- ✅ Better memory management

---

### 3.2 Patuhi Aturan MVVM

#### Masalah
- `TransaksiActivity.kt` memanggil `ProdukRepository` langsung
- Melanggar prinsip MVVM (Activity seharusnya hanya komunikasi dengan ViewModel)
- Sulit untuk testing

#### Solusi yang Akan Diimplementasikan

**File**: `TransaksiActivity.kt`, `TransaksiViewModel.kt`

**Tindakan**:
1. Pindahkan fungsi `addProdukByBarcode()` dari Activity ke ViewModel
2. Pindahkan fungsi `searchAndAddProduk()` ke ViewModel
3. Activity hanya observe LiveData/StateFlow dari ViewModel
4. Semua business logic di ViewModel

**Before**:
```kotlin
// TransaksiActivity.kt
private fun addProdukByBarcode(barcode: String) {
    lifecycleScope.launch {
        val produk = produkRepository.getProdukByBarcode(barcode) // SALAH!
        // ...
    }
}
```

**After**:
```kotlin
// TransaksiViewModel.kt
fun addProdukByBarcode(barcode: String) {
    viewModelScope.launch {
        val produk = produkRepository.getProdukByBarcode(barcode)
        // ...
    }
}

// TransaksiActivity.kt
private fun addProdukByBarcode(barcode: String) {
    viewModel.addProdukByBarcode(barcode) // BENAR!
}
```

#### Benefit
- ✅ Proper MVVM architecture
- ✅ Easier to test
- ✅ Better separation of concerns
- ✅ Maintainable

---

### 3.3 Turunkan minSdk (Opsional tapi Penting)

#### Masalah
- `minSdk 30` (Android 11) terlalu tinggi untuk target pasar UMKM
- Banyak HP kentang tidak bisa install aplikasi
- Kehilangan potensi user base yang besar

#### Solusi yang Akan Diimplementasikan

**File**: `build.gradle` (module: app)

**Tindakan**:
1. Check API yang digunakan di aplikasi
2. Turunkan `minSdk` ke:
   - **Rekomendasi**: `minSdk 24` (Android 7.0 Nougat) - 94.1% market share
   - **Alternative**: `minSdk 26` (Android 8.0 Oreo) - 92.5% market share
3. Test di device dengan Android versi rendah
4. Add compatibility code jika ada API yang memerlukan version check

```gradle
android {
    defaultConfig {
        minSdk 24 // DOWN from 30
        targetSdk 34
        // ...
    }
}
```

#### Benefit
- ✅ Lebih banyak device yang support
- ✅ Target pasar UMKM lebih luas
- ✅ Competitive advantage
- ✅ User base lebih besar

#### Catatan
- Check apakah ada API spesifik Android 11+ yang digunakan
- Test di device Android 7/8 untuk memastikan kompatibilitas
- Add version check untuk fitur yang butuh API level tinggi

---

## 📊 Prioritas Implementasi

### Prioritas 1 - KRITIS (SELESAI)
- ✅ Fix Password Change
- ✅ Fix Race Condition Stok

### Prioritas 2 - PENTING (PENDING)
- ⏳ Fix UX Scan (Fase 2.1)
- ⏳ Fix Database Migration (Fase 2.2)

### Prioritas 3 - OPTIONAL (PENDING)
- ⏳ Modernisasi Scanner API (Fase 3.1)
- ⏳ MVVM Compliance (Fase 3.2)
- ⏳ Turunkan minSdk (Fase 3.3)

---

## 🎯 Roadmap

### Sprint 1 - ✅ SELESAI
- [x] Analisis masalah kritis
- [x] Fix password system
- [x] Fix race condition stok
- [x] Testing manual

### Sprint 2 - NEXT
- [ ] Konfirmasi scope Fase 2
- [ ] Implementasi UX Scan
- [ ] Setup database migration
- [ ] Testing

### Sprint 3 - FUTURE
- [ ] Refactor ke modern API
- [ ] Enforce MVVM pattern
- [ ] Turunkan minSdk
- [ ] Testing compatibility

---

## 📝 Catatan Penting

### Untuk Developer
1. **JANGAN** update skema database sebelum setup migration (Fase 2.2)
2. **JANGAN** deploy ke production sebelum Fase 2.2 selesai
3. **SELALU** test perubahan di device real, bukan hanya emulator
4. **BACKUP** database sebelum testing perubahan database

### Untuk Future Development
1. Pertimbangkan fitur **multi-user/register** untuk sistem kasir multi-akun
2. Implementasi **encryption** untuk password (bcrypt/SHA-256)
3. Tambah **audit log** untuk tracking perubahan stok
4. Implementasi **backup/restore** database untuk keamanan data

### Security Notes
- Password saat ini disimpan plain text → Harus di-hash untuk production
- Pertimbangkan implementasi biometric authentication
- Add session timeout untuk keamanan

---

## 🔗 File Reference

### Fase 1 (SELESAI)
- `LoginViewModel.kt` - Password management
- `SettingsActivity.kt` - UI untuk change password
- `ProdukDao.kt` - Atomic stok operation
- `ProdukRepository.kt` - Repository layer
- `TransaksiViewModel.kt` - Transaction processing

### Fase 2 (PENDING)
- `TransaksiActivity.kt` - UX scan improvement
- `AddEditProdukDialogFragment.kt` - Dialog tambah produk
- `AppDatabase.kt` - Database migration

### Fase 3 (PENDING)
- `ProdukActivity.kt` - Scanner modernization
- `TransaksiActivity.kt` - MVVM refactor
- `build.gradle` - minSdk configuration

---

## ✅ Checklist Testing

### Password System
- [ ] Login dengan default password (admin/admin123)
- [ ] Ubah password ke password baru
- [ ] Logout dan login dengan password baru
- [ ] Coba ubah password dengan password lama yang salah
- [ ] Validasi password minimal 6 karakter
- [ ] Validasi konfirmasi password harus sama

### Stok System
- [ ] Buat transaksi dengan 1 item, check stok berkurang
- [ ] Buat transaksi dengan multiple items, check semua stok berkurang
- [ ] Coba transaksi dengan stok tidak cukup, harus error
- [ ] Test concurrent transactions (2 kasir bersamaan)
- [ ] Verify stok tidak pernah minus
- [ ] Check error message jelas saat stok habis

---

**Dokumentasi dibuat:** 2025
**Last updated:** Fase 1 Selesai
**Status:** Fase 1 ✅ | Fase 2 ⏳ | Fase 3 ⏳
