package com.minikasirpintarfree.app.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.minikasirpintarfree.app.data.model.Transaksi
import com.minikasirpintarfree.app.data.model.TransaksiItem
import com.minikasirpintarfree.app.databinding.ItemTransactionHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionHistoryAdapter : ListAdapter<Transaksi, TransactionHistoryAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val gson = Gson()

        fun bind(transaksi: Transaksi) {
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = sdf.format(transaksi.tanggal)
            val formattedTotal = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(transaksi.totalHarga)

            val itemsType = object : TypeToken<List<TransaksiItem>>() {}.type
            val items: List<TransaksiItem> = try {
                gson.fromJson(transaksi.items, itemsType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            val itemCount = items.sumOf { it.quantity }

            binding.tvTransactionId.text = "ID: ${transaksi.id}"
            binding.tvTransactionDate.text = formattedDate
            binding.tvTotalAmount.text = "Total: $formattedTotal"
            binding.tvItemCount.text = "$itemCount item"
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaksi>() {
        override fun areItemsTheSame(oldItem: Transaksi, newItem: Transaksi): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaksi, newItem: Transaksi): Boolean {
            return oldItem == newItem
        }
    }
}