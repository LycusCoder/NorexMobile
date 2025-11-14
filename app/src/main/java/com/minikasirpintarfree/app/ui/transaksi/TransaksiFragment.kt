package com.minikasirpintarfree.app.ui.transaksi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.integration.android.IntentIntegrator
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.model.TransaksiItem
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.databinding.FragmentTransaksiBinding
import com.minikasirpintarfree.app.ui.produk.AddEditProdukDialogFragment
import com.minikasirpintarfree.app.utils.NotificationHelper
import com.minikasirpintarfree.app.utils.PdfGenerator
import com.minikasirpintarfree.app.utils.StoreProfileHelper
import com.minikasirpintarfree.app.viewmodel.SharedViewModel
import com.minikasirpintarfree.app.viewmodel.TransaksiViewModel
import com.minikasirpintarfree.app.viewmodel.TransaksiViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class TransaksiFragment : Fragment() {
    private var _binding: FragmentTransaksiBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransaksiViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var cartAdapter: TransaksiItemAdapter
    private lateinit var searchAdapter: SearchResultAdapter

    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startBarcodeScanner()
        } else {
            Toast.makeText(requireContext(), "Permission kamera diperlukan untuk scan barcode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setupScannerLauncher()

            val database = AppDatabase.getDatabase(requireContext())
            val produkRepository = ProdukRepository(database.produkDao())
            val transaksiRepository = TransaksiRepository(database.transaksiDao())
            viewModel = ViewModelProvider(
                this,
                TransaksiViewModelFactory(transaksiRepository, produkRepository)
            )[TransaksiViewModel::class.java]

            NotificationHelper.createNotificationChannel(requireContext())

            setupRecyclerViews()
            setupSearch()
            setupClickListeners()
            observeViewModel()
            observeSharedViewModel()
        } catch (e: Exception) {
            android.util.Log.e("TransaksiFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupScannerLauncher() {
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = IntentIntegrator.parseActivityResult(
                    IntentIntegrator.REQUEST_CODE,
                    result.resultCode,
                    result.data
                )
                if (scanResult != null && scanResult.contents != null) {
                    val barcode = scanResult.contents
                    viewModel.addProdukByBarcode(barcode)
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Cart Adapter
        cartAdapter = TransaksiItemAdapter(
            onQuantityChange = { item, newQuantity ->
                viewModel.updateItemQuantity(item, newQuantity)
            },
            onItemRemove = { item ->
                viewModel.removeItemFromCart(item)
            }
        )
        binding.recyclerViewCart.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCart.adapter = cartAdapter

        // Search Adapter
        searchAdapter = SearchResultAdapter { produk ->
            viewModel.addProductToCart(produk)
            binding.etSearchProduk.text?.clear()
            binding.rvSearchResults.visibility = View.GONE
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun setupSearch() {
        binding.etSearchProduk.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchProduk(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnScanBarcode.setOnClickListener {
            startScan()
        }

        binding.btnBayar.setOnClickListener {
            showPaymentDialog()
        }

        binding.btnClearCart.setOnClickListener {
            viewModel.clearCart()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.cartItems.collect { items: List<TransaksiItem> ->
                cartAdapter.submitList(items)
                val isEmpty = items.isEmpty()
                binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.btnBayar.isEnabled = !isEmpty
                binding.btnClearCart.isEnabled = !isEmpty
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            searchAdapter.submitList(results)
            binding.rvSearchResults.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalHarga.observe(viewLifecycleOwner) { total: Double ->
            binding.tvTotalHarga.text = formatCurrency(total)
        }

        viewModel.kembalian.observe(viewLifecycleOwner) { kembalian: Double ->
            binding.tvKembalian.text = formatCurrency(kembalian)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message: String ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message: String ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.productNotFound.observe(viewLifecycleOwner) { barcode: String ->
            showProductNotFoundDialog(barcode)
        }
    }

    private fun observeSharedViewModel() {
        sharedViewModel.startScanEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                startScan()
            }
        }
    }

    private fun startScan() {
        if (checkCameraPermission()) {
            startBarcodeScanner()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startBarcodeScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scan barcode produk")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)

        scannerLauncher.launch(integrator.createScanIntent())
    }

    private fun showProductNotFoundDialog(barcode: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Produk Tidak Ditemukan")
            .setMessage("Produk dengan barcode [$barcode] tidak ditemukan.\n\nTambah produk baru dengan barcode ini?")
            .setPositiveButton("Ya") { _, _ ->
                showAddProdukDialog(barcode)
            }
            .setNegativeButton("Tidak", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showAddProdukDialog(barcode: String) {
        val dialog = AddEditProdukDialogFragment(
            produk = null,
            onSave = { newProduk ->
                lifecycleScope.launch {
                    val success = viewModel.insertProdukAndAddToCart(newProduk)
                    if (success) {
                        NotificationHelper.createNotificationChannel(requireContext())
                        NotificationHelper.showProductAddedNotification(
                            requireContext(),
                            newProduk.nama
                        )
                    }
                }
            },
            prefillBarcode = barcode
        )
        dialog.show(parentFragmentManager, "AddProdukDialog")
    }

    private fun showPaymentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        val etUang = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUangDiterima)
        val layoutKembalian = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutKembalian)
        val tvKembalian = dialogView.findViewById<android.widget.TextView>(R.id.tvKembalian)
        val btnBayar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBayar)
        val btnBatal = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatal)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val totalHarga = viewModel.totalHarga.value ?: 0.0

        etUang.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val uangDiterima = s.toString().toDoubleOrNull() ?: 0.0
                if (uangDiterima > 0) {
                    val kembalian = uangDiterima - totalHarga
                    if (kembalian >= 0) {
                        layoutKembalian.visibility = View.VISIBLE
                        tvKembalian.text = formatCurrency(kembalian)
                        btnBayar.isEnabled = true
                    } else {
                        layoutKembalian.visibility = View.GONE
                        btnBayar.isEnabled = false
                    }
                } else {
                    layoutKembalian.visibility = View.GONE
                    btnBayar.isEnabled = false
                }
            }
        })

        btnBayar.setOnClickListener {
            val uang = etUang.text.toString().toDoubleOrNull() ?: 0.0
            viewModel.setUangDiterima(uang)
            viewModel.processTransaksi { transaksi ->
                NotificationHelper.showTransactionSuccessNotification(requireContext(), transaksi.totalHarga)
                showReceiptDialog(transaksi)
            }
            dialog.dismiss()
        }

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            etUang.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etUang, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        etUang.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val uang = etUang.text.toString().toDoubleOrNull() ?: 0.0
                if (uang >= totalHarga) {
                    btnBayar.performClick()
                }
                true
            } else {
                false
            }
        }

        dialog.show()
    }

    private fun showReceiptDialog(transaksi: Transaksi) {
        val items = viewModel.getTransaksiItems(transaksi)
        showModernReceiptDialog(transaksi, items)
    }

    private fun showModernReceiptDialog(transaksi: Transaksi, items: List<TransaksiItem>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt_modern, null)

        val storeProfile = StoreProfileHelper.getStoreProfile(requireContext())

        val tvStoreName = dialogView.findViewById<android.widget.TextView>(R.id.tvStoreName)
        val tvStoreAddress = dialogView.findViewById<android.widget.TextView>(R.id.tvStoreAddress)
        val tvStorePhone = dialogView.findViewById<android.widget.TextView>(R.id.tvStorePhone)
        val tvDate = dialogView.findViewById<android.widget.TextView>(R.id.tvDate)
        val tvTransactionNo = dialogView.findViewById<android.widget.TextView>(R.id.tvTransactionNo)
        val recyclerViewItems = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewItems)
        val tvTotal = dialogView.findViewById<android.widget.TextView>(R.id.tvTotal)
        val tvPaid = dialogView.findViewById<android.widget.TextView>(R.id.tvPaid)
        val tvChange = dialogView.findViewById<android.widget.TextView>(R.id.tvChange)
        val btnSavePdf = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSavePdf)
        val btnShare = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShare)
        val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose)

        tvStoreName.text = storeProfile.name
        if (storeProfile.address.isNotEmpty()) {
            tvStoreAddress.text = storeProfile.address
            tvStoreAddress.visibility = View.VISIBLE
        }
        if (storeProfile.phone.isNotEmpty()) {
            tvStorePhone.text = storeProfile.phone
            tvStorePhone.visibility = View.VISIBLE
        }

        tvDate.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", transaksi.tanggal)
        tvTransactionNo.text = "#TRX-${transaksi.id.toString().padStart(3, '0')}"

        recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewItems.adapter = ReceiptItemAdapter(items)

        tvTotal.text = formatCurrency(transaksi.totalHarga)
        tvPaid.text = formatCurrency(transaksi.uangDiterima)
        tvChange.text = formatCurrency(transaksi.kembalian)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnSavePdf.setOnClickListener {
            saveReceiptAsPdf(transaksi, items)
        }

        btnShare.setOnClickListener {
            val receiptText = buildReceiptText(transaksi, items)
            shareReceipt(receiptText)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun buildReceiptText(transaksi: Transaksi, items: List<TransaksiItem>): String {
        val sb = StringBuilder()
        sb.appendLine("=== STRUK TRANSAKSI ===")
        sb.appendLine("Tanggal: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", transaksi.tanggal)}")
        sb.appendLine("----------------------")
        items.forEach { item ->
            sb.appendLine("${item.namaProduk}")
            sb.appendLine("  ${item.quantity}x ${formatCurrency(item.harga)} = ${formatCurrency(item.subtotal)}")
        }
        sb.appendLine("----------------------")
        sb.appendLine("Total: ${formatCurrency(transaksi.totalHarga)}")
        sb.appendLine("Bayar: ${formatCurrency(transaksi.uangDiterima)}")
        sb.appendLine("Kembali: ${formatCurrency(transaksi.kembalian)}")
        sb.appendLine("=======================")
        return sb.toString()
    }

    private fun saveReceiptAsPdf(transaksi: Transaksi, items: List<TransaksiItem>) {
        try {
            val pdfPath = PdfGenerator.generateReceipt(requireContext(), transaksi, items)
            Toast.makeText(requireContext(), "Struk disimpan: $pdfPath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareReceipt(receiptText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        startActivity(Intent.createChooser(intent, "Bagikan struk"))
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}