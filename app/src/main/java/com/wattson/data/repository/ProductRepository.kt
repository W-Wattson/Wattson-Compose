package com.wattson.data.repository

import com.google.gson.Gson
import com.wattson.data.remote.api.WattsonApi
import com.wattson.data.remote.dto.ProductDto
import com.wattson.data.remote.dto.ScanRequest
import com.wattson.data.remote.dto.ScanResponse
import com.wattson.domain.model.BarcodeFormat
import com.wattson.domain.model.DEFAULT_EPREL_CATEGORY_IDS
import com.wattson.domain.model.Product
import com.wattson.domain.model.ResolvedEprelProduct
import com.wattson.domain.model.Scan
import com.wattson.domain.model.ScanResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/**
 * Repository for product catalog lookups and scan registration.
 */
@Singleton
class ProductRepository @Inject constructor(
    private val api: WattsonApi
) {
    private val gson = Gson()

    /**
     * Fetches a product by its EAN or GTIN.
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
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "getProductByEan error", exception)
            Result.failure(exception)
        }
    }

    /**
     * Fetches a product by its internal identifier.
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
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "getProductById error", exception)
            Result.failure(exception)
        }
    }

    /**
     * Creates a scan record and resolves the best product payload available for the UI.
     */
    suspend fun scanProduct(userId: String, ean: String): ScanResult = withContext(Dispatchers.IO) {
        val normalizedEan = BarcodeNormalizer.normalizeToEan13(ean)
            ?: return@withContext ScanResult.NetworkError(
                gtin = ean,
                message = "Format de code-barres invalide. Seuls les codes EAN-13, EAN-8 et UPC-A sont supportes."
            )

        try {
            val scanResponse = api.createScan(userId, ScanRequest(ean = normalizedEan))

            if (scanResponse.isSuccessful) {
                android.util.Log.d("ProductRepository", "Scan registered: ${scanResponse.code()}")

                val scanData = parseScanResponse(scanResponse.body())
                val catalogProduct = lookupCatalogProduct(normalizedEan)
                val snapshotProduct = scanData?.product?.toDomain()?.toProduct(
                    productId = scanData.productId ?: normalizedEan,
                    gtin = normalizedEan
                )

                when {
                    catalogProduct != null -> {
                        ScanResult.Success(product = catalogProduct, isNewProduct = false)
                    }

                    snapshotProduct != null -> {
                        android.util.Log.d(
                            "ProductRepository",
                            "Using scan snapshot: ${snapshotProduct.name}"
                        )
                        ScanResult.Success(product = snapshotProduct, isNewProduct = true)
                    }

                    else -> {
                        android.util.Log.d(
                            "ProductRepository",
                            "No product data available, creating placeholder"
                        )
                        ScanResult.Success(
                            product = placeholderScannedProduct(normalizedEan),
                            isNewProduct = true
                        )
                    }
                }
            } else if (scanResponse.code() == 404) {
                ScanResult.ProductNotFound(
                    gtin = normalizedEan,
                    barcodeFormat = BarcodeFormat.EAN_13
                )
            } else {
                ScanResult.NetworkError(
                    gtin = normalizedEan,
                    message = "Erreur API: ${scanResponse.code()}"
                )
            }
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "Scan error", exception)
            ScanResult.NetworkError(
                gtin = normalizedEan,
                message = "Erreur: ${exception.message ?: exception.javaClass.simpleName}"
            )
        }
    }

    /**
     * Fetches a product directly from EPREL.
     */
    suspend fun getProductByEprelId(
        category: String,
        registrationNumber: String
    ): Result<Product> = withContext(Dispatchers.IO) {
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
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "getProductByEprelId error", exception)
            Result.failure(exception)
        }
    }

    /**
     * Resolves a product by trying the preferred EPREL categories first, then the defaults.
     */
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
                    android.util.Log.d(
                        "ProductRepository",
                        "EPREL scan registered: $category/$registrationNumber"
                    )
                } else {
                    android.util.Log.w(
                        "ProductRepository",
                        "Failed to register EPREL scan: ${response.code()}"
                    )
                }
            } catch (exception: Exception) {
                android.util.Log.w(
                    "ProductRepository",
                    "Failed to register EPREL scan",
                    exception
                )
            }
        }
    }

    /**
     * Fetches a product reconstructed from a previously stored scan snapshot.
     */
    suspend fun getProductFromScan(
        userId: String,
        scanId: String
    ): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = api.getScanById(userId, scanId)
            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!
                val snapshot = scanResponse.product
                if (snapshot != null) {
                    Result.success(
                        snapshot.toDomain().toProduct(
                            productId = scanResponse.productId ?: scanId,
                            gtin = scanResponse.ean
                        )
                    )
                } else {
                    Result.failure(Exception("Scan has no product snapshot"))
                }
            } else if (response.code() == 404) {
                Result.failure(ProductNotFoundException(scanId))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "getProductFromScan error", exception)
            Result.failure(exception)
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
                    Result.success(body.scans.map { it.toDomain() })
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Parses a product payload manually to avoid issues caused by chunked responses.
     */
    private fun parseProductResponse(
        body: ResponseBody?,
        identifier: String
    ): Result<Product> {
        if (body == null) {
            return Result.failure(Exception("Empty response body"))
        }

        return try {
            val json = body.string()
            android.util.Log.d(
                "ProductRepository",
                "Received JSON for $identifier: ${json.take(200)}..."
            )
            Result.success(gson.fromJson(json, ProductDto::class.java).toDomain())
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "JSON parse error", exception)
            Result.failure(exception)
        }
    }

    /**
     * Parses the scan payload manually to keep behavior aligned with the current backend setup.
     */
    private fun parseScanResponse(body: ResponseBody?): ScanResponse? {
        if (body == null) {
            return null
        }

        return try {
            val json = body.string()
            android.util.Log.d(
                "ProductRepository",
                "Scan response JSON: ${json.take(200)}..."
            )
            gson.fromJson(json, ScanResponse::class.java)
        } catch (exception: Exception) {
            android.util.Log.e("ProductRepository", "Failed to parse scan response", exception)
            null
        }
    }

    private suspend fun lookupCatalogProduct(normalizedEan: String): Product? {
        return try {
            val productResponse = api.getProductByEan(normalizedEan)
            if (productResponse.isSuccessful && productResponse.body() != null) {
                val json = productResponse.body()!!.string()
                android.util.Log.d(
                    "ProductRepository",
                    "Product JSON: ${json.take(100)}..."
                )
                gson.fromJson(json, ProductDto::class.java).toDomain().also { product ->
                    android.util.Log.d(
                        "ProductRepository",
                        "Product found in catalog: ${product.name}"
                    )
                }
            } else {
                null
            }
        } catch (exception: Exception) {
            android.util.Log.w(
                "ProductRepository",
                "Catalog lookup failed, using scan snapshot",
                exception
            )
            null
        }
    }
}

/**
 * Exception thrown when a product cannot be resolved.
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

    return name.equals("Unknown", ignoreCase = true) ||
        name.equals("Unknown Product", ignoreCase = true)
}
