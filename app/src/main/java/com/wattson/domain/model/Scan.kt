package com.wattson.domain.model

import java.time.Instant

/**
 * Represents a product scan in the user's history.
 */
data class Scan(
    val id: String,
    val userId: String,
    val gtin: String,
    val scannedAt: Instant,
    val snapshotData: ScanSnapshot
)

/**
 * Snapshot of product data at the time of scan.
 * Preserves historical data even if product info is updated later.
 * Contains all environmental and sustainability fields.
 */
data class ScanSnapshot(
    val productName: String,
    val brand: String,
    val model: String? = null,
    val category: ProductCategory = ProductCategory.OTHER,
    val commercialName: String? = null,
    // Energy
    val energyClass: EnergyClass? = null,
    val kwhPerYear: Int? = null,
    val energyEfficiencyIndex: Double? = null,
    val powerStandbyMode: Double? = null,
    val powerOffMode: Double? = null,
    // Noise
    val noiseDecibels: Int? = null,
    val noiseClass: String? = null,
    // Wet grip (tyres)
    val wetGripClass: String? = null,
    // Sustainability
    val repairabilityIndex: Double? = null,
    val durabilityScore: Double? = null,
    // EPREL details
    val eprelProductGroup: String? = null,
    val eprelDetails: Map<String, Any?> = emptyMap(),
    // Regulation
    val implementingAct: String? = null,
    val onMarketStartYear: Int? = null,
    val productFicheUrl: String? = null,
    // Source
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    // Legacy
    val imageUrl: String? = null
)

/**
 * Result of a barcode scan operation
 */
sealed interface ScanResult {
    /**
     * Scan was successful and product was found
     */
    data class Success(
        val product: Product,
        val isNewProduct: Boolean = false
    ) : ScanResult

    /**
     * Barcode was decoded but product not found in database
     */
    data class ProductNotFound(
        val gtin: String,
        val barcodeFormat: BarcodeFormat
    ) : ScanResult

    /**
     * Could not decode barcode from image
     */
    data object DecodingFailed : ScanResult

    /**
     * Camera or permission error
     */
    data class CameraError(
        val reason: CameraErrorReason
    ) : ScanResult

    /**
     * Network error when fetching product
     */
    data class NetworkError(
        val gtin: String?,
        val message: String?
    ) : ScanResult
}

/**
 * Supported barcode formats
 */
enum class BarcodeFormat {
    EAN_13,  // Standard EAN-13 (GTIN-13)
    EAN_8,   // Short EAN-8
    UPC_A,   // UPC-A (12 digits)
    UPC_E,   // UPC-E compressed
    QR_CODE, // QR Code (may contain URL or GTIN)
    DATA_MATRIX,
    CODE_128,
    CODE_39,
    UNKNOWN
}

/**
 * Camera error reasons
 */
enum class CameraErrorReason {
    PERMISSION_DENIED,
    CAMERA_NOT_AVAILABLE,
    CAMERA_IN_USE,
    INITIALIZATION_FAILED,
    UNKNOWN
}

/**
 * History entry combining scan and product data
 */
data class HistoryEntry(
    val scan: Scan,
    val product: Product?
) {
    val displayName: String get() = scan.snapshotData.productName
    val displayBrand: String get() = scan.snapshotData.brand
    val displayModel: String? get() = scan.snapshotData.model
    val displayEnergyClass: EnergyClass? get() = scan.snapshotData.energyClass
    val displayRepairabilityIndex: Double? get() = scan.snapshotData.repairabilityIndex
    val scannedAt: Instant get() = scan.scannedAt
}

/**
 * Filters for history list
 */
data class HistoryFilter(
    val searchQuery: String = "",
    val categories: Set<ProductCategory> = emptySet(),
    val energyClasses: Set<EnergyClass> = emptySet(),
    val sortOrder: HistorySortOrder = HistorySortOrder.DATE_DESC
)

/**
 * Sort options for history
 */
enum class HistorySortOrder {
    DATE_DESC,     // Most recent first
    DATE_ASC,      // Oldest first
    NAME_ASC,      // Alphabetical A-Z
    NAME_DESC,     // Alphabetical Z-A
    ENERGY_BEST,   // Best energy class first
    REPAIRABILITY  // Highest repairability first
}
