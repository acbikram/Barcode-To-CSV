package com.example.barcodescanner.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.barcodescanner.R
import com.example.barcodescanner.data.BarcodeRecord
import com.example.barcodescanner.databinding.DialogEditRecordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditRecordDialogFragment : DialogFragment() {
    private var _binding: DialogEditRecordBinding? = null
    private val binding get() = _binding!!
    private var record: BarcodeRecord? = null
    private var onSaveCallback: ((BarcodeRecord) -> Unit)? = null

    private val tagTypes = listOf("A4", "4PCS", "VEG", "4PCS_DATE", "4PCS_SAME")
    private val unitTypes = listOf("PCS", "CTN", "PKT", "KGS")

    companion object {
        fun newInstance(record: BarcodeRecord, onSave: (BarcodeRecord) -> Unit): EditRecordDialogFragment {
            return EditRecordDialogFragment().apply {
                this.record = record
                onSaveCallback = onSave
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditRecordBinding.inflate(LayoutInflater.from(context))
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Record")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ -> saveRecord() }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        loadRecordData()
    }

    private fun setupSpinners() {
        val tagAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tagTypes)
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEditTagType.adapter = tagAdapter

        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, unitTypes)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEditUnitType.adapter = unitAdapter
    }

    private fun loadRecordData() {
        record?.let {
            binding.textEditBarcode.text = it.barcode
            binding.editPrice.setText(it.price ?: "")
            binding.editCustomEng.setText(it.customEng ?: "")
            binding.editCustomAra.setText(it.customAra ?: "")
            val tagPos = tagTypes.indexOf(it.tagType).takeIf { p -> p >= 0 } ?: 0
            binding.spinnerEditTagType.setSelection(tagPos)
            val unitPos = unitTypes.indexOf(it.unitType).takeIf { p -> p >= 0 } ?: 0
            binding.spinnerEditUnitType.setSelection(unitPos)
            setupCopiesSpinner(it.copies)
        }
    }

    private fun setupCopiesSpinner(currentCopies: Int) {
        val copiesOptions = listOf(1,2,3,4,5)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, copiesOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEditCopies.adapter = adapter
        val pos = copiesOptions.indexOf(currentCopies).takeIf { it >= 0 } ?: 0
        binding.spinnerEditCopies.setSelection(pos)
    }

    private fun saveRecord() {
        val updated = record?.copy(
            price = binding.editPrice.text.toString().ifEmpty { null },
            tagType = binding.spinnerEditTagType.selectedItem.toString(),
            unitType = binding.spinnerEditUnitType.selectedItem.toString(),
            copies = binding.spinnerEditCopies.selectedItem as Int,
            customEng = binding.editCustomEng.text.toString().ifEmpty { null },
            customAra = binding.editCustomAra.text.toString().ifEmpty { null }
        )
        if (updated != null) {
            onSaveCallback?.invoke(updated)
            Toast.makeText(context, "Record updated", Toast.LENGTH_SHORT).show()
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
