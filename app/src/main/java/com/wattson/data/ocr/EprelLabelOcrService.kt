package com.wattson.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.googlecode.tesseract.android.TessBaseAPI
import com.wattson.domain.model.DEFAULT_EPREL_CATEGORY_IDS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class EprelLabelOcrResult(
    val primaryRegistrationNumber: String,
    val candidateRegistrationNumbers: List<String>,
    val rawText: String,
    val preferredCategories: List<String>
)

@Singleton
class EprelLabelOcrService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "EprelLabelOcrService"
        private const val OCR_LANGUAGE = "eng"
        private const val OCR_ASSET_PATH = "tessdata/eng.traineddata"
        private const val OCR_WHITELIST = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz:/.-_ "
    }

    private val tesseractMutex = Mutex()
    private val dataPath = File(context.filesDir, "tesseract")
    private var tessBaseApi: TessBaseAPI? = null
    private val qrScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    suspend fun extractRegistrationNumber(bitmap: Bitmap): Result<EprelLabelOcrResult> = withContext(Dispatchers.Default) {
        extractFromQrCode(bitmap)?.let { qrResult ->
            return@withContext Result.success(qrResult)
        }

        tesseractMutex.withLock {
            runCatching {
                ensureTrainedData()
                val tess = ensureTesseract()
                val observations = runOcr(bitmap, tess)
                val extraction = EprelIdParser.extractFromObservations(observations)
                val candidates = extraction.candidates.ifEmpty {
                    throw IllegalStateException("No EPREL registration number detected in OCR text")
                }

                EprelLabelOcrResult(
                    primaryRegistrationNumber = candidates.first(),
                    candidateRegistrationNumbers = candidates,
                    rawText = extraction.rawText,
                    preferredCategories = extraction.preferredCategories.ifEmpty { DEFAULT_EPREL_CATEGORY_IDS }
                )
            }.onFailure {
                Log.e(TAG, "Failed to extract EPREL registration number", it)
            }
        }
    }

    private suspend fun extractFromQrCode(bitmap: Bitmap): EprelLabelOcrResult? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val barcodes = suspendCancellableCoroutine<List<Barcode>> { continuation ->
            qrScanner.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        }

        val qrPayload = barcodes.firstNotNullOfOrNull { it.rawValue }
        val registrationNumber = qrPayload?.let(EprelIdParser::extractFromQrPayload) ?: return null

        return EprelLabelOcrResult(
            primaryRegistrationNumber = registrationNumber,
            candidateRegistrationNumbers = listOf(registrationNumber),
            rawText = "QR:$qrPayload",
            preferredCategories = qrPayload.extractPreferredCategories()
        )
    }

    private fun runOcr(bitmap: Bitmap, tess: TessBaseAPI): List<OcrTextObservation> {
        val observations = mutableListOf<OcrTextObservation>()

        buildCropSpecs(bitmap).forEach { cropSpec ->
            val croppedBitmap = cropBitmap(bitmap, cropSpec)
            val variants = buildVariants(croppedBitmap)

            variants.forEach { (variantName, candidateBitmap) ->
                try {
                    tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, OCR_WHITELIST)
                    tess.setPageSegMode(cropSpec.pageSegMode)
                    tess.setImage(candidateBitmap)

                    val text = tess.getUTF8Text().orEmpty()
                    val confidence = tess.meanConfidence()
                    if (text.isNotBlank()) {
                        observations += OcrTextObservation(
                            label = "${cropSpec.name}:$variantName",
                            text = text,
                            zoneBoost = cropSpec.scoreBoost + (confidence / 10)
                        )
                    }
                } finally {
                    tess.clear()
                    candidateBitmap.recycle()
                }
            }

            if (croppedBitmap != bitmap && !croppedBitmap.isRecycled) {
                croppedBitmap.recycle()
            }
        }

        return observations
    }

    private fun buildCropSpecs(bitmap: Bitmap): List<CropSpec> {
        // The EPREL registration number is typically printed near the QR block in the lower-right
        // section of EU energy labels. The fallback crops widen around that area before trying
        // the whole label if framing is imperfect.
        return listOf(
            CropSpec(
                name = "center_label",
                leftRatio = 0.16f,
                topRatio = 0.05f,
                rightRatio = 0.84f,
                bottomRatio = 0.96f,
                scoreBoost = 38,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            ),
            CropSpec(
                name = "center_registration_strip",
                leftRatio = 0.28f,
                topRatio = 0.70f,
                rightRatio = 0.84f,
                bottomRatio = 0.96f,
                scoreBoost = 62,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
            ),
            CropSpec(
                name = "registration_strip",
                leftRatio = 0.40f,
                topRatio = 0.70f,
                rightRatio = 0.98f,
                bottomRatio = 0.98f,
                scoreBoost = 55,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
            ),
            CropSpec(
                name = "lower_right",
                leftRatio = 0.48f,
                topRatio = 0.45f,
                rightRatio = 0.98f,
                bottomRatio = 0.98f,
                scoreBoost = 45,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            ),
            CropSpec(
                name = "right_column",
                leftRatio = 0.55f,
                topRatio = 0.20f,
                rightRatio = 0.98f,
                bottomRatio = 0.95f,
                scoreBoost = 28,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            ),
            CropSpec(
                name = "full_label",
                leftRatio = 0.04f,
                topRatio = 0.04f,
                rightRatio = 0.98f,
                bottomRatio = 0.98f,
                scoreBoost = 10,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            )
        ).map {
            it.clamped(bitmap.width, bitmap.height)
        }
    }

    private fun buildVariants(bitmap: Bitmap): List<Pair<String, Bitmap>> {
        val grayscale = upscaleIfNeeded(toGrayscale(bitmap))
        val highContrast = toBinaryBitmap(grayscale)
        return listOf(
            "gray" to grayscale,
            "binary" to highContrast
        )
    }

    private fun cropBitmap(bitmap: Bitmap, cropSpec: CropSpec): Bitmap {
        val left = (bitmap.width * cropSpec.leftRatio).toInt().coerceIn(0, bitmap.width - 1)
        val top = (bitmap.height * cropSpec.topRatio).toInt().coerceIn(0, bitmap.height - 1)
        val right = (bitmap.width * cropSpec.rightRatio).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bitmap.height * cropSpec.bottomRatio).toInt().coerceIn(top + 1, bitmap.height)

        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    private fun upscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = maxOf(bitmap.width, bitmap.height)
        if (maxDimension >= 1400) {
            return bitmap
        }

        val scale = 1400f / maxDimension.toFloat()
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(bitmap.width),
            (bitmap.height * scale).toInt().coerceAtLeast(bitmap.height),
            true
        )

        bitmap.recycle()
        return scaledBitmap
    }

    private fun toBinaryBitmap(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val averageLuma = pixels
            .map { pixel -> (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 }
            .average()
            .toInt()
            .coerceIn(90, 190)

        val outputPixels = IntArray(pixels.size)
        pixels.forEachIndexed { index, pixel ->
            val luma = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            val binary = if (luma >= averageLuma) 255 else 0
            outputPixels[index] = Color.argb(255, binary, binary, binary)
        }

        return Bitmap.createBitmap(outputPixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    private fun ensureTrainedData() {
        val tessDataDir = File(dataPath, "tessdata")
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }

        val targetFile = File(tessDataDir, "eng.traineddata")
        if (targetFile.exists() && targetFile.length() > 0L) {
            return
        }

        context.assets.open(OCR_ASSET_PATH).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun ensureTesseract(): TessBaseAPI {
        tessBaseApi?.let { return it }

        val api = TessBaseAPI()
        val initialized = api.init(dataPath.absolutePath, OCR_LANGUAGE)
        if (!initialized) {
            api.recycle()
            throw IllegalStateException("Unable to initialize Tesseract with $OCR_LANGUAGE")
        }

        api.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, OCR_WHITELIST)
        tessBaseApi = api
        return api
    }

    private fun String.extractPreferredCategories(): List<String> {
        val categoryFromUrl = Regex("product/([^/]+)/", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)

        return listOfNotNull(categoryFromUrl) + DEFAULT_EPREL_CATEGORY_IDS
    }

    private data class CropSpec(
        val name: String,
        val leftRatio: Float,
        val topRatio: Float,
        val rightRatio: Float,
        val bottomRatio: Float,
        val scoreBoost: Int,
        val pageSegMode: Int
    ) {
        fun clamped(width: Int, height: Int): CropSpec {
            val minWidthRatio = (48f / width.toFloat()).coerceAtMost(1f)
            val minHeightRatio = (32f / height.toFloat()).coerceAtMost(1f)
            return copy(
                rightRatio = rightRatio.coerceAtLeast(leftRatio + minWidthRatio).coerceAtMost(1f),
                bottomRatio = bottomRatio.coerceAtLeast(topRatio + minHeightRatio).coerceAtMost(1f)
            )
        }
    }
}
