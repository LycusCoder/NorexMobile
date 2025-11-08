package com.minikasirpintarfree.app.ui.produk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.databinding.ActivityAddEditProdukBinding
import com.minikasirpintarfree.app.utils.BarcodeGenerator
import kotlinx.coroutines.launch

class AddEditProdukActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditProdukBinding
    private var produk: Produk? = null
    private var prefillBarcode: String? = null
    private lateinit var repository: ProdukRepository
    
    companion object {
        const val EXTRA_PRODUK = "extra_produk"
        const val EXTRA_BARCODE = "extra_barcode"
        const val EXTRA_MODE = "extra_mode"
        const val MODE_ADD = "mode_add"
        const val MODE_EDIT = "mode_edit"
        const val RESULT_BARCODE_GENERATED = "result_barcode_generated"
        
        fun newIntent(
            context: Context,
            produk: Produk? = null,
            prefillBarcode: String? = null
        ): Intent {
            return Intent(context, AddEditProdukActivity::class.java).apply {
                putExtra(EXTRA_PRODUK, produk)
                putExtra(EXTRA_BARCODE, prefillBarcode)
                putExtra(EXTRA_MODE, if (produk != null) MODE_EDIT else MODE_ADD)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize repository
        val database = AppDatabase.getDatabase(this)
        repository = ProdukRepository(database.produkDao())
        
        // Get data from intent
        produk = intent.getParcelableExtra(EXTRA_PRODUK)
        prefillBarcode = intent.getStringExtra(EXTRA_BARCODE)
        
        setupToolbar()
        setupUI()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Set title based on mode
        binding.toolbar.title = if (produk != null) "Edit Produk" else "Tambah Produk"
        
        // Handle back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupUI() {
        if (produk != null) {
            // Edit mode - fill existing data
            binding.etNama.setText(produk!!.nama)
            binding.etKategori.setText(produk!!.kategori)
            binding.etHarga.setText(produk!!.harga.toInt().toString())
            binding.etStok.setText(produk!!.stok.toString())
            binding.etBarcode.setText(produk!!.barcode ?: "")
            binding.etDeskripsi.setText(produk!!.deskripsi ?: "")
            
            // Update FAB text
            binding.fabSave.text = "Update Produk"
        } else {
            // Add mode
            // Pre-fill barcode if provided (dari scan)
            if (!prefillBarcode.isNullOrEmpty()) {
                binding.etBarcode.setText(prefillBarcode)
                // Focus ke nama produk
                binding.etNama.requestFocus()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.fabSave.setOnClickListener {
            saveProduk()
        }
    }
    
    private fun saveProduk() {
        val nama = binding.etNama.text.toString().trim()
        val kategori = binding.etKategori.text.toString().trim()
        val hargaStr = binding.etHarga.text.toString().trim()
        val stokStr = binding.etStok.text.toString().trim()
        var barcode = binding.etBarcode.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()
        
        // Validation
        when {
            nama.isEmpty() -> {
                binding.etNama.error = "Nama produk harus diisi"
                binding.etNama.requestFocus()
                return
            }
            kategori.isEmpty() -> {
                binding.etKategori.error = "Kategori harus diisi"
                binding.etKategori.requestFocus()
                return
            }
            hargaStr.isEmpty() -> {
                binding.etHarga.error = "Harga harus diisi"
                binding.etHarga.requestFocus()
                return
            }
            stokStr.isEmpty() -> {
                binding.etStok.error = "Stok harus diisi"
                binding.etStok.requestFocus()
                return
            }
        }
        
        val harga = hargaStr.toDoubleOrNull() ?: 0.0
        val stok = stokStr.toIntOrNull() ?: 0
        
        // Auto-generate barcode jika kosong (mode tambah produk baru)
        val isBarcodeGenerated = barcode.isEmpty() && produk == null
        if (isBarcodeGenerated) {
            barcode = BarcodeGenerator.generateLocalBarcode()
        }
        
        // Create or update produk
        val produkToSave = if (produk != null) {
            // Edit mode
            produk!!.copy(
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
        
        // Save to database
        lifecycleScope.launch {
            try {
                if (produk != null) {
                    // Update existing produk
                    repository.update(produkToSave)
                    Toast.makeText(
                        this@AddEditProdukActivity,
                        "✅ Produk berhasil diupdate",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    // Insert new produk
                    repository.insert(produkToSave)
                    Toast.makeText(
                        this@AddEditProdukActivity,
                        "✅ Produk berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Jika barcode auto-generated, return produk untuk preview
                    if (isBarcodeGenerated) {
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_BARCODE_GENERATED, produkToSave)
                        }
                        setResult(RESULT_OK, resultIntent)
                    }
                    
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@AddEditProdukActivity,
                    "❌ Gagal menyimpan produk: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}