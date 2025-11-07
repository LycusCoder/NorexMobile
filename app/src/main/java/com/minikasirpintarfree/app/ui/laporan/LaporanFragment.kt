package com.minikasirpintarfree.app.ui.laporan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.databinding.FragmentLaporanBinding
import com.minikasirpintarfree.app.viewmodel.LaporanViewModel
import com.minikasirpintarfree.app.viewmodel.LaporanViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class LaporanFragment : Fragment() {
    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LaporanViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val database = AppDatabase.getDatabase(requireContext())
            val transaksiRepository = TransaksiRepository(database.transaksiDao())
            viewModel = ViewModelProvider(
                this,
                LaporanViewModelFactory(transaksiRepository)
            )[LaporanViewModel::class.java]
            
            setupClickListeners()
            observeViewModel()
            loadDefaultReport()
        } catch (e: Exception) {
            android.util.Log.e("LaporanFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupClickListeners() {
        binding.radioGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioHariIni -> viewModel.loadTransaksiHariIni()
                R.id.radioMingguIni -> viewModel.loadTransaksiMingguIni()
                R.id.radioBulanIni -> viewModel.loadTransaksiBulanIni()
            }
        }
        
        binding.btnExportPdf.setOnClickListener {
            exportReportToPdf()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.transaksiList.collect { _ ->
                updateStatistics()
                updateChart()
            }
        }
    }
    
    private fun loadDefaultReport() {
        viewModel.loadTransaksiHariIni()
    }
    
    private fun updateStatistics() {
        val totalPendapatan = viewModel.getTotalPendapatan()
        val totalTransaksi = viewModel.getTotalTransaksi()
        
        binding.tvTotalPendapatan.text = formatCurrency(totalPendapatan)
        binding.tvTotalTransaksi.text = totalTransaksi.toString()
    }
    
    private fun updateChart() {
        val chartData = viewModel.getChartData()
        val entries = chartData.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "Pendapatan")
        dataSet.color = resources.getColor(R.color.orange, null)
        
        val barData = BarData(dataSet)
        binding.barChart.data = barData
        binding.barChart.description.text = ""
        binding.barChart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index < chartData.size) chartData[index].first else ""
            }
        }
        binding.barChart.invalidate()
    }
    
    private fun exportReportToPdf() {
        Toast.makeText(requireContext(), "Fitur ekspor PDF akan segera tersedia", Toast.LENGTH_SHORT).show()
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
