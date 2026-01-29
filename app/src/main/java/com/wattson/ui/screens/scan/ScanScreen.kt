package com.wattson.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.wattson.ui.components.WattsonButton
import com.wattson.ui.components.WattsonOutlinedButton
import com.wattson.ui.theme.WattsonColors
import com.wattson.ui.theme.WattsonCorners
import com.wattson.ui.theme.WattsonPreviewTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.Executors

/**
 * Scan screen with camera preview for barcode scanning.
 */
@Composable
fun ScanScreen(
    uiState: ScanUiState,
    events: SharedFlow<ScanEvent>,
    onIntent: (ScanIntent) -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onIntent(ScanIntent.OnCameraPermissionResult(granted))
    }

    // Handle events
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is ScanEvent.NavigateToProductDetail -> onNavigateToProduct(event.productId)
                is ScanEvent.NavigateBack -> onNavigateBack()
                is ScanEvent.RequestCameraPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
                is ScanEvent.OpenAppSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                is ScanEvent.ShowError -> { /* Handled by UI state */ }
                is ScanEvent.ShowProductNotFound -> { /* Handled by UI state */ }
            }
        }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        onIntent(ScanIntent.RequestPermission)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.showPermissionRationale || !uiState.hasCameraPermission -> {
                PermissionDeniedContent(
                    onRequestPermission = { onIntent(ScanIntent.RequestPermission) },
                    onOpenSettings = { onIntent(ScanIntent.OpenSettings) }
                )
            }
            else -> {
                // Camera preview
                CameraPreviewContent(
                    isScanning = uiState.isScanning,
                    isTorchEnabled = uiState.isTorchEnabled,
                    onBarcodeDetected = { barcode, format ->
                        onIntent(ScanIntent.OnBarcodeDetected(barcode, format))
                    },
                    onToggleTorch = { onIntent(ScanIntent.ToggleTorch) }
                )

                // Scan frame overlay
                ScanFrameOverlay(
                    isProcessing = uiState.isProcessing
                )

                // Processing indicator
                AnimatedVisibility(
                    visible = uiState.isProcessing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    ProcessingOverlay()
                }
            }
        }

        // Error message
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            ErrorBanner(
                message = uiState.errorMessage ?: "",
                onDismiss = { onIntent(ScanIntent.DismissError) },
                onRetry = { onIntent(ScanIntent.RetryLastScan) }
            )
        }
    }
}

@Composable
private fun CameraPreviewContent(
    isScanning: Boolean,
    isTorchEnabled: Boolean,
    onBarcodeDetected: (String, com.wattson.domain.model.BarcodeFormat) -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Update torch state
    LaunchedEffect(isTorchEnabled) {
        camera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                // TODO: Implement barcode analysis with ML Kit
                                // For now, just close the image
                                imageProxy.close()
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        // Handle camera binding errors
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Torch toggle button
        IconButton(
            onClick = onToggleTorch,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isTorchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = if (isTorchEnabled) "Désactiver le flash" else "Activer le flash",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ScanFrameOverlay(
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isProcessing) 0.7f else 0.5f,
        animationSpec = tween(300),
        label = "overlayAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Semi-transparent overlay outside the scan area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(overlayAlpha)
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // Scan frame (clear area)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp, 200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .border(
                    width = 3.dp,
                    color = if (isProcessing) MaterialTheme.colorScheme.primary else Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // Instructions text
        if (!isProcessing) {
            Text(
                text = "Placez le code-barres dans le cadre",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 280.dp)
            )
        }
    }
}

@Composable
private fun ProcessingOverlay(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(WattsonCorners.Card)
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recherche du produit...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Accès à la caméra requis",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = WattsonColors.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Pour scanner les codes-barres des produits, Wattson a besoin d'accéder à votre caméra.",
            style = MaterialTheme.typography.bodyMedium,
            color = WattsonColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        WattsonButton(
            text = "Autoriser l'accès",
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        WattsonOutlinedButton(
            text = "Ouvrir les paramètres",
            onClick = onOpenSettings,
            leadingIcon = Icons.Filled.Settings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = WattsonCorners.Card,
        color = WattsonColors.Error.copy(alpha = 0.9f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            WattsonButton(
                text = "Réessayer",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ===== PREVIEWS =====

@ComposePreview(showBackground = true, showSystemUi = true)
@Composable
private fun ScanScreenPermissionDeniedPreview() {
    WattsonPreviewTheme {
        PermissionDeniedContent(
            onRequestPermission = {},
            onOpenSettings = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun ProcessingOverlayPreview() {
    WattsonPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            ProcessingOverlay()
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun ErrorBannerPreview() {
    WattsonPreviewTheme {
        ErrorBanner(
            message = "Produit non trouvé dans notre base de données",
            onDismiss = {},
            onRetry = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
