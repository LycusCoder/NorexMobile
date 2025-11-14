package com.minikasirpintarfree.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.databinding.FragmentDataManagementBinding
import com.minikasirpintarfree.app.viewmodel.SettingsViewModel
import com.minikasirpintarfree.app.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DataManagementFragment : Fragment() {

    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel

    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                exportProdukToCsv(uri)
            }
        }
    }

    private val importCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                importProdukFromCsv(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val produkRepository = ProdukRepository(database.produkDao())
        val transaksiRepository = TransaksiRepository(database.transaksiDao())
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val factory = SettingsViewModelFactory(produkRepository, transaksiRepository, sharedPreferences)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnExportProduk.setOnClickListener { openFilePickerForExport() }
        binding.btnImportProduk.setOnClickListener { openFilePickerForImport() }

        binding.cardResetProduk.setOnClickListener {
            showResetDialog("Produk") {
                viewModel.resetDataProduk {
                    Toast.makeText(requireContext(), "Data produk telah direset", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.cardResetTransaksi.setOnClickListener {
            showResetDialog("Transaksi") {
                viewModel.resetDataTransaksi {
                    Toast.makeText(requireContext(), "Data transaksi telah direset", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.cardResetAll.setOnClickListener {
            showResetDialog("Semua Data") {
                viewModel.resetAllData {
                    Toast.makeText(requireContext(), "Semua data telah direset", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openFilePickerForExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "produk_export.csv")
        }
        exportCsvLauncher.launch(intent)
    }

    private fun openFilePickerForImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
        }
        importCsvLauncher.launch(intent)
    }

    private fun exportProdukToCsv(uri: Uri) {
        lifecycleScope.launch {
            viewModel.getAllProduk().collect { productList ->
                try {
                    requireActivity().contentResolver.openOutputStream(uri)?.use {
                        val writer = OutputStreamWriter(it)
                        writer.appendLine("Nama,Kategori,Harga,Stok,Barcode")
                        productList.forEach { produk ->
                            writer.appendLine("${produk.nama},${produk.kategori},${produk.harga},${produk.stok},${produk.barcode ?: ""}")
                        }
                        writer.flush()
                        Toast.makeText(requireContext(), "Data produk berhasil diekspor", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal mengekspor data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importProdukFromCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val produkToImport = mutableListOf<Produk>()
                requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    reader.readLine() // Skip header
                    var line: String?
                    while (withContext(Dispatchers.IO) { reader.readLine() }.also { line = it } != null) {
                        val tokens = line!!.split(",")
                        if (tokens.size >= 4) {
                            val barcode = if (tokens.size > 4) tokens[4].trim() else null
                            var shouldAdd = true

                            if (!barcode.isNullOrEmpty()) {
                                val existingProduk = viewModel.getProdukByBarcode(barcode)
                                if (existingProduk != null) {
                                    shouldAdd = false // Barcode already exists, skip
                                }
                            }
                            
                            if (shouldAdd) {
                                val produk = Produk(
                                    nama = tokens[0].trim(),
                                    kategori = tokens[1].trim(),
                                    harga = tokens[2].toDoubleOrNull() ?: 0.0,
                                    stok = tokens[3].toIntOrNull() ?: 0,
                                    barcode = barcode
                                )
                                produkToImport.add(produk)
                            }
                        }
                    }
                }

                if (produkToImport.isNotEmpty()) {
                    viewModel.insertAllProduk(produkToImport)
                    Toast.makeText(requireContext(), "${produkToImport.size} produk baru berhasil diimpor", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Tidak ada produk baru untuk diimpor", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal mengimpor data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResetDialog(dataType: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Data")
            .setMessage("Apakah Anda yakin ingin mereset data $dataType? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Ya, Reset") { _, _ -> onConfirm() }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
