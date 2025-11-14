# Rencana Perbaikan Halaman Transaksi

Dokumen ini berisi daftar tugas untuk memperbaiki dan meningkatkan fungsionalitas halaman Transaksi berdasarkan diskusi.

## 1. Perbaikan UI (Mode Gelap & Konsistensi)

- [ ] **File:** `app/src/main/res/layout/fragment_transaksi.xml`
- [ ] **Tugas:**
    - [ ] Ganti `EditText` pencarian produk dengan `com.google.android.material.textfield.TextInputLayout` untuk memastikan warna teks dan *hint* sesuai dengan tema (terang/gelap).
    - [ ] Ganti `Button` "Cari" dan "Scan Barcode" dengan `com.google.android.material.button.MaterialButton`.
    - [ ] Pastikan semua warna teks menggunakan *theme attributes* (contoh: `android:textColor="?attr/colorOnSurface"`) untuk menghindari warna *hardcode*.

## 2. Fungsionalitas Tombol "Batal"

- [ ] **File Layout:** `app/src/main/res/layout/fragment_transaksi.xml`
    - [ ] **Tugas:** Ubah visibilitas tombol "Batal" (`btnClearCart`) menjadi `android:visibility="gone"` secara default.

- [ ] **File Logic:** `app/src/main/java/com/minikasirpintarfree/app/ui/transaksi/TransaksiFragment.kt`
    - [ ] **Tugas:** Tambahkan `Observer` untuk memantau data keranjang dari `TransaksiViewModel`.
    - [ ] Atur visibilitas tombol "Batal" menjadi `View.VISIBLE` jika keranjang berisi item, dan `View.GONE` jika kosong.

- [ ] **File ViewModel:** `app/src/main/java/com/minikasirpintarfree/app/ui/transaksi/TransaksiViewModel.kt`
    - [ ] **Tugas:** Buat fungsi baru `clearCart()` untuk menghapus semua item dari LiveData/StateFlow keranjang.
    - [ ] Hubungkan fungsi `clearCart()` ke `onClickListener` dari tombol "Batal" di `TransaksiFragment`.

## 3. Ikon Notifikasi Dinamis (Badge Keranjang)

- [ ] **File Menu:** Cari file menu XML yang relevan (kemungkinan di `app/src/main/res/menu/`) yang digunakan oleh `MainActivity` atau `TransaksiFragment`.
    - [ ] **Tugas:** Ganti ID item menu notifikasi dengan ikon `@drawable/ic_shopping_cart_outline`.

- [ ] **File Logic:** `app/src/main/java/com/minikasirpintarfree/app/ui/MainActivity.kt` (atau di mana `Toolbar` dikelola).
    - [ ] **Tugas:** Implementasikan `BadgeDrawable` untuk ikon keranjang di `Toolbar`.
    - [ ] Tambahkan `Observer` ke `TransaksiViewModel` untuk mendapatkan jumlah *item unik* dalam keranjang.
    - [ ] Update angka pada `BadgeDrawable` setiap kali ada perubahan pada keranjang. Angka yang ditampilkan adalah jumlah jenis produk, bukan total kuantitas.
