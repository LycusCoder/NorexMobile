package com.minikasirpintarfree.app.ui.produk

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.databinding.DialogBarcodePreviewBinding
import com.minikasirpintarfree.app.utils.BarcodeGenerator
import com.minikasirpintarfree.app.utils.BarcodePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class BarcodePreviewDialogFragment(
    private val produk: Produk,
    private val barcodeBitmap: Bitmap
) : DialogFragment() {
    
    private lateinit var binding: DialogBarcodePreviewBinding
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBarcodePreviewBinding.inflate(inflater, container, false)
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
        
        setupViews()
        setupClickListeners()
    }
    
    private fun setupViews() {
        // Set title
        binding.tvTitle.text = "Barcode Produk: ${produk.nama}"
        
        // Set barcode image
        binding.ivBarcode.setImageBitmap(barcodeBitmap)
        
        // Set barcode number
        binding.tvBarcodeNumber.text = produk.barcode ?: ""
        
        // Set product info
        binding.tvProductName.text = produk.nama
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.tvProductPrice.text = formatter.format(produk.harga)
    }
    
    private fun setupClickListeners() {
        // Save to Gallery
        binding.btnSaveToGallery.setOnClickListener {
            saveToGallery()
        }
        
        // Share Barcode
        binding.btnShare.setOnClickListener {
            shareBarcode()
        }
        
        // Print PDF
        binding.btnPrintPdf.setOnClickListener {
            generatePdf()
        }
        
        // Close Dialog
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun saveToGallery() {
        lifecycleScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    BarcodeGenerator.saveBarcodeToGallery(
                        requireContext(),
                        barcodeBitmap,
                        produk.barcode ?: "barcode"
                    )
                }
                
                if (uri != null) {
                    Toast.makeText(
                        requireContext(),
                        "✅ Barcode berhasil disimpan ke Galeri!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Gagal menyimpan barcode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun shareBarcode() {
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    BarcodeGenerator.createBarcodeFile(
                        requireContext(),
                        barcodeBitmap,
                        produk.barcode ?: "barcode"
                    )
                }
                
                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, 
                            "Barcode Produk: ${produk.nama}\n" +
                            "Kode: ${produk.barcode}\n" +
                            "Harga: ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(produk.harga)}"
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    startActivity(Intent.createChooser(shareIntent, "Bagikan Barcode"))
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Gagal membuat file untuk share",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun generatePdf() {
        lifecycleScope.launch {
            try {
                Toast.makeText(
                    requireContext(),
                    "⏳ Membuat PDF...",
                    Toast.LENGTH_SHORT
                ).show()
                
                val pdfFile = withContext(Dispatchers.IO) {
                    BarcodePdfGenerator.generateBarcodePdf(
                        requireContext(),
                        produk,
                        barcodeBitmap
                    )
                }
                
                if (pdfFile != null && pdfFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        pdfFile
                    )
                    
                    // Open PDF dengan print intent
                    val printIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    
                    Toast.makeText(
                        requireContext(),
                        "✅ PDF berhasil dibuat! File: ${pdfFile.name}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    try {
                        startActivity(printIntent)
                    } catch (e: Exception) {
                        // Jika tidak ada PDF viewer, tampilkan lokasi file
                        Toast.makeText(
                            requireContext(),
                            "PDF tersimpan di: ${pdfFile.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Gagal membuat PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}