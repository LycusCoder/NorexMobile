package com.minikasirpintarfree.app.ui.laporan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minikasirpintarfree.app.data.model.BestSellingProduct
import com.minikasirpintarfree.app.databinding.ItemProdukTerlarisBinding
import java.text.NumberFormat
import java.util.Locale

class ProdukTerlarisAdapter : ListAdapter<BestSellingProduct, ProdukTerlarisAdapter.ProdukViewHolder>(PRODUK_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdukViewHolder {
        val binding = ItemProdukTerlarisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProdukViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProdukViewHolder, position: Int) {
        val produk = getItem(position)
        if (produk != null) {
            holder.bind(produk, position + 1)
        }
    }

    inner class ProdukViewHolder(private val binding: ItemProdukTerlarisBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(produk: BestSellingProduct, rank: Int) {
            binding.tvRanking.text = rank.toString()
            binding.tvNamaProduk.text = produk.namaProduk

            val totalTerjual = produk.totalTerjual
            val totalPendapatan = produk.totalPendapatan
            val formattedPendapatan = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalPendapatan)

            binding.tvDetailTerjual.text = "$totalTerjual terjual • $formattedPendapatan"
        }
    }

    companion object {
        private val PRODUK_COMPARATOR = object : DiffUtil.ItemCallback<BestSellingProduct>() {
            override fun areItemsTheSame(oldItem: BestSellingProduct, newItem: BestSellingProduct): Boolean {
                return oldItem.produkId == newItem.produkId
            }

            override fun areContentsTheSame(oldItem: BestSellingProduct, newItem: BestSellingProduct): Boolean {
                return oldItem == newItem
            }
        }
    }
}
