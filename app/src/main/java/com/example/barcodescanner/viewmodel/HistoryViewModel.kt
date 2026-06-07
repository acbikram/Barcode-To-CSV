package com.example.barcodescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcodescanner.data.BarcodeRecord
import com.example.barcodescanner.data.BarcodeRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: BarcodeRepository) : ViewModel() {
    val allRecords = repository.getAllRecords()
    
    fun updateRecord(record: BarcodeRecord) {
        viewModelScope.launch { repository.updateRecord(record) }
    }
    
    fun deleteRecord(id: Long) {
        viewModelScope.launch { repository.deleteRecord(id) }
    }
}
