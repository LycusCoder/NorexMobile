package com.minikasirpintarfree.app.ui.produk

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.model.Produk
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

    private var isNewProductMode = true // Default to new product

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

        // Perbaikan: Inisialisasi ViewModel menggunakan factory dengan Application context
        val factory = ProdukViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[ProdukViewModel::class.java]

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
        binding.toolbar.title = if (existingProduk != null) "Edit Produk" else "Tambah Produk"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupFields() {
        // Set default mode
        updateBarcodeView(isNewProductMode)

        existingProduk?.let {
            binding.etNama.setText(it.nama)
            binding.etKategori.setText(it.kategori)
            binding.etHarga.setText(it.harga.toString())
            binding.etStok.setText(it.stok.toString())
            binding.etDeskripsi.setText(it.deskripsi ?: "")

            if (!it.barcode.isNullOrEmpty()) {
                isNewProductMode = false
                binding.toggleProductType.check(R.id.btnScanProduct)
                binding.etBarcode.setText(it.barcode)
            }
        } ?: run {
            if (!prefillBarcode.isNullOrEmpty()) {
                isNewProductMode = false
                binding.toggleProductType.check(R.id.btnScanProduct)
                binding.etBarcode.setText(prefillBarcode)
                binding.etNama.requestFocus()
            }
        }
    }

    private fun setupClickListeners() {
        binding.toggleProductType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isNewProductMode = checkedId == R.id.btnNewProduct
                updateBarcodeView(isNewProductMode)
            }
        }

        binding.fabSave.setOnClickListener {
            saveProduk()
        }

        binding.tilBarcode.setEndIconOnClickListener {
            Toast.makeText(this, "Fitur Scan akan segera tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBarcodeView(isNew: Boolean) {
        if (isNew) {
            binding.layoutNewBarcode.visibility = View.VISIBLE
            binding.layoutScanBarcode.visibility = View.GONE
        } else {
            binding.layoutNewBarcode.visibility = View.GONE
            binding.layoutScanBarcode.visibility = View.VISIBLE
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

        if (nama.isEmpty()) {
            binding.etNama.error = "Nama produk harus diisi"
            return
        }
        if (kategori.isEmpty()) {
            binding.etKategori.error = "Kategori harus diisi"
            return
        }
        val harga = hargaStr.toDoubleOrNull()
        if (harga == null || harga <= 0) {
            binding.etHarga.error = "Harga harus valid"
            return
        }
        val stok = stokStr.toIntOrNull()
        if (stok == null || stok < 0) {
            binding.etStok.error = "Stok harus valid"
            return
        }

        var isBarcodeGenerated = false
        if (isNewProductMode && existingProduk == null) {
            barcode = BarcodeGenerator.generateLocalBarcode()
            isBarcodeGenerated = true
        } else if (!isNewProductMode && barcode.isEmpty()) {
            binding.etBarcode.error = "Barcode tidak boleh kosong untuk tipe produk ini"
            return
        }

        val produkToSave = existingProduk?.copy(
            nama = nama, kategori = kategori, harga = harga, stok = stok,
            barcode = if (barcode.isEmpty()) null else barcode,
            deskripsi = if (deskripsi.isEmpty()) null else deskripsi
        ) ?: Produk(
            nama = nama, kategori = kategori, harga = harga, stok = stok,
            barcode = if (barcode.isEmpty()) null else barcode,
            deskripsi = if (deskripsi.isEmpty()) null else deskripsi
        )

        lifecycleScope.launch {
            try {
                if (existingProduk != null) {
                    viewModel.updateProduk(produkToSave)
                } else {
                    viewModel.insertProduk(produkToSave)
                }

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
                Toast.makeText(this@AddEditProdukActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}