package com.wattson.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Size
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wattson.R
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
                    scanMode = uiState.scanMode,
                    isTorchEnabled = uiState.isTorchEnabled,
                    onBarcodeDetected = { barcode, format ->
                        onIntent(ScanIntent.OnBarcodeDetected(barcode, format))
                    },
                    onLabelPreviewFrame = { bitmap ->
                        onIntent(ScanIntent.OnEprelLabelPreviewFrame(bitmap))
                    },
                    onLabelCaptureFailed = { message ->
                        onIntent(ScanIntent.OnEprelLabelCaptureFailed(message))
                    },
                    onToggleTorch = { onIntent(ScanIntent.ToggleTorch) }
                )

                // Scan frame overlay
                ScanFrameOverlay(
                    isProcessing = uiState.isProcessing,
                    scanMode = uiState.scanMode,
                    labelGuidance = uiState.labelGuidance
                )

                // Processing indicator
                AnimatedVisibility(
                    visible = uiState.isProcessing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    ProcessingOverlay(stage = uiState.processingStage)
                }

                // EPREL search button (bottom-left)
                if (!uiState.isProcessing) {
                    Surface(
                        onClick = { onIntent(ScanIntent.ShowEprelDialog) },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.eprel_search),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (uiState.scanMode == ScanMode.EPREL_LABEL) {
                                onIntent(ScanIntent.CancelEprelLabelGuidance)
                            } else {
                                onIntent(ScanIntent.StartEprelLabelGuidance)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = if (uiState.scanMode == ScanMode.EPREL_LABEL) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.scanMode == ScanMode.EPREL_LABEL) Icons.Filled.Close else Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = if (uiState.scanMode == ScanMode.EPREL_LABEL) {
                                    MaterialTheme.colorScheme.onError
                                } else {
                                    MaterialTheme.colorScheme.onSecondary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (uiState.scanMode == ScanMode.EPREL_LABEL) {
                                        R.string.cancel_label_scan
                                    } else {
                                        R.string.read_energy_label
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.scanMode == ScanMode.EPREL_LABEL) {
                                    MaterialTheme.colorScheme.onError
                                } else {
                                    MaterialTheme.colorScheme.onSecondary
                                }
                            )
                        }
                    }
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

        // EPREL search bottom sheet
        if (uiState.showEprelDialog) {
            EprelSearchBottomSheet(
                onDismiss = { onIntent(ScanIntent.DismissEprelDialog) },
                onSearch = { category, id ->
                    onIntent(ScanIntent.SearchByEprelId(category, id))
                }
            )
        }
    }
}

@Composable
private fun CameraPreviewContent(
    isScanning: Boolean,
    scanMode: ScanMode,
    isTorchEnabled: Boolean,
    onBarcodeDetected: (String, com.wattson.domain.model.BarcodeFormat) -> Unit,
    onLabelPreviewFrame: (android.graphics.Bitmap) -> Unit,
    onLabelCaptureFailed: (String) -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnLabelPreviewFrame by rememberUpdatedState(onLabelPreviewFrame)
    val currentOnLabelCaptureFailed by rememberUpdatedState(onLabelCaptureFailed)

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val scanAnalyzer = remember {
        ScanFrameAnalyzer(
            onBarcodeDetected = { barcode, format ->
                currentOnBarcodeDetected(barcode, format)
            },
            onLabelPreviewFrame = { bitmap ->
                currentOnLabelPreviewFrame(bitmap)
            },
            onLabelCaptureFailed = { error ->
                currentOnLabelCaptureFailed(error.message ?: "Label capture failed")
            },
            onError = { /* Ignore camera analysis errors silently */ }
        )
    }

    // Update torch state
    LaunchedEffect(isTorchEnabled) {
        camera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    LaunchedEffect(isScanning, scanMode) {
        scanAnalyzer.setBarcodeScanningEnabled(isScanning && scanMode == ScanMode.BARCODE)
    }

    LaunchedEffect(scanMode) {
        scanAnalyzer.setLabelPreviewEnabled(scanMode == ScanMode.EPREL_LABEL)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanAnalyzer.close()
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
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor, scanAnalyzer)
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
                contentDescription = if (isTorchEnabled) stringResource(R.string.flash_off) else stringResource(R.string.flash_on),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ScanFrameOverlay(
    isProcessing: Boolean,
    scanMode: ScanMode,
    labelGuidance: LabelGuidanceState,
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

        val frameColor = when {
            isProcessing -> MaterialTheme.colorScheme.primary
            scanMode == ScanMode.EPREL_LABEL && labelGuidance == LabelGuidanceState.Ready -> Color(0xFF35C759)
            scanMode == ScanMode.EPREL_LABEL -> MaterialTheme.colorScheme.error
            else -> Color.White
        }

        val frameSizeModifier = if (scanMode == ScanMode.EPREL_LABEL) {
            Modifier.size(width = 250.dp, height = 470.dp)
        } else {
            Modifier.size(280.dp, 200.dp)
        }

        // Scan frame (clear area)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .then(frameSizeModifier)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .border(
                    width = 3.dp,
                    color = frameColor,
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // Instructions text
        if (!isProcessing) {
            val instructionText = when (scanMode) {
                ScanMode.BARCODE -> stringResource(R.string.place_barcode)
                ScanMode.EPREL_LABEL -> when (labelGuidance) {
                    LabelGuidanceState.Ready -> stringResource(R.string.label_scan_ready)
                    LabelGuidanceState.Searching -> stringResource(R.string.label_scan_instruction)
                    LabelGuidanceState.NotScannable -> stringResource(R.string.label_scan_not_ready)
                    LabelGuidanceState.Hidden -> stringResource(R.string.label_scan_instruction)
                }
            }

            val helperText = when (scanMode) {
                ScanMode.BARCODE -> stringResource(R.string.scan_label_hint)
                ScanMode.EPREL_LABEL -> stringResource(R.string.label_scan_helper)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = if (scanMode == ScanMode.EPREL_LABEL) 560.dp else 300.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(
    stage: ScanProcessingStage?,
    modifier: Modifier = Modifier
) {
    val messageRes = when (stage) {
        ScanProcessingStage.OCR_LABEL -> R.string.reading_energy_label
        ScanProcessingStage.SEARCH_PRODUCT, null -> R.string.searching_product
    }

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
            text = stringResource(messageRes),
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
            text = stringResource(R.string.camera_access_required),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = WattsonColors.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.camera_access_description),
            style = MaterialTheme.typography.bodyMedium,
            color = WattsonColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        WattsonButton(
            text = stringResource(R.string.allow_access),
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        WattsonOutlinedButton(
            text = stringResource(R.string.open_settings),
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
                text = stringResource(R.string.retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ===== EPREL SEARCH BOTTOM SHEET =====

/**
 * EPREL categories available for search.
 */
private data class EprelCategory(
    val apiName: String,
    val displayName: String
)

private val EPREL_CATEGORIES = listOf(
    EprelCategory("lightsources", "Sources lumineuses"),
    EprelCategory("electronicdisplays", "Écrans / TV"),
    EprelCategory("washingmachines2019", "Lave-linge"),
    EprelCategory("washerdryers", "Lave-linge séchant"),
    EprelCategory("dishwashers2019", "Lave-vaisselle"),
    EprelCategory("refrigeratingappliances2019", "Réfrigérateurs"),
    EprelCategory("tumbledryers", "Sèche-linge"),
    EprelCategory("ovens", "Fours"),
    EprelCategory("rangehoods", "Hottes aspirantes"),
    EprelCategory("airconditioners", "Climatiseurs"),
    EprelCategory("tyres", "Pneus"),
    EprelCategory("spaceheaters", "Chauffage"),
    EprelCategory("waterheaters", "Chauffe-eau"),
    EprelCategory("electronicdisplays20232766", "Écrans (2023)"),
    EprelCategory("smartphonestablets20231669", "Smartphones / Tablettes")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EprelSearchBottomSheet(
    onDismiss: () -> Unit,
    onSearch: (category: String, registrationNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf(EPREL_CATEGORIES[0]) }
    var registrationNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.eprel_search_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.eprel_search_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.eprel_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    EPREL_CATEGORIES.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Registration number input
            OutlinedTextField(
                value = registrationNumber,
                onValueChange = { registrationNumber = it.filter { c -> c.isLetterOrDigit() } },
                label = { Text(stringResource(R.string.eprel_registration_number)) },
                placeholder = { Text("ex: 2640403") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (registrationNumber.isNotBlank()) {
                            onSearch(selectedCategory.apiName, registrationNumber)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Search button
            WattsonButton(
                text = stringResource(R.string.eprel_search_button),
                onClick = {
                    if (registrationNumber.isNotBlank()) {
                        onSearch(selectedCategory.apiName, registrationNumber)
                    }
                },
                enabled = registrationNumber.isNotBlank(),
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
            ProcessingOverlay(stage = ScanProcessingStage.SEARCH_PRODUCT)
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
