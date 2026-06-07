package com.example.barcodescanner.scanning

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Find first non-null raw value without using break
                    val barcodeValue = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                    if (barcodeValue != null) {
                        onBarcodeDetected(barcodeValue)
                    }
                }
                .addOnFailureListener { e -> Log.e("BarcodeAnalyzer", "Scan failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }
}
