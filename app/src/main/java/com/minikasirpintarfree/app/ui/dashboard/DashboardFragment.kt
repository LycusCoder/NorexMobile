package com.minikasirpintarfree.app.ui.dashboard

import android.app.AlertDialog // Pastikan pakai android.app.AlertDialog atau androidx.appcompat.app.AlertDialog
import android.content.Intent // Digunakan untuk shareReceipt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController // Tetap ada, meski tidak dipakai di sini
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager // Sudah ada, tidak perlu di-import ulang
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.model.TransaksiItem
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.databinding.FragmentDashboardBinding
import com.minikasirpintarfree.app.ui.dashboard.BestSellerAdapter
import com.minikasirpintarfree.app.ui.dashboard.RecentTransaksiAdapter
import com.minikasirpintarfree.app.ui.transaksi.ReceiptItemAdapter // NEW: untuk struk modern
import com.minikasirpintarfree.app.utils.NotificationHelper
import com.minikasirpintarfree.app.utils.PdfGenerator // NEW: untuk save PDF
import com.minikasirpintarfree.app.utils.StoreProfileHelper // NEW: untuk info toko
import com.minikasirpintarfree.app.viewmodel.DashboardViewModel
import com.minikasirpintarfree.app.viewmodel.DashboardViewModelFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private lateinit var bestSellerAdapter: BestSellerAdapter
    private lateinit var recentTransaksiAdapter: RecentTransaksiAdapter
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val produkRepository = ProdukRepository(database.produkDao())
            val transaksiRepository = TransaksiRepository(database.transaksiDao())

            viewModel = ViewModelProvider(
                this,
                DashboardViewModelFactory(produkRepository, transaksiRepository)
            )[DashboardViewModel::class.java]

            NotificationHelper.createNotificationChannel(requireContext())

            setupGreeting()
            setupRecyclerViews()
            setupClickListeners()
            observeViewModel()
        } catch (e: Exception) {
            android.util.Log.e("DashboardFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupGreeting() {
        // Get nama toko from SharedPreferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val namaToko = prefs.getString("nama_toko", "Mini Kasir Pintar") ?: "Mini Kasir Pintar"
        binding.tvGreeting.text = "Halo, $namaToko 👋"

        // Set current date
        val currentDate = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(java.util.Date())
        binding.tvDate.text = currentDate
    }

    private fun setupRecyclerViews() {
        // Setup Best Seller RecyclerView
        bestSellerAdapter = BestSellerAdapter()
        binding.recyclerBestSeller.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bestSellerAdapter
        }

        // Setup Recent Transaksi RecyclerView
        recentTransaksiAdapter = RecentTransaksiAdapter { transaksi ->
            showTransaksiDetail(transaksi) // Call rewritten function
        }
        binding.recyclerRecentTransaksi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentTransaksiAdapter
        }
    }

    private fun setupClickListeners() {
        // Menu cards removed - navigation now handled by BottomNavigationView only
        // Future feature: Add click listeners for new dashboard features here
    }

    // ✅ PERUBAHAN 1: Rewrite showTransaksiDetail untuk memanggil dialog modern
    private fun showTransaksiDetail(transaksi: Transaksi) {
        try {
            val itemsType = object : TypeToken<List<TransaksiItem>>() {}.type
            val items: List<TransaksiItem> = gson.fromJson(transaksi.items, itemsType)
            showModernReceiptDialog(transaksi, items)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal memuat detail transaksi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.totalProduk.observe(viewLifecycleOwner) { total: Int ->
            binding.tvTotalProduk.text = total.toString()
        }

        viewModel.totalTransaksiHariIni.observe(viewLifecycleOwner) { total: Int ->
            binding.tvTotalTransaksi.text = total.toString()
        }

        viewModel.totalPendapatanHariIni.observe(viewLifecycleOwner) { total: Double ->
            val formattedPendapatan = if (total >= 1000000) {
                String.format("%.1fJt", total / 1000000)
            } else if (total >= 1000) {
                String.format("%.0fK", total / 1000)
            } else {
                String.format("%.0f", total)
            }
            binding.tvTotalPendapatan.text = formattedPendapatan
        }

        viewModel.stokMenipis.observe(viewLifecycleOwner) { total: Int ->
            binding.tvStokMenipis.text = total.toString()
            // ✅ FIXED: Notification spam dihapus, hanya update UI
        }

        viewModel.bestSellingProducts.observe(viewLifecycleOwner) { products ->
            if (products.isEmpty()) {
                binding.recyclerBestSeller.visibility = View.GONE
                binding.tvEmptyBestSeller.visibility = View.VISIBLE
            } else {
                binding.recyclerBestSeller.visibility = View.VISIBLE
                binding.tvEmptyBestSeller.visibility = View.GONE
                bestSellerAdapter.submitList(products)
            }
        }

        viewModel.recentTransaksi.observe(viewLifecycleOwner) { transaksiList ->
            if (transaksiList.isEmpty()) {
                binding.recyclerRecentTransaksi.visibility = View.GONE
                binding.tvEmptyTransaksi.visibility = View.VISIBLE
            } else {
                binding.recyclerRecentTransaksi.visibility = View.VISIBLE
                binding.tvEmptyTransaksi.visibility = View.GONE
                recentTransaksiAdapter.submitList(transaksiList)
            }
        }
    }

    // ✅ FUNGSI BARU 1: Menampilkan Struk Modern
    private fun showModernReceiptDialog(transaksi: Transaksi, items: List<TransaksiItem>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt_modern, null)

        // Get store profile
        val storeProfile = StoreProfileHelper.getStoreProfile(requireContext())

        // Setup views
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

        // Set store info
        tvStoreName.text = storeProfile.name
        if (storeProfile.address.isNotEmpty()) {
            tvStoreAddress.text = storeProfile.address
            tvStoreAddress.visibility = View.VISIBLE
        } else {
            tvStoreAddress.visibility = View.GONE
        }
        if (storeProfile.phone.isNotEmpty()) {
            tvStorePhone.text = storeProfile.phone
            tvStorePhone.visibility = View.VISIBLE
        } else {
            tvStorePhone.visibility = View.GONE
        }

        // Set transaction info
        tvDate.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", transaksi.tanggal)
        tvTransactionNo.text = "#TRX-${transaksi.id.toString().padStart(3, '0')}"

        // Setup RecyclerView for items
        recyclerViewItems.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerViewItems.adapter = ReceiptItemAdapter(items)

        // Set totals
        tvTotal.text = formatCurrency(transaksi.totalHarga)
        tvPaid.text = formatCurrency(transaksi.uangDiterima)
        tvChange.text = formatCurrency(transaksi.kembalian)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Button actions
        btnSavePdf.setOnClickListener { saveReceiptAsPdf(transaksi, items) }
        btnShare.setOnClickListener {
            val receiptText = buildReceiptText(transaksi, items)
            shareReceipt(receiptText)
        }
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ✅ FUNGSI BARU 2: Format Text untuk Share
    private fun buildReceiptText(transaksi: Transaksi, items: List<TransaksiItem>): String {
        val sb = StringBuilder()
        val storeProfile = StoreProfileHelper.getStoreProfile(requireContext())
        sb.appendLine("=== ${storeProfile.name.uppercase(Locale.ROOT)} ===")
        if (storeProfile.address.isNotEmpty()) {
            sb.appendLine(storeProfile.address)
        }
        if (storeProfile.phone.isNotEmpty()) {
            sb.appendLine(storeProfile.phone)
        }
        sb.appendLine("======================")
        sb.appendLine("Tanggal: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", transaksi.tanggal)}")
        sb.appendLine("No: #TRX-${transaksi.id.toString().padStart(3, '0')}")
        sb.appendLine("----------------------")
        items.forEach { item ->
            sb.appendLine("${item.namaProduk}")
            sb.appendLine("  ${item.quantity}x ${formatCurrency(item.harga)} = ${formatCurrency(item.subtotal)}")
        }
        sb.appendLine("----------------------")
        sb.appendLine("Total: ${formatCurrency(transaksi.totalHarga)}")
        sb.appendLine("Bayar: ${formatCurrency(transaksi.uangDiterima)}")
        sb.appendLine("Kembali: ${formatCurrency(transaksi.kembalian)}")
        sb.appendLine("======================")
        sb.appendLine("Terima kasih!")
        return sb.toString()
    }

    // ✅ FUNGSI BARU 3: Simpan Struk sebagai PDF
    private fun saveReceiptAsPdf(transaksi: Transaksi, items: List<TransaksiItem>) {
        try {
            val pdfPath = PdfGenerator.generateReceipt(requireContext(), transaksi, items)
            Toast.makeText(requireContext(), "✅ Struk disimpan: $pdfPath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ FUNGSI BARU 4: Share Struk
    private fun shareReceipt(receiptText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        startActivity(Intent.createChooser(intent, "Bagikan struk"))
    }

    // ✅ FUNGSI BARU 5: Format Rupiah
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}