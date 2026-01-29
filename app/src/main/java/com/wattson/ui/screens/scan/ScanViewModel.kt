package com.wattson.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val showPermissionRationale: Boolean = false
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
}

/**
 * ViewModel for the Scan screen.
 * Manages camera state and barcode detection.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val scanProductUseCase: ScanProductUseCase,
    // private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase
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
                // TODO: Replace with actual use case
                // val result = scanProductUseCase(barcode, format)
                
                // Simulate API call
                kotlinx.coroutines.delay(1500)
                
                // Mock success - in production, check if product exists
                val productId = "prod_${barcode.takeLast(6)}"
                val product = com.wattson.domain.model.Product(
                    id = productId,
                    gtin = barcode,
                    name = "Produit scanné",
                    brand = "Marque",
                    model = "Modèle",
                    category = com.wattson.domain.model.ProductCategory.OTHER,
                    energyLabel = null,
                    repairabilityIndex = null,
                    updatedAt = java.time.Instant.now()
                )
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        scanResult = ScanResult.Success(
                            product = product,
                            isNewProduct = true
                        )
                    ) 
                }
                
                _events.emit(ScanEvent.NavigateToProductDetail(productId))
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "Scan failed",
                        scanResult = ScanResult.NetworkError(
                            gtin = barcode,
                            message = e.message ?: "Network error"
                        )
                    ) 
                }
                _events.emit(ScanEvent.ShowError(e.message ?: "Scan failed"))
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

    /**
     * Called when the screen is first displayed.
     * Checks camera permission status.
     */
    fun onScreenVisible() {
        // Permission will be checked by the composable and result sent via intent
    }
}
