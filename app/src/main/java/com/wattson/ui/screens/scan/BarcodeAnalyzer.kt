package com.wattson.ui.screens.scan

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.wattson.domain.model.BarcodeFormat

/**
 * ML Kit barcode analyzer for CameraX image analysis.
 * Detects EAN/GTIN barcodes in camera frames.
 *
 * @param onBarcodeDetected Callback when a barcode is successfully detected
 * @param onError Callback when an error occurs during analysis
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String, BarcodeFormat) -> Unit,
    private val onError: (Exception) -> Unit = {}
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "BarcodeAnalyzer"
        
        // Throttle to avoid processing too many frames
        private const val SCAN_THROTTLE_MS = 500L
    }

    private var lastAnalyzedTimestamp = 0L
    private var isProcessing = false

    private val scanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        
        // Throttle processing
        if (currentTimestamp - lastAnalyzedTimestamp < SCAN_THROTTLE_MS || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastAnalyzedTimestamp = currentTimestamp

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                processBarcodes(barcodes)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Barcode scanning failed", exception)
                onError(exception)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun processBarcodes(barcodes: List<Barcode>) {
        if (barcodes.isEmpty()) return

        // Find the first valid barcode with raw value
        val validBarcode = barcodes.firstOrNull { barcode ->
            !barcode.rawValue.isNullOrEmpty() && isValidProductBarcode(barcode)
        }

        validBarcode?.let { barcode ->
            val rawValue = barcode.rawValue ?: return@let
            val format = mapBarcodeFormat(barcode.format)
            
            Log.d(TAG, "Barcode detected: $rawValue (format: $format)")
            onBarcodeDetected(rawValue, format)
        }
    }

    private fun isValidProductBarcode(barcode: Barcode): Boolean {
        val rawValue = barcode.rawValue ?: return false
        
        return when (barcode.format) {
            Barcode.FORMAT_EAN_13 -> rawValue.length == 13 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_EAN_8 -> rawValue.length == 8 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_UPC_A -> rawValue.length == 12 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_UPC_E -> rawValue.length in 6..8 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_QR_CODE -> {
                // QR codes might contain GTIN or URL with product info
                rawValue.all { it.isDigit() } && rawValue.length in 8..14
            }
            else -> true
        }
    }

    private fun mapBarcodeFormat(mlKitFormat: Int): BarcodeFormat {
        return when (mlKitFormat) {
            Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
            Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
            Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
            Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
            Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
            Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
            Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
            Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
            else -> BarcodeFormat.UNKNOWN
        }
    }

    /**
     * Release scanner resources when no longer needed.
     */
    fun close() {
        scanner.close()
    }
}

/**
 * Extension to validate EAN-13 checksum.
 */
fun String.isValidEan13(): Boolean {
    if (length != 13 || !all { it.isDigit() }) return false
    
    val digits = map { it.digitToInt() }
    val checksum = digits.dropLast(1).mapIndexed { index, digit ->
        if (index % 2 == 0) digit else digit * 3
    }.sum()
    
    val calculatedCheckDigit = (10 - (checksum % 10)) % 10
    return calculatedCheckDigit == digits.last()
}

/**
 * Normalize barcode to GTIN-13 format.
 * EAN-8 is padded with leading zeros.
 * UPC-A is prefixed with 0.
 */
fun String.toGtin13(): String {
    return when (length) {
        8 -> "00000$this" // EAN-8
        12 -> "0$this"    // UPC-A
        13 -> this        // Already EAN-13
        else -> this.padStart(13, '0')
    }
}
