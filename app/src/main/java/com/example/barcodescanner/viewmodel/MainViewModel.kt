package com.example.barcodescanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcodescanner.data.BarcodeRecord
import com.example.barcodescanner.data.BarcodeRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: BarcodeRepository) : ViewModel() {
    val recentScans = repository.getRecentScans()
    
    private val _saveResult = MutableSharedFlow<Long>()
    val saveResult: SharedFlow<Long> = _saveResult

    fun saveRecord(record: BarcodeRecord) {
        viewModelScope.launch {
            val id = repository.insertRecord(record)
            _saveResult.emit(id)
        }
    }
}
