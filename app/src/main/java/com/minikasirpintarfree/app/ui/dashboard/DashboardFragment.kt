package com.minikasirpintarfree.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.databinding.FragmentDashboardBinding
import com.minikasirpintarfree.app.ui.login.LoginActivity
import com.minikasirpintarfree.app.utils.NotificationHelper
import com.minikasirpintarfree.app.viewmodel.DashboardViewModel
import com.minikasirpintarfree.app.viewmodel.DashboardViewModelFactory

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    
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
        val currentDate = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        binding.tvDate.text = currentDate
    }
    
    private fun setupClickListeners() {
        // Menu cards removed - navigation now handled by BottomNavigationView only
        // Future feature: Add click listeners for new dashboard features here
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
            if (total > 0) {
                NotificationHelper.showNotification(
                    requireContext(),
                    "Peringatan Stok",
                    "Ada $total produk dengan stok menipis",
                    NotificationHelper.NOTIFICATION_ID_LOW_STOCK
                )
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
