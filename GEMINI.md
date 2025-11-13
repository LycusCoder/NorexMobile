# GEMINI.md - Panduan Khusus Gemini & Android Studio

## 🛠️ Konteks Lingkungan

* **IDE:** Android Studio (diasumsikan versi terbaru).
* **Target Device:** minSdk 24 (Android 7.0 - Nougat).
* **Arsitektur:** Single-Activity (MainActivity) + Fragments.
* **State Management:** LiveData dan Kotlin Flow.

## 🚫 Solusi yang HARUS Dihindari

1.  **Deprecated API:** Jangan sarankan `onActivityResult()`. Gunakan `registerForActivityResult`.
2.  **Destructive Migration:** Jangan sarankan `fallbackToDestructiveMigration()`.
3.  **UI/Biz Logic Mixing:** Jangan sarankan akses `Repository` dari `Fragment/Activity`. Semua *business logic* harus di `ViewModel`.
4.  **Java Code:** Jangan berikan contoh kode Java, hanya Kotlin.

## 💡 Fokus Bantuan yang Diutamakan

1.  **Refactoring MVVM:** Membantu memindahkan *logic* dari Fragment ke ViewModel (terutama di `TransaksiFragment.kt`).
2.  **Android UI XML:** Modifikasi layout XML (e.g., `dialog_payment.xml`, `fragment_laporan.xml`) menggunakan *Material Components* dan *theme attributes* (`?attr/colorPrimary`).
3.  **Coroutines/Flow:** Membantu *collect* atau *launch* coroutine di `lifecycleScope` atau `viewModelScope`.
4.  **Barcode/PDF:** Membantu *utility class* seperti `BarcodeGenerator.kt` atau `PdfGenerator.kt`.

## 📌 Contoh Konvensi Kode (Wajib Ditiru)

* **Panggilan Data:** `viewModelScope.launch { produkRepository.getAllProduk().collect { ... } }`
* **Fragment Binding:** Pola `private var _binding: ...? = null` dengan *cleanup* di `onDestroyView()`.
* **Theme:** Penggunaan *theme attributes* (misal `android:textColor="?attr/colorOnSurface"`).