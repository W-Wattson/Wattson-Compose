package com.wattson.ui.screens.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
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
import com.wattson.data.ocr.EprelIdParser
import com.wattson.domain.model.BarcodeFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScanFrameAnalyzer(
    private val onBarcodeDetected: (String, BarcodeFormat) -> Unit,
    private val onLabelPreviewFrame: (Bitmap) -> Unit,
    private val onLabelCaptureFailed: (Exception) -> Unit = {},
    private val onError: (Exception) -> Unit = {}
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ScanFrameAnalyzer"
        private const val BARCODE_THROTTLE_MS = 500L
        private const val LABEL_PREVIEW_THROTTLE_MS = 1300L
    }

    private var lastBarcodeTimestamp = 0L
    private var lastLabelPreviewTimestamp = 0L
    private var isProcessingBarcode = false
    private val barcodeScanningEnabled = AtomicBoolean(true)
    private val labelPreviewEnabled = AtomicBoolean(false)

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

    fun setBarcodeScanningEnabled(enabled: Boolean) {
        barcodeScanningEnabled.set(enabled)
    }

    fun setLabelPreviewEnabled(enabled: Boolean) {
        labelPreviewEnabled.set(enabled)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (labelPreviewEnabled.get()) {
            sampleLabelPreview(imageProxy)
            return
        }

        if (!barcodeScanningEnabled.get()) {
            imageProxy.close()
            return
        }

        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastBarcodeTimestamp < BARCODE_THROTTLE_MS || isProcessingBarcode) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessingBarcode = true
        lastBarcodeTimestamp = currentTimestamp

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
                isProcessingBarcode = false
                imageProxy.close()
            }
    }

    private fun sampleLabelPreview(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastLabelPreviewTimestamp < LABEL_PREVIEW_THROTTLE_MS) {
            imageProxy.close()
            return
        }

        lastLabelPreviewTimestamp = currentTimestamp

        try {
            onLabelPreviewFrame(imageProxy.toBitmap())
        } catch (exception: Exception) {
            Log.e(TAG, "Label preview failed", exception)
            onLabelCaptureFailed(exception)
        } finally {
            imageProxy.close()
        }
    }

    private fun processBarcodes(barcodes: List<Barcode>) {
        if (barcodes.isEmpty()) return

        val validBarcode = barcodes.firstOrNull { barcode ->
            !barcode.rawValue.isNullOrEmpty() && isValidScanPayload(barcode)
        }

        validBarcode?.let { barcode ->
            val rawValue = barcode.rawValue ?: return@let
            val format = mapBarcodeFormat(barcode.format)

            Log.d(TAG, "Barcode detected: $rawValue (format: $format)")
            onBarcodeDetected(rawValue, format)
        }
    }

    private fun isValidScanPayload(barcode: Barcode): Boolean {
        val rawValue = barcode.rawValue ?: return false

        return when (barcode.format) {
            Barcode.FORMAT_EAN_13 -> rawValue.length == 13 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_EAN_8 -> rawValue.length == 8 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_UPC_A -> rawValue.length == 12 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_UPC_E -> rawValue.length in 6..8 && rawValue.all { it.isDigit() }
            Barcode.FORMAT_QR_CODE -> {
                val digitsOnly = rawValue.all { it.isDigit() } && rawValue.length in 8..14
                val eprelQr = EprelIdParser.extractFromQrPayload(rawValue) != null
                digitsOnly || eprelQr
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

    fun close() {
        scanner.close()
    }
}

private fun ImageProxy.toBitmap(): Bitmap {
    val nv21Buffer = yuv420888ToNv21(planes, width, height)
    val yuvImage = YuvImage(nv21Buffer, ImageFormat.NV21, width, height, null)
    val outputStream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, outputStream)
    val jpegBytes = outputStream.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    return bitmap.rotate(imageInfo.rotationDegrees)
}

private fun Bitmap.rotate(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this

    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    recycle()
    return rotatedBitmap
}

private fun yuv420888ToNv21(
    planes: Array<ImageProxy.PlaneProxy>,
    width: Int,
    height: Int
): ByteArray {
    val ySize = width * height
    val uvSize = width * height / 4
    val nv21 = ByteArray(ySize + uvSize * 2)

    copyPlane(
        plane = planes[0].buffer,
        rowStride = planes[0].rowStride,
        pixelStride = planes[0].pixelStride,
        width = width,
        height = height,
        output = nv21,
        offset = 0,
        outputStride = 1
    )

    copyPlane(
        plane = planes[2].buffer,
        rowStride = planes[2].rowStride,
        pixelStride = planes[2].pixelStride,
        width = width / 2,
        height = height / 2,
        output = nv21,
        offset = ySize,
        outputStride = 2
    )

    copyPlane(
        plane = planes[1].buffer,
        rowStride = planes[1].rowStride,
        pixelStride = planes[1].pixelStride,
        width = width / 2,
        height = height / 2,
        output = nv21,
        offset = ySize + 1,
        outputStride = 2
    )

    return nv21
}

private fun copyPlane(
    plane: ByteBuffer,
    rowStride: Int,
    pixelStride: Int,
    width: Int,
    height: Int,
    output: ByteArray,
    offset: Int,
    outputStride: Int
) {
    val rowBuffer = ByteArray(rowStride)
    var outputOffset = offset

    for (row in 0 until height) {
        val length = if (pixelStride == 1 && outputStride == 1) width else (width - 1) * pixelStride + 1
        plane.position(row * rowStride)
        plane.get(rowBuffer, 0, length)

        var column = 0
        while (column < width) {
            output[outputOffset] = rowBuffer[column * pixelStride]
            outputOffset += outputStride
            column++
        }
    }
}
