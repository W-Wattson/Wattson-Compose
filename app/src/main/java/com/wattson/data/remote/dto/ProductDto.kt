package com.wattson.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import java.time.Instant

/**
 * Product data transfer object from backend API.
 * Contains full environmental and sustainability data.
 */
data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("gtin") val gtin: String,
    @SerializedName("name") val name: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("commercialName") val commercialName: String?,
    @SerializedName("energyLabel") val energyLabel: EnergyLabelDto?,
    @SerializedName("noiseLevel") val noiseLevel: NoiseLevelDto?,
    @SerializedName("wetGrip") val wetGrip: WetGripDto?,
    @SerializedName("repairabilityScore") val repairabilityScore: RepairabilityDto?,
    @SerializedName("durabilityScore") val durabilityScore: DurabilityDto?,
    @SerializedName("carbonFootprint") val carbonFootprint: CarbonFootprintDto?,
    @SerializedName("eprelProductGroup") val eprelProductGroup: String?,
    @SerializedName("eprelDetails") val eprelDetails: Map<String, Any?>?,
    @SerializedName("implementingAct") val implementingAct: String?,
    @SerializedName("onMarketStartYear") val onMarketStartYear: Int?,
    @SerializedName("productFicheUrl") val productFicheUrl: String?,
    @SerializedName("sourceInfo") val sourceInfo: SourceInfoDto?,
    @SerializedName("updatedAt") val updatedAt: String?
) {
    fun toDomain(): Product {
        return Product(
            id = id,
            gtin = gtin,
            name = name ?: "Unknown Product",
            brand = brand ?: "Unknown",
            model = model,
            category = mapCategory(category),
            commercialName = commercialName,
            energyLabel = EnergyClass.fromString(energyLabel?.label),
            kwhPerYear = energyLabel?.kwhPerYear,
            energyEfficiencyIndex = energyLabel?.energyEfficiencyIndex,
            powerStandbyMode = energyLabel?.powerStandbyMode,
            powerOffMode = energyLabel?.powerOffMode,
            noiseDecibels = noiseLevel?.decibelValue,
            noiseClass = noiseLevel?.noiseClass,
            wetGripClass = wetGrip?.gripClass,
            repairabilityIndex = repairabilityScore?.index,
            eprelProductGroup = eprelProductGroup,
            eprelDetails = eprelDetails ?: emptyMap(),
            implementingAct = implementingAct,
            onMarketStartYear = onMarketStartYear,
            productFicheUrl = productFicheUrl,
            sourceName = sourceInfo?.sourceName,
            sourceUrl = sourceInfo?.sourceUrl,
            updatedAt = updatedAt?.let { parseInstant(it) } ?: Instant.now()
        )
    }

    private fun mapCategory(category: String?): ProductCategory {
        return when (category?.lowercase()) {
            "smartphone", "mobile", "phone", "telephone" -> ProductCategory.TELEPHONIE
            "laptop", "computer", "pc", "informatique", "ordinateur portable", "tablette" -> ProductCategory.INFORMATIQUE
            "television", "tv", "ecran electronique", "ecran" -> ProductCategory.ELECTRONIQUE
            "washing-machine", "dryer", "dishwasher" -> ProductCategory.ELECTROMENAGER
            "refrigerator", "fridge", "freezer" -> ProductCategory.ELECTROMENAGER
            "vacuum-cleaner", "kitchen-appliance" -> ProductCategory.ELECTROMENAGER
            "lave-vaisselle", "machine a laver", "lave-linge sechant",
            "seche-linge", "four", "hotte aspirante",
            "refrigerateur", "armoire refrigeree professionnelle",
            "electromenager" -> ProductCategory.ELECTROMENAGER
            "gaming-console", "gaming" -> ProductCategory.GAMING
            "audio", "headphones", "speaker", "casque audio", "enceinte" -> ProductCategory.AUDIO_VIDEO
            "air-conditioner", "heater", "climatiseur", "chauffage",
            "chauffage local", "chauffe-eau",
            "chaudiere a combustible solide" -> ProductCategory.CLIMATISATION
            "source lumineuse" -> ProductCategory.ECLAIRAGE
            "pneu" -> ProductCategory.OTHER
            else -> ProductCategory.OTHER
        }
    }

    private fun parseInstant(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            Instant.now()
        }
    }
}

data class EnergyLabelDto(
    @SerializedName("label") val label: String?,
    @SerializedName("kwhPerYear") val kwhPerYear: Int?,
    @SerializedName("energyEfficiencyIndex") val energyEfficiencyIndex: Double?,
    @SerializedName("powerStandbyMode") val powerStandbyMode: Double?,
    @SerializedName("powerOffMode") val powerOffMode: Double?
)

data class NoiseLevelDto(
    @SerializedName("decibelValue") val decibelValue: Int?,
    @SerializedName("noiseClass") val noiseClass: String?
)

data class WetGripDto(
    @SerializedName("gripClass") val gripClass: String?
)

data class RepairabilityDto(
    @SerializedName("index") val index: Double?,
    @SerializedName("classLabel") val classLabel: String?
)

data class DurabilityDto(
    @SerializedName("value") val value: Double?,
    @SerializedName("resistanceClass") val resistanceClass: String?,
    @SerializedName("enduranceHours") val enduranceHours: Int?
)

data class CarbonFootprintDto(
    @SerializedName("kgCo2Lifetime") val kgCo2Lifetime: Double?,
    @SerializedName("kgCo2Manufacturing") val kgCo2Manufacturing: Double?
)

data class SourceInfoDto(
    @SerializedName("sourceName") val sourceName: String?,
    @SerializedName("sourceUrl") val sourceUrl: String?,
    @SerializedName("fetchedAt") val fetchedAt: String?,
    @SerializedName("completeness") val completeness: Double?
)
