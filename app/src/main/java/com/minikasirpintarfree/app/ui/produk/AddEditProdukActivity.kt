package com.minikasirpintarfree.app.ui.produk

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.databinding.ActivityAddEditProdukBinding
import com.minikasirpintarfree.app.utils.BarcodeGenerator
import com.minikasirpintarfree.app.viewmodel.ProdukViewModel
import com.minikasirpintarfree.app.viewmodel.ProdukViewModelFactory
import kotlinx.coroutines.launch

class AddEditProdukActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditProdukBinding
    private lateinit var viewModel: ProdukViewModel
    private var existingProduk: Produk? = null
    private var prefillBarcode: String? = null
    
    companion object {
        private const val EXTRA_PRODUK = "extra_produk"
        private const val EXTRA_PREFILL_BARCODE = "extra_prefill_barcode"
        const val RESULT_BARCODE_GENERATED = "result_barcode_generated"
        
        fun newIntent(
            context: Context,
            produk: Produk? = null,
            prefillBarcode: String? = null
        ): Intent {
            return Intent(context, AddEditProdukActivity::class.java).apply {
                putExtra(EXTRA_PRODUK, produk)
                putExtra(EXTRA_PREFILL_BARCODE, prefillBarcode)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup ViewModel
        val database = AppDatabase.getDatabase(this)
        val produkRepository = ProdukRepository(database.produkDao())
        viewModel = ViewModelProvider(
            this,
            ProdukViewModelFactory(produkRepository)
        )[ProdukViewModel::class.java]
        
        // Get extras
        existingProduk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PRODUK, Produk::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PRODUK)
        }
        prefillBarcode = intent.getStringExtra(EXTRA_PREFILL_BARCODE)
        
        setupToolbar()
        setupFields()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        if (existingProduk != null) {
            binding.toolbar.title = "Edit Produk"
        } else {
            binding.toolbar.title = "Tambah Produk"
        }
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupFields() {
        existingProduk?.let { produk ->
            // Edit mode - populate fields
            binding.etNama.setText(produk.nama)
            binding.etKategori.setText(produk.kategori)
            binding.etHarga.setText(produk.harga.toString())
            binding.etStok.setText(produk.stok.toString())
            binding.etBarcode.setText(produk.barcode ?: "")
            binding.etDeskripsi.setText(produk.deskripsi ?: "")
        } ?: run {
            // Add mode - prefill barcode if available
            if (!prefillBarcode.isNullOrEmpty()) {
                binding.etBarcode.setText(prefillBarcode)
                // Focus ke field nama
                binding.etNama.requestFocus()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.fabSave.setOnClickListener {
            saveProduk()
        }
    }
    
    private fun observeViewModel() {
        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        viewModel.successMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        if (nama.isEmpty()) {
            binding.etNama.error = "Nama produk harus diisi"
            binding.etNama.requestFocus()
            return
        }
        
        if (kategori.isEmpty()) {
            binding.etKategori.error = "Kategori harus diisi"
            binding.etKategori.requestFocus()
            return
        }
        
        val harga = hargaStr.toDoubleOrNull()
        if (harga == null || harga <= 0) {
            binding.etHarga.error = "Harga harus valid dan lebih dari 0"
            binding.etHarga.requestFocus()
            return
        }
        
        val stok = stokStr.toIntOrNull()
        if (stok == null || stok < 0) {
            binding.etStok.error = "Stok harus valid dan tidak negatif"
            binding.etStok.requestFocus()
            return
        }
        
        // Auto-generate barcode if empty (only for new products)
        val isBarcodeGenerated = barcode.isEmpty() && existingProduk == null
        if (isBarcodeGenerated) {
            barcode = BarcodeGenerator.generateLocalBarcode()
        }
        
        // Create or update produk
        val produkToSave = existingProduk?.copy(
            nama = nama,
            kategori = kategori,
            harga = harga,
            stok = stok,
            barcode = if (barcode.isEmpty()) null else barcode,
            deskripsi = if (deskripsi.isEmpty()) null else deskripsi
        ) ?: Produk(
            nama = nama,
            kategori = kategori,
            harga = harga,
            stok = stok,
            barcode = if (barcode.isEmpty()) null else barcode,
            deskripsi = if (deskripsi.isEmpty()) null else deskripsi
        )
        
        // Save to database
        lifecycleScope.launch {
            try {
                if (existingProduk != null) {
                    viewModel.updateProduk(produkToSave)
                } else {
                    viewModel.insertProduk(produkToSave)
                }
                
                // If barcode was generated, pass it back for preview
                if (isBarcodeGenerated) {
                    val resultIntent = Intent().apply {
                        putExtra(RESULT_BARCODE_GENERATED, produkToSave)
                    }
                    setResult(RESULT_OK, resultIntent)
                } else {
                    setResult(RESULT_OK)
                }
                
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddEditProdukActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}