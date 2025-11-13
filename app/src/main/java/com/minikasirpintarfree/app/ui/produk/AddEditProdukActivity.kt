package com.minikasirpintarfree.app.ui.produk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
    private var imageUri: Uri? = null

    private var isNewProductMode = true // Default to new product

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Izin untuk mengakses galeri ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                imageUri = it
                Glide.with(this)
                    .load(imageUri)
                    .into(binding.ivProdukGambar)
            }
        }
    }

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
        setupFocusListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = if (existingProduk != null) "Edit Produk" else "Tambah Produk"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupFields() {
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
            
            it.gambar?.let {
                imageUri = Uri.parse(it)
                 Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_storefront)
                    .error(R.drawable.ic_storefront)
                    .into(binding.ivProdukGambar)
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

        binding.btnSimpan.setOnClickListener {
            saveProduk()
        }

        binding.btnPilihGambar.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.tilBarcode.setEndIconOnClickListener {
            Toast.makeText(this, "Fitur Scan akan segera tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFocusListeners() {
        val listener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // Delay is to allow the keyboard to come up before we scroll.
                v.postDelayed({
                    // This generic call asks the parent hierarchy to scroll to make the view v visible.
                    // The NestedScrollView will handle this request.
                    val rect = Rect(0, 0, v.width, v.height)
                    v.requestRectangleOnScreen(rect, false)
                }, 200)
            }
        }

        binding.etBarcode.onFocusChangeListener = listener
        binding.etNama.onFocusChangeListener = listener
        binding.etKategori.onFocusChangeListener = listener
        binding.etHarga.onFocusChangeListener = listener
        binding.etStok.onFocusChangeListener = listener
        binding.etDeskripsi.onFocusChangeListener = listener
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Optionally, show a rationale to the user
                Toast.makeText(this, "Izin diperlukan untuk memilih gambar", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
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
            binding.tilNama.error = "Nama produk harus diisi"
            return
        }
        if (kategori.isEmpty()) {
            binding.tilKategori.error = "Kategori harus diisi"
            return
        }
        val harga = hargaStr.toDoubleOrNull()
        if (harga == null || harga <= 0) {
            binding.tilHarga.error = "Harga harus valid"
            return
        }
        val stok = stokStr.toIntOrNull()
        if (stok == null || stok < 0) {
            binding.tilStok.error = "Stok harus valid"
            return
        }

        var isBarcodeGenerated = false
        if (isNewProductMode && existingProduk == null) {
            barcode = BarcodeGenerator.generateLocalBarcode()
            isBarcodeGenerated = true
        } else if (!isNewProductMode && barcode.isEmpty()) {
            binding.tilBarcode.error = "Barcode tidak boleh kosong"
            return
        }

        val produkToSave = existingProduk?.copy(
            nama = nama, kategori = kategori, harga = harga, stok = stok,
            barcode = if (barcode.isEmpty()) null else barcode,
            deskripsi = if (deskripsi.isEmpty()) null else deskripsi,
            gambar = imageUri?.toString()
        ) ?: Produk(
            nama = nama, kategori = kategori, harga = harga, stok = stok,
            barcode = if (barcode.isEmpty()) null else barcode,
            deskripsi = if (deskripsi.isEmpty()) null else deskripsi,
            gambar = imageUri?.toString()
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
