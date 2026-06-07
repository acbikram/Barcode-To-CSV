package com.example.barcodescanner.data

import kotlinx.coroutines.flow.Flow

class BarcodeRepository(private val barcodeDao: BarcodeDao) {
    suspend fun insertRecord(record: BarcodeRecord): Long = barcodeDao.insert(record)
    fun getAllRecords(): Flow<List<BarcodeRecord>> = barcodeDao.getAllRecords()
    fun getRecentScans(): Flow<List<BarcodeRecord>> = barcodeDao.getRecentScans()
    suspend fun updateRecord(record: BarcodeRecord) = barcodeDao.update(record)
    suspend fun deleteRecord(id: Long) = barcodeDao.deleteById(id)

    // NEW
    suspend fun findDuplicate(barcode: String, tagType: String, unitType: String): BarcodeRecord? =
        barcodeDao.findDuplicate(barcode, tagType, unitType)

    suspend fun incrementCopies(record: BarcodeRecord) {
        record.copies += 1
        barcodeDao.update(record)
    }
}
