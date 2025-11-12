package com.minikasirpintarfree.app.ui.laporan

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.model.BestSellingProduct
import com.minikasirpintarfree.app.databinding.FragmentLaporanBinding
import com.minikasirpintarfree.app.ui.laporan.adapter.ProdukTerlarisAdapter
import com.minikasirpintarfree.app.viewmodel.LaporanViewModel
import com.minikasirpintarfree.app.viewmodel.LaporanViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class LaporanFragment : Fragment() {
    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LaporanViewModel
    private lateinit var produkTerlarisAdapter: ProdukTerlarisAdapter

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
            val factory = LaporanViewModelFactory(requireActivity().application)
            viewModel = ViewModelProvider(this, factory)[LaporanViewModel::class.java]

            setupRecyclerView()
            setupClickListeners()
            observeViewModel()
            loadDefaultReport()
        } catch (e: Exception) {
            android.util.Log.e("LaporanFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        produkTerlarisAdapter = ProdukTerlarisAdapter()
        binding.recyclerProdukTerlaris.apply {
            adapter = produkTerlarisAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
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
                updatePieChart()
                updateBarChart()
            }
        }

        // Observe best-selling products
        lifecycleScope.launch {
            viewModel.produkTerlaris.collect { produkList ->
                updateProdukTerlaris(produkList)
            }
        }
    }

    private fun updateProdukTerlaris(produkList: List<BestSellingProduct>) {
        if (produkList.isEmpty()) {
            binding.tvEmptyProdukTerlaris.visibility = View.VISIBLE
            binding.recyclerProdukTerlaris.visibility = View.GONE
        } else {
            binding.tvEmptyProdukTerlaris.visibility = View.GONE
            binding.recyclerProdukTerlaris.visibility = View.VISIBLE
            produkTerlarisAdapter.submitList(produkList.take(3))
        }
    }

    private fun loadDefaultReport() {
        binding.toggleGroupPeriod.check(R.id.btnHariIni)
        viewModel.loadTransaksiHariIni()
    }

    private fun updateStatistics() {
        val totalPendapatan = viewModel.getTotalPendapatan()
        val totalTransaksi = viewModel.getTotalTransaksi()

        binding.tvTotalPendapatan.text = formatCurrency(totalPendapatan)
        binding.tvTotalTransaksi.text = totalTransaksi.toString()
    }

    private fun updatePieChart() {
        val chartData = viewModel.getChartData()
        if (chartData.isEmpty()) {
            binding.pieChart.visibility = View.GONE
            return
        }
        binding.pieChart.visibility = View.VISIBLE

        val entries = chartData.map { PieEntry(it.second.toFloat(), it.first) }

        val dataSet = PieDataSet(entries, "Grafik Penjualan")

        // --- Chart Modernization ---

        // 1. Resolve theme colors
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val colorOnSurface = typedValue.data

        // 2. Styling dataset
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.sliceSpace = 2f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))

        // 3. Setup Chart
        with(binding.pieChart) {
            data = pieData
            description.isEnabled = false
            legend.textColor = colorOnSurface
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setHoleRadius(58f)
            setTransparentCircleRadius(61f)
            setEntryLabelColor(colorOnSurface)
            setEntryLabelTextSize(10f)

            // 6. Animation & Invalidation
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateBarChart() {
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
