package com.example.barcodescanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.data.BarcodeRecord
import com.example.barcodescanner.databinding.ItemRecentScanBinding

class RecentScansAdapter : ListAdapter<BarcodeRecord, RecentScansAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRecentScanBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemRecentScanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: BarcodeRecord) {
            binding.textBarcode.text = record.barcode
            binding.textTagType.text = record.tagType
            binding.textUnitType.text = record.unitType
            binding.textCopies.text = "x${record.copies}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BarcodeRecord>() {
        override fun areItemsTheSame(old: BarcodeRecord, new: BarcodeRecord) = old.id == new.id
        override fun areContentsTheSame(old: BarcodeRecord, new: BarcodeRecord) = old == new
    }
}
