package com.minikasirpintarfree.app.ui.produk

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.databinding.DialogAddEditProdukBinding
import com.minikasirpintarfree.app.utils.BarcodeGenerator

class AddEditProdukDialogFragment(
    private val produk: Produk?,
    private val onSave: (Produk) -> Unit,
    private val prefillBarcode: String? = null,  // ✅ TAMBAHAN: Parameter untuk pre-fill barcode
    private val onBarcodeGenerated: ((Produk) -> Unit)? = null  // ✅ CALLBACK: For barcode preview
) : DialogFragment() {
    private lateinit var binding: DialogAddEditProdukBinding
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogAddEditProdukBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (produk != null) {
            // Edit mode
            binding.etNama.setText(produk.nama)
            binding.etKategori.setText(produk.kategori)
            binding.etHarga.setText(produk.harga.toString())
            binding.etStok.setText(produk.stok.toString())
            binding.etBarcode.setText(produk.barcode ?: "")
            binding.etDeskripsi.setText(produk.deskripsi ?: "")
            binding.tvTitle.text = "Edit Produk"
        } else {
            binding.tvTitle.text = "Tambah Produk"
            
            // ✅ TAMBAHAN: Auto-fill barcode jika ada prefillBarcode
            if (!prefillBarcode.isNullOrEmpty()) {
                binding.etBarcode.setText(prefillBarcode)
                // Focus ke field nama agar user bisa langsung input nama produk
                binding.etNama.requestFocus()
            }
        }
        
        binding.btnSave.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            val kategori = binding.etKategori.text.toString().trim()
            val harga = binding.etHarga.text.toString().toDoubleOrNull() ?: 0.0
            val stok = binding.etStok.text.toString().toIntOrNull() ?: 0
            var barcode = binding.etBarcode.text.toString().trim()
            val deskripsi = binding.etDeskripsi.text.toString().trim()
            
            if (nama.isEmpty() || kategori.isEmpty()) {
                binding.etNama.error = "Nama dan kategori harus diisi"
                return@setOnClickListener
            }
            
            // ✅ AUTO-GENERATE BARCODE: Jika barcode kosong dan mode tambah produk baru
            val isBarcodeGenerated = barcode.isEmpty() && produk == null
            if (isBarcodeGenerated) {
                barcode = BarcodeGenerator.generateLocalBarcode()
            }
            
            val produkToSave = if (produk != null) {
                // Edit mode
                produk.copy(
                    nama = nama,
                    kategori = kategori,
                    harga = harga,
                    stok = stok,
                    barcode = if (barcode.isEmpty()) null else barcode,
                    deskripsi = if (deskripsi.isEmpty()) null else deskripsi
                )
            } else {
                // Add mode
                Produk(
                    nama = nama,
                    kategori = kategori,
                    harga = harga,
                    stok = stok,
                    barcode = if (barcode.isEmpty()) null else barcode,
                    deskripsi = if (deskripsi.isEmpty()) null else deskripsi
                )
            }
            
            onSave(produkToSave)
            
            // ✅ SHOW PREVIEW: Jika barcode auto-generated, show preview dialog
            if (isBarcodeGenerated && onBarcodeGenerated != null) {
                onBarcodeGenerated.invoke(produkToSave)
            }
            
            dismiss()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
