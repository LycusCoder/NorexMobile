package com.minikasirpintarfree.app.ui.produk

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.model.Produk
import com.minikasirpintarfree.app.databinding.ItemProdukBinding
import java.text.NumberFormat
import java.util.Locale

class ProdukAdapter(
    private val onItemClick: (Produk) -> Unit,
    private val onItemDelete: (Produk) -> Unit
) : ListAdapter<Produk, ProdukAdapter.ProdukViewHolder>(ProdukDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdukViewHolder {
        val binding = ItemProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProdukViewHolder(binding, onItemClick, onItemDelete)
    }
    
    override fun onBindViewHolder(holder: ProdukViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ProdukViewHolder(
        private val binding: ItemProdukBinding,
        private val onItemClick: (Produk) -> Unit,
        private val onItemDelete: (Produk) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(produk: Produk) {
            binding.tvNamaProduk.text = produk.nama
            binding.tvKategori.text = produk.kategori
            binding.tvHarga.text = formatCurrency(produk.harga)
            binding.tvStok.text = "Stok: ${produk.stok}"
            
            if (produk.stok <= 10) {
                binding.tvStok.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red))
            } else {
                // Resolve colorOnSurface from the current theme to reset the text color
                val typedValue = TypedValue()
                val theme = binding.root.context.theme
                theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                binding.tvStok.setTextColor(typedValue.data)
            }
            
            binding.root.setOnClickListener {
                onItemClick(produk)
            }
            
            binding.btnDelete.setOnClickListener {
                onItemDelete(produk)
            }
        }
        
        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            return format.format(amount)
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
