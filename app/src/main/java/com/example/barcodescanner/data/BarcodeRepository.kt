package com.example.barcodescanner.data

import kotlinx.coroutines.flow.Flow

class BarcodeRepository(private val barcodeDao: BarcodeDao) {
    suspend fun insertRecord(record: BarcodeRecord): Long = barcodeDao.insert(record)
    fun getAllRecords(): Flow<List<BarcodeRecord>> = barcodeDao.getAllRecords()
    fun getRecentScans(): Flow<List<BarcodeRecord>> = barcodeDao.getRecentScans()
    suspend fun updateRecord(record: BarcodeRecord) = barcodeDao.update(record)
    suspend fun deleteRecord(id: Long) = barcodeDao.deleteById(id)
}
