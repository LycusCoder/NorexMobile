# AGENTS.md - Panduan Arsitektur Proyek Mini Kasir Pintar

## 🎯 Fokus Utama Proyek
Aplikasi Kasir UMKM menggunakan Single-Activity Architecture, MVVM, dan Room Database.

## 🛠️ Aturan Teknologi Wajib

### 1. Bahasa
- **Wajib:** Kotlin. JANGAN gunakan Java.

### 2. Arsitektur
- **Wajib:** MVVM (Model-View-ViewModel).
- **Komponen Navigasi:** Single-Activity (`MainActivity`) yang menghosting Fragments. Navigasi dihandle oleh Navigation Component.
- **View Layer:** Fragments (bukan Activity).

### 3. Asynchronous / Coroutines
- **Wajib:** Kotlin Coroutines dan Flow.
- **Scope:** Gunakan `viewModelScope` untuk operasi di ViewModel.
- **Async di Fragment:** Gunakan `lifecycleScope`.

### 4. Database (Room)
- **Library:** Room Database.
- **Konflik Stok:** Gunakan **atomic query** (`decrementStok`) di `ProdukDao.kt` untuk mencegah *race condition*. JANGAN gunakan pattern Read -> Modify -> Write di Kotlin/ViewModel.
- **Migration:** Database sudah diatur untuk migrasi aman (version 3). JANGAN gunakan `fallbackToDestructiveMigration()`.

### 5. Konvensi Penamaan (Lokal Pride 🇮🇩)
- **Model/Entity:** `Produk`, `Transaksi`, `Notifikasi`.
- **Metode/Variabel:** Gunakan Bahasa Indonesia (e.g., `loadNotifikasi`, `totalHarga`, `uangDiterima`, `formatCurrency`).

## ⚠️ Peringatan Penting (Constraint)

1. **minSdk:** **minSdk sudah diturunkan ke 24 (Android 7.0)**. JANGAN sarankan perubahan atau *upgrade* yang membutuhkan API > 24.
2. **Permission:** Permission Kamera dan Storage sudah dihandle secara *runtime*.
3. **Scanner:** Scanner menggunakan Activity Result API, bukan `onActivityResult()` yang *deprecated*.
4. **Theme:** Terdapat 4 tema berbeda yang diatur oleh `ThemeHelper.kt`.

---

## 📂 Struktur Folder Kunci (Untuk Modifikasi Kode)

| Folder | Isi & Peran |
| :--- | :--- |
| `app/src/main/java/.../ui/` | **VIEW LAYER:** Fragments dan Adapters |
| `app/src/main/java/.../viewmodel/` | **BUSINESS LOGIC:** Semua `ViewModel` dan `Factory` |
| `app/src/main/java/.../repository/` | **DATA LAYER:** Akses data dari DAO |
| `app/src/main/java/.../dao/` | **ROOM DAO:** Interaksi langsung dengan DB (SQL Query) |
| `app/src/main/java/.../model/` | **MODEL/ENTITY:** Class data `Produk`, `Transaksi`, dll. |
| `app/src/main/res/navigation/` | **mobile_navigation.xml:** Graph navigasi utama |