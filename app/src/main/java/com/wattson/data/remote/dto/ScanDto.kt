package com.wattson.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.Scan
import com.wattson.domain.model.ScanSnapshot
import java.time.Instant

/**
 * Request body for creating a scan.
 * Either ean (barcode scan) or eprelCategory+eprelRegistrationNumber (EPREL scan) must be provided.
 */
data class ScanRequest(
    @SerializedName("ean") val ean: String? = null,
    @SerializedName("eprelCategory") val eprelCategory: String? = null,
    @SerializedName("eprelRegistrationNumber") val eprelRegistrationNumber: String? = null
)

/**
 * Response from scan creation or retrieval.
 */
data class ScanResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("ean") val ean: String,
    @SerializedName("productId") val productId: String?,
    @SerializedName("product") val product: ProductSnapshotDto?,
    @SerializedName("scannedAt") val scannedAt: String
) {
    fun toDomain(): Scan {
        return Scan(
            id = id,
            userId = userId,
            gtin = ean,
            scannedAt = parseInstant(scannedAt),
            snapshotData = product?.toDomain() ?: ScanSnapshot(
                productName = "Unknown Product",
                brand = "Unknown"
            )
        )
    }

    private fun parseInstant(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            Instant.now()
        }
    }
}

/**
 * Product snapshot within a scan response.
 * Mirrors the backend ProductSnapshotResponse with all environmental fields.
 */
data class ProductSnapshotDto(
    @SerializedName("name") val name: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("commercialName") val commercialName: String?,
    // Energy
    @SerializedName("energyLabel") val energyLabel: String?,
    @SerializedName("kwhPerYear") val kwhPerYear: Int?,
    @SerializedName("energyEfficiencyIndex") val energyEfficiencyIndex: Double?,
    @SerializedName("powerStandbyMode") val powerStandbyMode: Double?,
    @SerializedName("powerOffMode") val powerOffMode: Double?,
    // Noise
    @SerializedName("noiseDecibels") val noiseDecibels: Int?,
    @SerializedName("noiseClass") val noiseClass: String?,
    // Wet grip
    @SerializedName("wetGripClass") val wetGripClass: String?,
    // Sustainability
    @SerializedName("repairabilityIndex") val repairabilityIndex: Double?,
    @SerializedName("durabilityScore") val durabilityScore: Double?,
    // Regulation
    @SerializedName("implementingAct") val implementingAct: String?,
    @SerializedName("onMarketStartYear") val onMarketStartYear: Int?,
    @SerializedName("productFicheUrl") val productFicheUrl: String?,
    // EPREL details
    @SerializedName("eprelProductGroup") val eprelProductGroup: String?,
    @SerializedName("eprelDetails") val eprelDetails: Map<String, Any?>?,
    // Source
    @SerializedName("sourceName") val sourceName: String?,
    @SerializedName("sourceUrl") val sourceUrl: String?
) {
    fun toDomain(): ScanSnapshot {
        return ScanSnapshot(
            productName = name ?: "Unknown Product",
            brand = brand ?: "Unknown",
            model = model,
            category = mapCategory(category),
            commercialName = commercialName,
            energyClass = EnergyClass.fromString(energyLabel),
            kwhPerYear = kwhPerYear,
            energyEfficiencyIndex = energyEfficiencyIndex,
            powerStandbyMode = powerStandbyMode,
            powerOffMode = powerOffMode,
            noiseDecibels = noiseDecibels,
            noiseClass = noiseClass,
            wetGripClass = wetGripClass,
            repairabilityIndex = repairabilityIndex,
            durabilityScore = durabilityScore,
            eprelProductGroup = eprelProductGroup,
            eprelDetails = eprelDetails ?: emptyMap(),
            implementingAct = implementingAct,
            onMarketStartYear = onMarketStartYear,
            productFicheUrl = productFicheUrl,
            sourceName = sourceName,
            sourceUrl = sourceUrl
        )
    }

    private fun mapCategory(category: String?): ProductCategory {
        return when (category?.lowercase()) {
            // English labels
            "smartphone", "mobile", "phone" -> ProductCategory.TELEPHONIE
            "laptop", "computer", "pc" -> ProductCategory.INFORMATIQUE
            "television", "tv" -> ProductCategory.AUDIO_VIDEO
            "washing-machine", "dryer", "dishwasher" -> ProductCategory.ELECTROMENAGER
            "refrigerator", "fridge", "freezer" -> ProductCategory.ELECTROMENAGER
            "vacuum-cleaner", "kitchen-appliance" -> ProductCategory.ELECTROMENAGER
            "gaming-console", "gaming" -> ProductCategory.GAMING
            "audio", "headphones", "speaker" -> ProductCategory.AUDIO_VIDEO
            "air-conditioner", "heater" -> ProductCategory.CLIMATISATION
            // EPREL French labels
            "lave-vaisselle", "machine a laver", "lave-linge sechant",
            "seche-linge", "four", "hotte aspirante",
            "refrigerateur", "armoire refrigeree professionnelle" -> ProductCategory.ELECTROMENAGER
            "ecran electronique", "television" -> ProductCategory.ELECTRONIQUE
            "source lumineuse" -> ProductCategory.ECLAIRAGE
            "climatiseur", "chauffage", "chauffage local",
            "chauffe-eau", "chaudiere a combustible solide" -> ProductCategory.CLIMATISATION
            "pneu" -> ProductCategory.OTHER
            "electromenager" -> ProductCategory.ELECTROMENAGER
            else -> ProductCategory.OTHER
        }
    }
}

/**
 * Response for scan history listing.
 */
data class ScanHistoryResponse(
    @SerializedName("scans") val scans: List<ScanResponse>,
    @SerializedName("totalCount") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("totalPages") val totalPages: Int
)
