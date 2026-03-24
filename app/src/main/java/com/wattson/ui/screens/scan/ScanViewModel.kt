package com.wattson.ui.screens.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.ocr.EprelIdParser
import com.wattson.data.ocr.EprelLabelOcrService
import com.wattson.data.repository.AuthRepository
import com.wattson.domain.model.BarcodeFormat
import com.wattson.domain.model.ResolvedEprelProduct
import com.wattson.domain.model.ScanResult
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val processingStage: ScanProcessingStage? = null,
    val scanMode: ScanMode = ScanMode.BARCODE,
    val labelGuidance: LabelGuidanceState = LabelGuidanceState.Hidden,
    val hasCameraPermission: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val lastScannedCode: String? = null,
    val scanResult: ScanResult? = null,
    val errorMessage: UiText? = null,
    val showPermissionRationale: Boolean = false,
    val showEprelDialog: Boolean = false
)

enum class ScanProcessingStage {
    OCR_LABEL,
    SEARCH_PRODUCT
}

enum class ScanMode {
    BARCODE,
    EPREL_LABEL
}

sealed interface LabelGuidanceState {
    data object Hidden : LabelGuidanceState
    data object Searching : LabelGuidanceState
    data object NotScannable : LabelGuidanceState
    data object Ready : LabelGuidanceState
}
sealed interface ScanEvent {
    data class NavigateToProductDetail(val productId: String) : ScanEvent
    data object NavigateBack : ScanEvent
    data class ShowError(val message: UiText) : ScanEvent
    data class ShowProductNotFound(val barcode: String) : ScanEvent
    data object RequestCameraPermission : ScanEvent
    data object OpenAppSettings : ScanEvent
}

sealed interface ScanIntent {
    data object StartScanning : ScanIntent
    data object StopScanning : ScanIntent
    data class OnBarcodeDetected(val barcode: String, val format: BarcodeFormat) : ScanIntent
    data class OnCameraPermissionResult(val granted: Boolean) : ScanIntent
    data object ToggleTorch : ScanIntent
    data object DismissError : ScanIntent
    data object NavigateBack : ScanIntent
    data object RequestPermission : ScanIntent
    data object OpenSettings : ScanIntent
    data object RetryLastScan : ScanIntent
    data object ShowEprelDialog : ScanIntent
    data object DismissEprelDialog : ScanIntent
    data class SearchByEprelId(val category: String, val registrationNumber: String) : ScanIntent
    data object StartEprelLabelGuidance : ScanIntent
    data object CancelEprelLabelGuidance : ScanIntent
    data class OnEprelLabelPreviewFrame(val bitmap: Bitmap) : ScanIntent
    data class OnEprelLabelCaptureFailed(val message: String) : ScanIntent
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val productRepository: com.wattson.data.repository.ProductRepository,
    private val authRepository: AuthRepository,
    private val eprelLabelOcrService: EprelLabelOcrService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>()
    val events = _events.asSharedFlow()

    private var lastRetryRequest: RetryRequest? = null
    private var isEvaluatingLabelFrame = false
    fun onIntent(intent: ScanIntent) {
        when (intent) {
            is ScanIntent.StartScanning -> startScanning()
            is ScanIntent.StopScanning -> stopScanning()
            is ScanIntent.OnBarcodeDetected -> onBarcodeDetected(intent.barcode, intent.format)
            is ScanIntent.OnCameraPermissionResult -> onCameraPermissionResult(intent.granted)
            is ScanIntent.ToggleTorch -> toggleTorch()
            is ScanIntent.DismissError -> dismissError()
            is ScanIntent.NavigateBack -> navigateBack()
            is ScanIntent.RequestPermission -> requestPermission()
            is ScanIntent.OpenSettings -> openSettings()
            is ScanIntent.RetryLastScan -> retryLastScan()
            is ScanIntent.ShowEprelDialog -> showEprelDialog()
            is ScanIntent.DismissEprelDialog -> dismissEprelDialog()
            is ScanIntent.SearchByEprelId -> searchByEprelId(intent.category, intent.registrationNumber)
            is ScanIntent.StartEprelLabelGuidance -> startEprelLabelGuidance()
            is ScanIntent.CancelEprelLabelGuidance -> cancelEprelLabelGuidance()
            is ScanIntent.OnEprelLabelPreviewFrame -> onEprelLabelPreviewFrame(intent.bitmap)
            is ScanIntent.OnEprelLabelCaptureFailed -> onEprelLabelCaptureFailed(intent.message)
        }
    }

    private fun startScanning() {
        if (_uiState.value.hasCameraPermission) {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    isProcessing = false,
                    processingStage = null,
                    scanMode = ScanMode.BARCODE,
                    labelGuidance = LabelGuidanceState.Hidden,
                    errorMessage = null,
                    scanResult = null
                )
            }
        } else {
            viewModelScope.launch {
                _events.emit(ScanEvent.RequestCameraPermission)
            }
        }
    }

    private fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
    }

    private fun onBarcodeDetected(barcode: String, format: BarcodeFormat) {
        if (_uiState.value.scanMode != ScanMode.BARCODE) {
            return
        }
        if (_uiState.value.isProcessing || _uiState.value.lastScannedCode == barcode) {
            return
        }

        if (format == BarcodeFormat.QR_CODE) {
            val eprelRegistrationNumber = EprelIdParser.extractFromQrPayload(barcode)
            if (eprelRegistrationNumber != null) {
                viewModelScope.launch {
                    resolveAndNavigateEprel(
                        registrationNumbers = listOf(eprelRegistrationNumber),
                        preferredCategories = emptyList()
                    )
                }
                return
            }
        }

        lastRetryRequest = RetryRequest.Barcode(barcode)
        processBarcode(barcode)
    }

    private fun processBarcode(barcode: String) {
        beginSearch(lastScannedCode = barcode)

        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val scanResult = productRepository.scanProduct(userId, barcode)

                when (scanResult) {
                    is ScanResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingStage = null,
                                scanResult = scanResult
                            )
                        }
                        _events.emit(ScanEvent.NavigateToProductDetail(scanResult.product.gtin))
                    }

                    is ScanResult.ProductNotFound -> {
                        val message = UiText.StringResource(R.string.scan_product_not_found, barcode)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingStage = null,
                                errorMessage = message,
                                scanResult = scanResult,
                                isScanning = true
                            )
                        }
                        _events.emit(ScanEvent.ShowProductNotFound(barcode))
                    }

                    is ScanResult.NetworkError -> {
                        val message = scanResult.message
                            ?.takeIf { it.isNotBlank() }
                            ?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_network_generic)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingStage = null,
                                errorMessage = message,
                                scanResult = scanResult,
                                isScanning = true
                            )
                        }
                        _events.emit(ScanEvent.ShowError(message))
                    }

                    else -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingStage = null,
                                errorMessage = UiText.StringResource(R.string.error_unexpected_generic),
                                scanResult = scanResult,
                                isScanning = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ScanViewModel", "Scan exception", e)
                val errorText = e.toUiTextOr(UiText.StringResource(R.string.error_network_generic))
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingStage = null,
                        errorMessage = errorText,
                        scanResult = ScanResult.NetworkError(
                            gtin = barcode,
                            message = null
                        ),
                        isScanning = true
                    )
                }
                _events.emit(ScanEvent.ShowError(errorText))
            }
        }
    }

    private fun startEprelLabelGuidance() {
        if (!_uiState.value.hasCameraPermission) {
            requestPermission()
            return
        }

        _uiState.update {
            it.copy(
                isScanning = true,
                isProcessing = false,
                processingStage = null,
                scanMode = ScanMode.EPREL_LABEL,
                labelGuidance = LabelGuidanceState.Searching,
                errorMessage = null,
                scanResult = null,
                lastScannedCode = null
            )
        }
    }

    private fun cancelEprelLabelGuidance() {
        isEvaluatingLabelFrame = false
        _uiState.update {
            it.copy(
                scanMode = ScanMode.BARCODE,
                labelGuidance = LabelGuidanceState.Hidden,
                isProcessing = false,
                processingStage = null,
                errorMessage = null,
                isScanning = it.hasCameraPermission
            )
        }
    }

    private fun onEprelLabelPreviewFrame(bitmap: Bitmap) {
        if (_uiState.value.scanMode != ScanMode.EPREL_LABEL || _uiState.value.isProcessing || isEvaluatingLabelFrame) {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            return
        }

        isEvaluatingLabelFrame = true
        viewModelScope.launch {
            try {
                val extractionResult = eprelLabelOcrService.extractRegistrationNumber(bitmap)
                extractionResult.fold(
                    onSuccess = { result ->
                        _uiState.update { state ->
                            state.copy(labelGuidance = LabelGuidanceState.Ready)
                        }
                        resolveAndNavigateEprel(
                            registrationNumbers = result.candidateRegistrationNumbers,
                            preferredCategories = result.preferredCategories
                        )
                    },
                    onFailure = {
                        if (_uiState.value.scanMode == ScanMode.EPREL_LABEL && !_uiState.value.isProcessing) {
                            _uiState.update { state ->
                                state.copy(labelGuidance = LabelGuidanceState.NotScannable)
                            }
                        }
                    }
                )
            } finally {
                isEvaluatingLabelFrame = false
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun onEprelLabelCaptureFailed(message: String) {
        if (message.isNotBlank()) {
            Log.w("ScanViewModel", "EPREL label capture failed: $message")
        }
        if (_uiState.value.scanMode == ScanMode.EPREL_LABEL) {
            _uiState.update {
                it.copy(
                    labelGuidance = LabelGuidanceState.NotScannable,
                    errorMessage = UiText.StringResource(R.string.label_scan_capture_failed)
                )
            }
        }
    }

    private suspend fun resolveAndNavigateEprel(
        registrationNumbers: List<String>,
        preferredCategories: List<String>
    ) {
        val normalizedCandidates = registrationNumbers
            .mapNotNull { candidate -> candidate.filter(Char::isDigit).takeIf(EprelIdParser::isPlausibleRegistrationNumber) }
            .distinct()

        if (normalizedCandidates.isEmpty()) {
            if (_uiState.value.scanMode == ScanMode.EPREL_LABEL) {
                _uiState.update { it.copy(labelGuidance = LabelGuidanceState.NotScannable) }
            } else {
                showLabelReadError()
            }
            return
        }

        lastRetryRequest = RetryRequest.UnresolvedEprel(normalizedCandidates, preferredCategories)
        beginSearch(lastScannedCode = normalizedCandidates.first())

        var resolvedProduct: ResolvedEprelProduct? = null
        for (registrationNumber in normalizedCandidates) {
            val result = productRepository.resolveProductByEprelRegistrationNumber(
                registrationNumber = registrationNumber,
                preferredCategories = preferredCategories
            )

            if (result.isSuccess) {
                resolvedProduct = result.getOrNull()
                break
            }
        }

        val resolved = resolvedProduct
        if (resolved == null) {
            val message = UiText.StringResource(R.string.scan_eprel_product_not_found)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    processingStage = null,
                    errorMessage = message,
                    scanMode = ScanMode.EPREL_LABEL,
                    labelGuidance = LabelGuidanceState.NotScannable,
                    isScanning = it.hasCameraPermission
                )
            }
            _events.emit(ScanEvent.ShowError(message))
            return
        }

        handleResolvedEprelProduct(resolved)
    }

    private suspend fun handleResolvedEprelProduct(resolved: ResolvedEprelProduct) {
        lastRetryRequest = RetryRequest.ResolvedEprel(
            category = resolved.category,
            registrationNumber = resolved.registrationNumber
        )

        _uiState.update {
            it.copy(
                isProcessing = false,
                processingStage = null,
                scanMode = ScanMode.BARCODE,
                labelGuidance = LabelGuidanceState.Hidden,
                scanResult = ScanResult.Success(product = resolved.product, isNewProduct = false)
            )
        }

        try {
            val userId = authRepository.getCurrentUserId()
            productRepository.registerEprelScan(userId, resolved.category, resolved.registrationNumber)
        } catch (e: Exception) {
            Log.w("ScanViewModel", "Failed to register EPREL scan in history", e)
        }

        val productId = "eprel:${resolved.category}/${resolved.registrationNumber}"
        _events.emit(ScanEvent.NavigateToProductDetail(productId))
    }

    private fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { state ->
            state.copy(
                hasCameraPermission = granted,
                showPermissionRationale = !granted,
                isScanning = granted
            )
        }
    }

    private fun toggleTorch() {
        _uiState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
    }

    private fun dismissError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                scanResult = null,
                lastScannedCode = null,
                isProcessing = false,
                processingStage = null,
                isScanning = it.hasCameraPermission
            )
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _events.emit(ScanEvent.NavigateBack)
        }
    }

    private fun requestPermission() {
        viewModelScope.launch {
            _events.emit(ScanEvent.RequestCameraPermission)
        }
    }

    private fun openSettings() {
        viewModelScope.launch {
            _events.emit(ScanEvent.OpenAppSettings)
        }
    }

    private fun retryLastScan() {
        when (val retryRequest = lastRetryRequest) {
            is RetryRequest.Barcode -> {
                _uiState.update { it.copy(lastScannedCode = null) }
                processBarcode(retryRequest.barcode)
            }

            is RetryRequest.ResolvedEprel -> {
                _uiState.update { it.copy(lastScannedCode = null) }
                searchByEprelId(retryRequest.category, retryRequest.registrationNumber)
            }

            is RetryRequest.UnresolvedEprel -> {
                _uiState.update { it.copy(lastScannedCode = null) }
                beginSearch()
                viewModelScope.launch {
                    resolveAndNavigateEprel(
                        registrationNumbers = retryRequest.registrationNumbers,
                        preferredCategories = retryRequest.preferredCategories
                    )
                }
            }

            null -> {
                if (_uiState.value.scanMode == ScanMode.EPREL_LABEL) {
                    _uiState.update {
                        it.copy(
                            errorMessage = null,
                            labelGuidance = LabelGuidanceState.Searching,
                            isScanning = it.hasCameraPermission
                        )
                    }
                } else {
                    startScanning()
                }
            }
        }
    }

    private fun showEprelDialog() {
        _uiState.update { it.copy(showEprelDialog = true) }
    }

    private fun dismissEprelDialog() {
        _uiState.update { it.copy(showEprelDialog = false) }
    }

    private fun searchByEprelId(category: String, registrationNumber: String) {
        if (_uiState.value.isProcessing) {
            return
        }

        lastRetryRequest = RetryRequest.ResolvedEprel(category, registrationNumber)
        beginSearch(lastScannedCode = "$category/$registrationNumber", closeDialog = true)

        viewModelScope.launch {
            try {
                val result = productRepository.getProductByEprelId(category, registrationNumber)
                result.fold(
                    onSuccess = { product ->
                        handleResolvedEprelProduct(
                            ResolvedEprelProduct(
                                category = category,
                                registrationNumber = registrationNumber,
                                product = product
                            )
                        )
                    },
                    onFailure = {
                        val message = UiText.StringResource(R.string.scan_eprel_product_not_found)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingStage = null,
                                errorMessage = message,
                                isScanning = it.hasCameraPermission
                            )
                        }
                        _events.emit(ScanEvent.ShowError(message))
                    }
                )
            } catch (e: Exception) {
                Log.e("ScanViewModel", "EPREL search error", e)
                val errorText = e.toUiTextOr(
                    UiText.StringResource(R.string.scan_eprel_search_error)
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingStage = null,
                        errorMessage = errorText,
                        isScanning = it.hasCameraPermission
                    )
                }
                _events.emit(ScanEvent.ShowError(errorText))
            }
        }
    }

    private fun beginSearch(
        lastScannedCode: String? = null,
        closeDialog: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                showEprelDialog = if (closeDialog) false else it.showEprelDialog,
                isScanning = false,
                isProcessing = true,
                processingStage = if (it.scanMode == ScanMode.EPREL_LABEL) ScanProcessingStage.OCR_LABEL else ScanProcessingStage.SEARCH_PRODUCT,
                errorMessage = null,
                scanResult = null,
                lastScannedCode = lastScannedCode ?: it.lastScannedCode
            )
        }
    }

    private fun showLabelReadError() {
        _uiState.update {
            it.copy(
                isProcessing = false,
                processingStage = null,
                errorMessage = UiText.StringResource(R.string.label_scan_capture_failed),
                isScanning = it.hasCameraPermission
            )
        }
    }

    sealed interface RetryRequest {
        data class Barcode(val barcode: String) : RetryRequest
        data class ResolvedEprel(val category: String, val registrationNumber: String) : RetryRequest
        data class UnresolvedEprel(
            val registrationNumbers: List<String>,
            val preferredCategories: List<String>
        ) : RetryRequest
    }

    fun onScreenVisible() {
    }
}
