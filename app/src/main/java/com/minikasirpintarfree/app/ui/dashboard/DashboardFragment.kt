package com.minikasirpintarfree.app.ui.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.model.TransaksiItem
import com.minikasirpintarfree.app.databinding.FragmentDashboardBinding
import com.minikasirpintarfree.app.ui.transaksi.ReceiptItemAdapter
import com.minikasirpintarfree.app.utils.NotificationHelper
import com.minikasirpintarfree.app.utils.PdfGenerator
import com.minikasirpintarfree.app.utils.StoreProfileHelper
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
            val factory = DashboardViewModelFactory(requireActivity().application)
            viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

            NotificationHelper.createNotificationChannel(requireContext())

            setupRecyclerViews()
            setupClickListeners()
            observeViewModel()
            setupGreeting()
            viewModel.checkLowStockNotifications(requireContext())

        } catch (e: Exception) {
            android.util.Log.e("DashboardFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupGreeting() {
        viewModel.loadGreeting(requireContext())
        val currentDate = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(java.util.Date())
        binding.tvDate.text = currentDate
    }

    private fun setupRecyclerViews() {
        bestSellerAdapter = BestSellerAdapter()
        binding.recyclerBestSeller.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bestSellerAdapter
        }

        recentTransaksiAdapter = RecentTransaksiAdapter { transaksi ->
            showTransaksiDetail(transaksi)
        }
        binding.recyclerRecentTransaksi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentTransaksiAdapter
        }
    }

    private fun setupClickListeners() {
        binding.cardTambahProduk.setOnClickListener {
            findNavController().navigate(R.id.produkFragment)
        }

        binding.cardScanBarcode.setOnClickListener {
            Toast.makeText(requireContext(), "Fitur segera hadir!", Toast.LENGTH_SHORT).show()
        }

        binding.tvSeeAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_transactionHistoryFragment)
        }
    }

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
        viewModel.greetingText.observe(viewLifecycleOwner) { greeting ->
            binding.tvGreeting.text = greeting
        }

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
        }

        viewModel.bestSellingProducts.observe(viewLifecycleOwner) { products ->
            if (products.isEmpty()) {
                binding.recyclerBestSeller.visibility = View.GONE
                binding.tvEmptyBestSeller.visibility = View.VISIBLE
            } else {
                binding.recyclerBestSeller.visibility = View.VISIBLE
                binding.tvEmptyBestSeller.visibility = View.GONE
                bestSellerAdapter.submitList(products.take(3))
            }
        }

        viewModel.recentTransaksi.observe(viewLifecycleOwner) { transaksiList ->
            if (transaksiList.isEmpty()) {
                binding.recyclerRecentTransaksi.visibility = View.GONE
                binding.tvEmptyTransaksi.visibility = View.VISIBLE
            } else {
                binding.recyclerRecentTransaksi.visibility = View.VISIBLE
                binding.tvEmptyTransaksi.visibility = View.GONE
                recentTransaksiAdapter.submitList(transaksiList.take(3))
            }
        }
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
        tvStoreAddress.visibility = if (storeProfile.address.isNotEmpty()) {
            tvStoreAddress.text = storeProfile.address
            View.VISIBLE
        } else {
            View.GONE
        }
        tvStorePhone.visibility = if (storeProfile.phone.isNotEmpty()) {
            tvStorePhone.text = storeProfile.phone
            View.VISIBLE
        } else {
            View.GONE
        }

        tvDate.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", transaksi.tanggal)
        tvTransactionNo.text = "#TRX-${transaksi.id.toString().padStart(3, '0')}"

        recyclerViewItems.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerViewItems.adapter = ReceiptItemAdapter(items)

        tvTotal.text = formatCurrency(transaksi.totalHarga)
        tvPaid.text = formatCurrency(transaksi.uangDiterima)
        tvChange.text = formatCurrency(transaksi.kembalian)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        btnSavePdf.setOnClickListener { saveReceiptAsPdf(transaksi, items) }
        btnShare.setOnClickListener {
            val receiptText = buildReceiptText(transaksi, items)
            shareReceipt(receiptText)
        }
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun buildReceiptText(transaksi: Transaksi, items: List<TransaksiItem>): String {
        val sb = StringBuilder()
        val storeProfile = StoreProfileHelper.getStoreProfile(requireContext())
        sb.appendLine("=== ${storeProfile.name.uppercase(Locale.ROOT)} ===")
        if (storeProfile.address.isNotEmpty()) sb.appendLine(storeProfile.address)
        if (storeProfile.phone.isNotEmpty()) sb.appendLine(storeProfile.phone)
        sb.appendLine("======================")
        sb.appendLine("Tanggal: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", transaksi.tanggal)}")
        sb.appendLine("No: #TRX-${transaksi.id.toString().padStart(3, '0')}")
        sb.appendLine("----------------------")
        items.forEach {
            sb.appendLine("${it.namaProduk}")
            sb.appendLine("  ${it.quantity}x ${formatCurrency(it.harga)} = ${formatCurrency(it.subtotal)}")
        }
        sb.appendLine("----------------------")
        sb.appendLine("Total: ${formatCurrency(transaksi.totalHarga)}")
        sb.appendLine("Bayar: ${formatCurrency(transaksi.uangDiterima)}")
        sb.appendLine("Kembali: ${formatCurrency(transaksi.kembalian)}")
        sb.appendLine("======================")
        sb.appendLine("Terima kasih!")
        return sb.toString()
    }

    private fun saveReceiptAsPdf(transaksi: Transaksi, items: List<TransaksiItem>) {
        try {
            val pdfPath = PdfGenerator.generateReceipt(requireContext(), transaksi, items)
            Toast.makeText(requireContext(), "✅ Struk disimpan: $pdfPath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
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