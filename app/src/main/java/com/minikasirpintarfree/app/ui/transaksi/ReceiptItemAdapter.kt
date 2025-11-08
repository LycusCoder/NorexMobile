package com.minikasirpintarfree.app.ui.transaksi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.model.TransaksiItem
import java.text.NumberFormat
import java.util.Locale

class ReceiptItemAdapter(private val items: List<TransaksiItem>) : 
    RecyclerView.Adapter<ReceiptItemAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemDetail: TextView = view.findViewById(R.id.tvItemDetail)
        val tvItemSubtotal: TextView = view.findViewById(R.id.tvItemSubtotal)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvItemName.text = item.namaProduk
        holder.tvItemDetail.text = "${item.quantity}x ${formatCurrency(item.harga)}"
        holder.tvItemSubtotal.text = formatCurrency(item.subtotal)
    }
    
    override fun getItemCount() = items.size
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount)
    }
}