package com.minikasirpintarfree.app.ui.transaksi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.databinding.ItemSearchResultBinding
import java.text.NumberFormat
import java.util.Locale

class SearchResultAdapter(private val onItemClick: (Produk) -> Unit) : 
    ListAdapter<Produk, SearchResultAdapter.SearchResultViewHolder>(ProdukDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val produk = getItem(position)
        holder.bind(produk)
        holder.itemView.setOnClickListener { onItemClick(produk) }
    }

    inner class SearchResultViewHolder(private val binding: ItemSearchResultBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            return format.format(amount)
        }

        fun bind(produk: Produk) {
            binding.tvNamaProdukSearch.text = produk.nama
            binding.tvHargaProdukSearch.text = formatCurrency(produk.harga)
        }
    }

    class ProdukDiffCallback : DiffUtil.ItemCallback<Produk>() {
        override fun areItemsTheSame(oldItem: Produk, newItem: Produk): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Produk, newItem: Produk): Boolean {
            return oldItem == newItem
        }
    }
}