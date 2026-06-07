package com.example.barcodescanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.data.BarcodeRecord
import com.example.barcodescanner.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val onItemClick: (BarcodeRecord) -> Unit) : ListAdapter<BarcodeRecord, HistoryAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemHistoryBinding, private val onItemClick: (BarcodeRecord) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        fun bind(record: BarcodeRecord) {
            binding.textBarcode.text = record.barcode
            binding.textTagUnit.text = "${record.tagType} / ${record.unitType}"
            binding.textCopies.text = "Copies: ${record.copies}"
            binding.textTimestamp.text = dateFormat.format(Date(record.timestamp))
            binding.textPrice.text = if (record.price.isNullOrEmpty()) "Price: —" else "Price: ${record.price}"
            binding.textCustomEng.text = record.customEng ?: ""
            binding.textCustomAra.text = record.customAra ?: ""
            binding.root.setOnClickListener { onItemClick(record) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BarcodeRecord>() {
        override fun areItemsTheSame(old: BarcodeRecord, new: BarcodeRecord) = old.id == new.id
        override fun areContentsTheSame(old: BarcodeRecord, new: BarcodeRecord) = old == new
    }
}
