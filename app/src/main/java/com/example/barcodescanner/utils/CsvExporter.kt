package com.example.barcodescanner.utils

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.barcodescanner.data.BarcodeRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportToCSV(activity: Activity, records: List<BarcodeRecord>) {
        try {
            val fileName = "barcode_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(activity.cacheDir, fileName)
            FileOutputStream(file).use { os ->
                os.write("pos_code,price,tag_type,unit_type,copies,custom_eng,custom_ara\n".toByteArray())
                records.forEach {
                    val posCode = "\"=\"\"${it.barcode}\"\"\""
                    val price = escapeCsvField(it.price ?: "")
                    val tag = escapeCsvField(it.tagType)
                    val unit = escapeCsvField(it.unitType)
                    val copies = it.copies.toString()
                    val eng = escapeCsvField(it.customEng ?: "")
                    val ara = escapeCsvField(it.customAra ?: "")
                    os.write("$posCode,$price,$tag,$unit,$copies,$eng,$ara\n".toByteArray())
                }
            }
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Export CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field
    }
}
