package com.minikasirpintarfree.app.ui.produk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.integration.android.IntentIntegrator
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.databinding.FragmentProdukBinding
import com.minikasirpintarfree.app.utils.NotificationHelper
import com.minikasirpintarfree.app.viewmodel.ProdukViewModel
import com.minikasirpintarfree.app.viewmodel.ProdukViewModelFactory
import kotlinx.coroutines.launch

class ProdukFragment : Fragment() {
    private var _binding: FragmentProdukBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProdukViewModel
    private lateinit var adapter: ProdukAdapter
    private val CAMERA_PERMISSION_CODE = 100

    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>
    private lateinit var addEditProdukLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProdukBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setupScannerLauncher()
            setupAddEditProdukLauncher()

            // Perbaikan: Inisialisasi ViewModel menggunakan factory dengan Application context
            val factory = ProdukViewModelFactory(requireActivity().application)
            viewModel = ViewModelProvider(this, factory)[ProdukViewModel::class.java]

            setupRecyclerView()
            setupClickListeners()
            observeViewModel()
            checkLowStockNotifications()
        } catch (e: Exception) {
            android.util.Log.e("ProdukFragment", "Error in onViewCreated", e)
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
                if (scanResult != null) {
                    if (scanResult.contents == null) {
                        Toast.makeText(requireContext(), "Scan dibatalkan", Toast.LENGTH_SHORT).show()
                    } else {
                        val barcode = scanResult.contents
                        viewModel.getProdukByBarcode(
                            barcode,
                            onSuccess = { produk: Produk ->
                                Toast.makeText(requireContext(), "Produk ditemukan: ${produk.nama}", Toast.LENGTH_SHORT).show()
                                showEditProdukActivity(produk)
                            },
                            onError = {
                                Toast.makeText(requireContext(), "Produk dengan barcode $barcode tidak ditemukan", Toast.LENGTH_SHORT).show()
                                showAddProdukActivity(prefillBarcode = barcode)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupAddEditProdukLauncher() {
        addEditProdukLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val produk = result.data?.getParcelableExtra<Produk>(AddEditProdukActivity.RESULT_BARCODE_GENERATED)
                if (produk != null) {
                    showBarcodePreview(produk)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProdukAdapter(
            onItemClick = { produk ->
                showEditProdukActivity(produk)
            },
            onItemDelete = { produk ->
                viewModel.deleteProduk(produk)
            }
        )
        binding.recyclerViewProduk.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewProduk.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabAddProduk.setOnClickListener {
            showAddProdukActivity()
        }

        binding.btnScanBarcode.setOnClickListener {
            if (checkCameraPermission()) {
                startBarcodeScanner()
            } else {
                requestCameraPermission()
            }
        }

        binding.etSearch.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchProduk(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.loadAllProduk()
                } else {
                    viewModel.searchProduk(newText)
                }
                return true
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.produkList.collect { list: List<Produk> ->
                adapter.submitList(list)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message: String ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message: String ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLowStockNotifications() {
        lifecycleScope.launch {
            viewModel.produkStokMenipis.collect { produkList: List<Produk> ->
                produkList.forEach { produk: Produk ->
                    if (produk.stok <= 10) {
                        // Cek apakah sudah ada notifikasi untuk produk ini hari ini
                        val hasNotificationToday = NotificationHelper.hasLowStockNotificationToday(
                            requireContext(),
                            produk.nama
                        )
                        
                        // Hanya kirim notifikasi jika belum ada notifikasi hari ini
                        if (!hasNotificationToday) {
                            NotificationHelper.createNotificationChannel(requireContext())
                            NotificationHelper.showLowStockNotification(
                                requireContext(),
                                produk.nama,
                                produk.stok
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBarcodeScanner()
        } else {
            Toast.makeText(requireContext(), "Permission kamera diperlukan untuk scan barcode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBarcodeScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scan barcode produk")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(false)

        scannerLauncher.launch(integrator.createScanIntent())
    }

    private fun showAddProdukActivity(prefillBarcode: String? = null) {
        val intent = AddEditProdukActivity.newIntent(
            requireContext(),
            produk = null,
            prefillBarcode = prefillBarcode
        )
        addEditProdukLauncher.launch(intent)
    }

    private fun showEditProdukActivity(produk: Produk) {
        val intent = AddEditProdukActivity.newIntent(
            requireContext(),
            produk = produk
        )
        addEditProdukLauncher.launch(intent)
    }

    private fun showBarcodePreview(produk: Produk) {
        try {
            val barcodeBitmap = com.minikasirpintarfree.app.utils.BarcodeGenerator.generateBarcodeBitmap(
                produk.barcode ?: "",
                400,
                200
            )

            if (barcodeBitmap != null) {
                val previewDialog = BarcodePreviewDialogFragment(produk, barcodeBitmap)
                previewDialog.show(parentFragmentManager, "BarcodePreviewDialog")
            } else {
                Toast.makeText(
                    requireContext(),
                    "❌ Gagal generate barcode image",
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}