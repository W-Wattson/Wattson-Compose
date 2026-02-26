package com.wattson.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.data.repository.AuthRepository
import com.wattson.domain.model.BarcodeFormat
import com.wattson.domain.model.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Scan screen.
 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val lastScannedCode: String? = null,
    val scanResult: ScanResult? = null,
    val errorMessage: String? = null,
    val showPermissionRationale: Boolean = false,
    val showEprelDialog: Boolean = false
)

/**
 * One-shot events for scan screen.
 */
sealed interface ScanEvent {
    data class NavigateToProductDetail(val productId: String) : ScanEvent
    data object NavigateBack : ScanEvent
    data class ShowError(val message: String) : ScanEvent
    data class ShowProductNotFound(val barcode: String) : ScanEvent
    data object RequestCameraPermission : ScanEvent
    data object OpenAppSettings : ScanEvent
}

/**
 * User intents for scan screen.
 */
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
}

/**
 * ViewModel for the Scan screen.
 * Manages camera state and barcode detection.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val productRepository: com.wattson.data.repository.ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>()
    val events = _events.asSharedFlow()

    /**
     * Process user intents.
     */
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
        }
    }

    private fun startScanning() {
        if (_uiState.value.hasCameraPermission) {
            _uiState.update { 
                it.copy(
                    isScanning = true, 
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
        // Avoid processing the same barcode multiple times
        if (_uiState.value.isProcessing || _uiState.value.lastScannedCode == barcode) {
            return
        }

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isProcessing = true,
                    lastScannedCode = barcode,
                    isScanning = false
                ) 
            }

            try {
                // Call real API via ProductRepository
                val userId = authRepository.getCurrentUserId()
                val scanResult = productRepository.scanProduct(userId, barcode)

                if (scanResult is ScanResult.Success) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            scanResult = scanResult
                        )
                    }
                    // Navigate using GTIN (EAN) instead of internal ID for API lookup
                    _events.emit(ScanEvent.NavigateToProductDetail(scanResult.product.gtin))
                }
                else if (scanResult is ScanResult.ProductNotFound) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Produit non trouvé: $barcode",
                            scanResult = scanResult
                        )
                    }
                    _events.emit(ScanEvent.ShowProductNotFound(barcode))
                }
                else if (scanResult is ScanResult.NetworkError) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = scanResult.message ?: "Erreur réseau",
                            scanResult = scanResult
                        )
                    }
                    _events.emit(ScanEvent.ShowError(scanResult.message ?: "Erreur réseau"))
                }
                else {
                    // Handle other ScanResult types (DecodingFailed, CameraError)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Erreur inattendue: ${scanResult::class.simpleName}",
                            scanResult = scanResult
                        )
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("ScanViewModel", "Scan exception", e)
                val errorMsg = e.message ?: e.javaClass.simpleName
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = errorMsg,
                        scanResult = ScanResult.NetworkError(
                            gtin = barcode,
                            message = errorMsg
                        )
                    )
                }
                _events.emit(ScanEvent.ShowError(errorMsg))
            }
        }
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
                lastScannedCode = null
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
        val lastCode = _uiState.value.lastScannedCode
        if (lastCode != null) {
            _uiState.update { it.copy(lastScannedCode = null) }
            onBarcodeDetected(lastCode, BarcodeFormat.EAN_13)
        } else {
            startScanning()
        }
    }

    private fun showEprelDialog() {
        _uiState.update { it.copy(showEprelDialog = true) }
    }

    private fun dismissEprelDialog() {
        _uiState.update { it.copy(showEprelDialog = false) }
    }

    private fun searchByEprelId(category: String, registrationNumber: String) {
        if (_uiState.value.isProcessing) return

        _uiState.update {
            it.copy(
                showEprelDialog = false,
                isProcessing = true,
                errorMessage = null,
                lastScannedCode = "$category/$registrationNumber"
            )
        }

        viewModelScope.launch {
            try {
                val result = productRepository.getProductByEprelId(category, registrationNumber)
                result.fold(
                    onSuccess = { product ->
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                scanResult = ScanResult.Success(product = product, isNewProduct = false)
                            )
                        }
                        // Register the EPREL scan in history
                        try {
                            val userId = authRepository.getCurrentUserId()
                            productRepository.registerEprelScan(userId, category, registrationNumber)
                        } catch (e: Exception) {
                            android.util.Log.w("ScanViewModel", "Failed to register EPREL scan in history", e)
                        }
                        // Navigate using EPREL identifier (gtin is synthetic "0000000000000")
                        val productId = "eprel:$category/$registrationNumber"
                        _events.emit(ScanEvent.NavigateToProductDetail(productId))
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "Produit EPREL non trouvé: $category/$registrationNumber"
                            )
                        }
                        _events.emit(ScanEvent.ShowError("Produit EPREL non trouvé"))
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ScanViewModel", "EPREL search error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "Erreur de recherche EPREL"
                    )
                }
                _events.emit(ScanEvent.ShowError(e.message ?: "Erreur de recherche EPREL"))
            }
        }
    }

    /**
     * Called when the screen is first displayed.
     * Checks camera permission status.
     */
    fun onScreenVisible() {
        // Permission will be checked by the composable and result sent via intent
    }
}
