package com.example.barcodescanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.data.BarcodeDatabase
import com.example.barcodescanner.data.BarcodeRepository
import com.example.barcodescanner.databinding.ActivityHistoryBinding
import com.example.barcodescanner.ui.adapters.HistoryAdapter
import com.example.barcodescanner.ui.dialogs.EditRecordDialogFragment
import com.example.barcodescanner.utils.CsvExporter
import com.example.barcodescanner.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter

    companion object {
        fun start(activity: AppCompatActivity) {
            activity.startActivity(Intent(activity, HistoryActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "History"

        val repository = BarcodeRepository(BarcodeDatabase.getDatabase(this).barcodeDao())
        viewModel = HistoryViewModel(repository)

        adapter = HistoryAdapter { record ->
            EditRecordDialogFragment.newInstance(record) { updated ->
                viewModel.updateRecord(updated)
            }.show(supportFragmentManager, "edit_dialog")
        }
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistory.adapter = adapter
        viewModel.allRecords.observe(this) { records -> adapter.submitList(records) }

        binding.buttonExportAll.setOnClickListener {
            lifecycleScope.launch {
                val records = repository.getAllRecords().first()
                CsvExporter.exportToCSV(this@HistoryActivity, records)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
