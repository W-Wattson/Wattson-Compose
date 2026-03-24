package com.wattson.data.repository

import com.google.gson.Gson
import com.wattson.data.remote.api.WattsonApi
import com.wattson.data.remote.dto.ProductDto
import com.wattson.data.remote.dto.ScanRequest
import com.wattson.data.remote.dto.ScanResponse
import com.wattson.domain.model.DEFAULT_EPREL_CATEGORY_IDS
import com.wattson.domain.model.Product
import com.wattson.domain.model.ResolvedEprelProduct
import com.wattson.domain.model.Scan
import com.wattson.domain.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for product and scan operations.
 * Handles communication with the backend API.
 */
@Singleton
class ProductRepository @Inject constructor(
    private val api: WattsonApi
) {
    private val gson = Gson()

    /**
     * Fetches a product by its EAN/GTIN barcode.
     */
    suspend fun getProductByEan(ean: String): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductByEan(ean)
            if (response.isSuccessful) {
                parseProductResponse(response.body(), ean)
            } else if (response.code() == 404) {
                Result.failure(ProductNotFoundException(ean))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "getProductByEan error", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches a product by its internal ID.
     */
    suspend fun getProductById(id: String): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductById(id)
            if (response.isSuccessful) {
                parseProductResponse(response.body(), id)
            } else if (response.code() == 404) {
                Result.failure(ProductNotFoundException(id))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "getProductById error", e)
            Result.failure(e)
        }
    }

    /**
     * Parse ResponseBody manually to avoid chunked encoding issues with ADB reverse.
     */
    private fun parseProductResponse(body: ResponseBody?, identifier: String): Result<Product> {
        if (body == null) {
            return Result.failure(Exception("Empty response body"))
        }
        return try {
            val jsonString = body.string()
            android.util.Log.d("ProductRepository", "Received JSON: ${jsonString.take(200)}...")
            val productDto = gson.fromJson(jsonString, ProductDto::class.java)
            Result.success(productDto.toDomain())
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "JSON parse error", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a scan record and returns the scan result.
     * Parses the scan response which contains product snapshot data,
     * then tries to enrich with full product details from the catalog.
     */
    suspend fun scanProduct(userId: String, ean: String): ScanResult = withContext(Dispatchers.IO) {
        // Normalize barcode to EAN-13 format
        val normalizedEan = normalizeToEan13(ean)

        // Validate EAN format
        if (normalizedEan == null) {
            return@withContext ScanResult.NetworkError(
                gtin = ean,
                message = "Format de code-barres invalide. Seuls les codes EAN-13, EAN-8 et UPC-A sont supportés."
            )
        }

        return@withContext try {
            // Register the scan and parse response (contains product snapshot)
            val scanResponse = api.createScan(userId, ScanRequest(ean = normalizedEan))

            if (scanResponse.isSuccessful) {
                android.util.Log.d("ProductRepository", "Scan registered: ${scanResponse.code()}")

                // Parse scan response to get product snapshot
                val scanData = parseScanResponse(scanResponse.body())
                val snapshot = scanData?.product

                // Try to get full product details from catalog
                var product: Product? = null
                try {
                    val productResponse = api.getProductByEan(normalizedEan)
                    if (productResponse.isSuccessful && productResponse.body() != null) {
                        val jsonString = productResponse.body()!!.string()
                        android.util.Log.d("ProductRepository", "Product JSON: ${jsonString.take(100)}...")
                        val productDto = gson.fromJson(jsonString, ProductDto::class.java)
                        product = productDto.toDomain()
                        android.util.Log.d("ProductRepository", "Product found in catalog: ${product.name}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProductRepository", "Catalog lookup failed, using scan snapshot", e)
                }

                // Use catalog product if found, otherwise build from scan snapshot
                if (product != null) {
                    ScanResult.Success(product = product, isNewProduct = false)
                } else if (snapshot != null) {
                    android.util.Log.d("ProductRepository", "Using scan snapshot: ${snapshot.name}")
                    val snapshotDomain = snapshot.toDomain()
                    ScanResult.Success(
                        product = Product(
                            id = scanData.productId ?: normalizedEan,
                            gtin = normalizedEan,
                            name = snapshotDomain.productName,
                            brand = snapshotDomain.brand,
                            model = snapshotDomain.model,
                            category = snapshotDomain.category,
                            commercialName = snapshotDomain.commercialName,
                            energyLabel = snapshotDomain.energyClass,
                            kwhPerYear = snapshotDomain.kwhPerYear,
                            energyEfficiencyIndex = snapshotDomain.energyEfficiencyIndex,
                            powerStandbyMode = snapshotDomain.powerStandbyMode,
                            powerOffMode = snapshotDomain.powerOffMode,
                            noiseDecibels = snapshotDomain.noiseDecibels,
                            noiseClass = snapshotDomain.noiseClass,
                            wetGripClass = snapshotDomain.wetGripClass,
                            repairabilityIndex = snapshotDomain.repairabilityIndex,
                            eprelProductGroup = snapshotDomain.eprelProductGroup,
                            eprelDetails = snapshotDomain.eprelDetails,
                            implementingAct = snapshotDomain.implementingAct,
                            onMarketStartYear = snapshotDomain.onMarketStartYear,
                            productFicheUrl = snapshotDomain.productFicheUrl,
                            sourceName = snapshotDomain.sourceName,
                            sourceUrl = snapshotDomain.sourceUrl
                        ),
                        isNewProduct = true
                    )
                } else {
                    android.util.Log.d("ProductRepository", "No product data available, creating placeholder")
                    ScanResult.Success(
                        product = Product(
                            id = normalizedEan,
                            gtin = normalizedEan,
                            name = "Produit scanné ($normalizedEan)",
                            brand = "Non répertorié",
                            model = null,
                            category = com.wattson.domain.model.ProductCategory.OTHER,
                            energyLabel = null
                        ),
                        isNewProduct = true
                    )
                }
            } else if (scanResponse.code() == 404) {
                ScanResult.ProductNotFound(
                    gtin = normalizedEan,
                    barcodeFormat = com.wattson.domain.model.BarcodeFormat.EAN_13
                )
            } else {
                ScanResult.NetworkError(gtin = normalizedEan, message = "Erreur API: ${scanResponse.code()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Scan error", e)
            ScanResult.NetworkError(gtin = normalizedEan, message = "Erreur: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Parse scan response body manually to avoid chunked encoding issues with ADB reverse.
     */
    private fun parseScanResponse(body: ResponseBody?): ScanResponse? {
        if (body == null) return null
        return try {
            val jsonString = body.string()
            android.util.Log.d("ProductRepository", "Scan response JSON: ${jsonString.take(200)}...")
            gson.fromJson(jsonString, ScanResponse::class.java)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Failed to parse scan response", e)
            null
        }
    }

    /**
     * Fetches a product directly from EPREL by category and registration number.
     * Bypasses barcode scanning — useful for testing and products without GTIN in EPREL.
     */
    suspend fun getProductByEprelId(category: String, registrationNumber: String): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductByEprelId(category, registrationNumber)
            if (response.isSuccessful) {
                parseProductResponse(response.body(), "$category/$registrationNumber")
                    .mapCatching { product ->
                        if (product.isInvalidEprelPlaceholder()) {
                            throw ProductNotFoundException("$category/$registrationNumber")
                        }
                        product
                    }
            } else if (response.code() == 404) {
                Result.failure(ProductNotFoundException("$category/$registrationNumber"))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "getProductByEprelId error", e)
            Result.failure(e)
        }
    }

    suspend fun resolveProductByEprelRegistrationNumber(
        registrationNumber: String,
        preferredCategories: List<String> = emptyList()
    ): Result<ResolvedEprelProduct> = withContext(Dispatchers.IO) {
        val orderedCategories = (preferredCategories + DEFAULT_EPREL_CATEGORY_IDS).distinct()
        var lastError: Throwable? = null

        for (category in orderedCategories) {
            val result = getProductByEprelId(category, registrationNumber)
            result.fold(
                onSuccess = { product ->
                    return@withContext Result.success(
                        ResolvedEprelProduct(
                            category = category,
                            registrationNumber = registrationNumber,
                            product = product
                        )
                    )
                },
                onFailure = { error ->
                    lastError = error
                }
            )
        }

        Result.failure(lastError ?: ProductNotFoundException(registrationNumber))
    }

    /**
     * Registers an EPREL scan in the scan history.
     * Sends an EPREL-based scan request to the scan-service so it appears in history.
     */
    suspend fun registerEprelScan(userId: String, category: String, registrationNumber: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = ScanRequest(
                    eprelCategory = category,
                    eprelRegistrationNumber = registrationNumber
                )
                val response = api.createScan(userId, request)
                if (response.isSuccessful) {
                    android.util.Log.d("ProductRepository", "EPREL scan registered: $category/$registrationNumber")
                } else {
                    android.util.Log.w("ProductRepository", "Failed to register EPREL scan: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.w("ProductRepository", "Failed to register EPREL scan", e)
            }
        }
    }

    /**
     * Fetches a specific scan by its ID, returning it as a Product built from the snapshot.
     * Used when navigating from history to product detail.
     */
    suspend fun getProductFromScan(userId: String, scanId: String): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = api.getScanById(userId, scanId)
            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!
                val snapshot = scanResponse.product
                if (snapshot != null) {
                    val domain = snapshot.toDomain()
                    Result.success(Product(
                        id = scanResponse.productId ?: scanId,
                        gtin = scanResponse.ean,
                        name = domain.productName,
                        brand = domain.brand,
                        model = domain.model,
                        category = domain.category,
                        commercialName = domain.commercialName,
                        energyLabel = domain.energyClass,
                        kwhPerYear = domain.kwhPerYear,
                        energyEfficiencyIndex = domain.energyEfficiencyIndex,
                        powerStandbyMode = domain.powerStandbyMode,
                        powerOffMode = domain.powerOffMode,
                        noiseDecibels = domain.noiseDecibels,
                        noiseClass = domain.noiseClass,
                        wetGripClass = domain.wetGripClass,
                        repairabilityIndex = domain.repairabilityIndex,
                        eprelProductGroup = domain.eprelProductGroup,
                        eprelDetails = domain.eprelDetails,
                        implementingAct = domain.implementingAct,
                        onMarketStartYear = domain.onMarketStartYear,
                        productFicheUrl = domain.productFicheUrl,
                        sourceName = domain.sourceName,
                        sourceUrl = domain.sourceUrl
                    ))
                } else {
                    Result.failure(Exception("Scan has no product snapshot"))
                }
            } else if (response.code() == 404) {
                Result.failure(ProductNotFoundException(scanId))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "getProductFromScan error", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the user's scan history.
     */
    suspend fun getScanHistory(
        userId: String,
        page: Int = 0,
        size: Int = 20
    ): Result<List<Scan>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getScanHistory(userId, page, size)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val scans = body.scans.map { it.toDomain() }
                    Result.success(scans)
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Normalize barcode to EAN-13 format.
 * - UPC-A (12 digits) -> prepend 0
 * - EAN-8 (8 digits) -> prepend 00000
 * - EAN-13 (13 digits) -> return as-is
 * - Others -> return null (invalid)
 */
private fun normalizeToEan13(barcode: String): String? {
    // Only digits allowed
    if (!barcode.all { it.isDigit() }) return null

    return when (barcode.length) {
        13 -> barcode                    // EAN-13
        12 -> "0$barcode"                // UPC-A -> EAN-13
        8 -> "00000$barcode"             // EAN-8 -> EAN-13
        else -> null                     // Invalid format
    }
}

/**
 * Exception thrown when a product is not found.
 */
class ProductNotFoundException(val identifier: String) : Exception("Product not found: $identifier")

private fun Product.isInvalidEprelPlaceholder(): Boolean {
    val detailCode = eprelDetails["code"]?.toString()
    val detailMessage = eprelDetails["message"]?.toString()

    if (detailCode.equals("NOT_FOUND", ignoreCase = true)) {
        return true
    }

    if (detailMessage?.contains("not found", ignoreCase = true) == true) {
        return true
    }

    return name.equals("Unknown", ignoreCase = true) || name.equals("Unknown Product", ignoreCase = true)
}
