# 🔧 Fix: Compilation Errors di ProdukActivity.kt

## 📋 Masalah

Saat compile project, muncul error di `ProdukActivity.kt` baris 217 dan 232:

```
e: file:///home/lycus/AndroidStudioProjects/NorexMobile/app/src/main/java/com/minikasirpintarfree/app/ui/produk/ProdukActivity.kt:217:50 No value passed for parameter 'onSave'
e: file:///home/lycus/AndroidStudioProjects/NorexMobile/app/src/main/java/com/minikasirpintarfree/app/ui/produk/ProdukActivity.kt:217:56 Type mismatch: inferred type is ([Error type: Cannot infer a lambda parameter type]) -> Unit but String? was expected
e: file:///home/lycus/AndroidStudioProjects/NorexMobile/app/src/main/java/com/minikasirpintarfree/app/ui/produk/ProdukActivity.kt:217:58 Cannot infer a type for this parameter. Please specify it explicitly.
```

## 🔍 Root Cause Analysis (RCA)

### Signature AddEditProdukDialogFragment

Constructor `AddEditProdukDialogFragment` memiliki 3 parameter:

```kotlin
class AddEditProdukDialogFragment(
    private val produk: Produk?,              // Parameter 1: produk yang akan di-edit (null untuk add)
    private val onSave: (Produk) -> Unit,     // Parameter 2: callback saat save (REQUIRED)
    private val prefillBarcode: String? = null // Parameter 3: barcode pre-fill (optional)
) : DialogFragment()
```

### Kode Bermasalah di ProdukActivity.kt

Pada baris 217 dan 232, hanya 2 parameter yang dikirim:

```kotlin
// ❌ SALAH - Hanya 2 parameter
private fun showAddProdukDialog() {
    val dialog = AddEditProdukDialogFragment(null) { produk ->
        // Lambda ini dikira sebagai parameter ke-3 (prefillBarcode: String?)
        // Padahal seharusnya sebagai parameter ke-2 (onSave)
        viewModel.insertProduk(produk)
    }
    dialog.show(supportFragmentManager, "AddProdukDialog")
}

private fun showEditProdukDialog(produk: Produk) {
    val dialog = AddEditProdukDialogFragment(produk) { updatedProduk ->
        // Error yang sama
        viewModel.updateProduk(updatedProduk)
    }
    dialog.show(supportFragmentManager, "EditProdukDialog")
}
```

**Mengapa Error?**
- Kotlin meng-infer lambda `{ produk -> ... }` sebagai parameter ke-3 (`prefillBarcode: String?`)
- Parameter ke-2 (`onSave`) tidak di-pass → Error "No value passed for parameter 'onSave'"
- Type lambda tidak match dengan `String?` → Error "Type mismatch"

## ✅ Solusi

Gunakan **named parameters** untuk menghindari ambiguitas:

```kotlin
// ✅ BENAR - Named parameters
private fun showAddProdukDialog() {
    val dialog = AddEditProdukDialogFragment(
        produk = null,
        onSave = { produk ->
            viewModel.insertProduk(produk)
            NotificationHelper.showNotification(
                this,
                "Produk Berhasil Ditambahkan",
                "Produk ${produk.nama} berhasil didaftarkan",
                NotificationHelper.NOTIFICATION_ID_LOW_STOCK + 1,
                "PRODUCT_ADDED"
            )
        }
    )
    dialog.show(supportFragmentManager, "AddProdukDialog")
}

private fun showEditProdukDialog(produk: Produk) {
    val dialog = AddEditProdukDialogFragment(
        produk = produk,
        onSave = { updatedProduk ->
            viewModel.updateProduk(updatedProduk)
        }
    )
    dialog.show(supportFragmentManager, "EditProdukDialog")
}
```

## 📝 File yang Diubah

### `/app/app/src/main/java/com/minikasirpintarfree/app/ui/produk/ProdukActivity.kt`

**Baris 216-232:** Update fungsi `showAddProdukDialog()` dan `showEditProdukDialog()`

### Perubahan Detail:

**Before:**
```kotlin
AddEditProdukDialogFragment(null) { produk -> ... }
AddEditProdukDialogFragment(produk) { updatedProduk -> ... }
```

**After:**
```kotlin
AddEditProdukDialogFragment(
    produk = null,
    onSave = { produk -> ... }
)
AddEditProdukDialogFragment(
    produk = produk,
    onSave = { updatedProduk -> ... }
)
```

## ✅ Status

**SELESAI** - Compilation errors fixed!

### File TransaksiActivity.kt

File `TransaksiActivity.kt` **sudah benar** sejak awal karena sudah menggunakan named parameters:

```kotlin
val dialog = AddEditProdukDialogFragment(
    produk = null,
    onSave = { newProduk -> ... },
    prefillBarcode = barcode
)
```

## 🧪 Testing

### Cara Test di Android Studio:

1. **Sync Gradle:**
   ```
   File → Sync Project with Gradle Files
   ```

2. **Build Project:**
   ```
   Build → Make Project (Ctrl+F9)
   ```

3. **Verifikasi tidak ada compilation error**

4. **Run di Emulator/Device:**
   - Test fitur tambah produk baru (FAB di ProdukActivity)
   - Test fitur scan barcode → edit produk
   - Pastikan dialog muncul dan bisa save

### Expected Behavior:

✅ Project compile tanpa error
✅ Dialog tambah produk muncul saat klik FAB
✅ Dialog edit produk muncul saat scan barcode
✅ Callback `onSave` terpanggil saat save
✅ Produk tersimpan/terupdate di database

## 📚 Lessons Learned

### Best Practice: Named Parameters

Untuk constructor/function dengan:
- Multiple parameters dengan tipe yang mirip
- Optional parameters
- Lambda parameters

**SELALU gunakan named parameters** untuk menghindari ambiguitas:

```kotlin
// ❌ Avoid (ambiguous)
SomeDialog(param1, { ... })

// ✅ Prefer (explicit)
SomeDialog(
    param1 = param1,
    callback = { ... }
)
```

### Kotlin Lambda Trailing

Kotlin memiliki syntax sugar "trailing lambda":
```kotlin
function(arg1, arg2) { lambda }
// Sama dengan:
function(arg1, arg2, { lambda })
```

**Namun ini bisa ambiguous** jika ada optional parameter setelah lambda!

## 🔗 Related Files

- `/app/app/src/main/java/com/minikasirpintarfree/app/ui/produk/ProdukActivity.kt` (FIXED)
- `/app/app/src/main/java/com/minikasirpintarfree/app/ui/produk/AddEditProdukDialogFragment.kt` (no change)
- `/app/app/src/main/java/com/minikasirpintarfree/app/ui/transaksi/TransaksiActivity.kt` (already correct)

---

**Dokumentasi dibuat:** 2025
**Status:** ✅ SELESAI
**Fase:** Post Fase 2 Bug Fix
