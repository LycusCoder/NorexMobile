package com.minikasirpintarfree.app.ui.laporan

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
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
        binding.toggleGroupPeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnHariIni -> viewModel.loadTransaksiHariIni()
                    R.id.btnMingguIni -> viewModel.loadTransaksiMingguIni()
                    R.id.btnBulanIni -> viewModel.loadTransaksiBulanIni()
                }
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
        // Default to loading today's report
        binding.toggleGroupPeriod.check(R.id.btnHariIni)
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

        // --- FASE 2: Chart Modernization ---

        // 1. Resolve theme colors
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val colorOnSurface = typedValue.data

        // 2. Styling dataset
        dataSet.color = colorPrimary
        dataSet.valueTextColor = colorOnSurface
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        // 3. Setup Chart
        with(binding.barChart) {
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)

            // 4. X-Axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = colorOnSurface
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < chartData.size) chartData[index].first else ""
                    }
                }
            }

            // 5. Y-Axis styling
            axisLeft.apply {
                setDrawGridLines(false)
                textColor = colorOnSurface
                axisMinimum = 0f
            }
            axisRight.isEnabled = false

            // 6. Animation & Invalidation
            animateY(1000)
            invalidate()
        }
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
