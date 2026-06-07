package com.example.barcodescanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {
    @Insert
    suspend fun insert(record: BarcodeRecord): Long

    @Query("SELECT * FROM barcode_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BarcodeRecord>>

    @Query("SELECT * FROM barcode_records ORDER BY timestamp DESC LIMIT 50")
    fun getRecentScans(): Flow<List<BarcodeRecord>>

    @Update
    suspend fun update(record: BarcodeRecord)

    @Query("DELETE FROM barcode_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM barcode_records")
    suspend fun deleteAll()
}
