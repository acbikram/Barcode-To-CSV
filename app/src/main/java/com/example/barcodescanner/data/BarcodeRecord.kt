package com.example.barcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_records")
data class BarcodeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    var price: String? = null,
    val tagType: String,
    val unitType: String,
    var copies: Int,
    var customEng: String? = null,
    var customAra: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
