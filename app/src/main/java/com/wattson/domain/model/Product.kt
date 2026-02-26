package com.wattson.domain.model

import java.time.Instant

/**
 * Represents a product in the Wattson system, including optional metrics and imagery.
 */
data class Product(
    val id: String,
    val gtin: String, // EAN/GTIN-13
    val name: String,
    val brand: String,
    val model: String?,
    val category: ProductCategory,
    val commercialName: String? = null,
    // Energy
    val energyLabel: EnergyClass?,
    val kwhPerYear: Int? = null,
    val energyEfficiencyIndex: Double? = null,
    val powerStandbyMode: Double? = null, // Watts
    val powerOffMode: Double? = null, // Watts
    // Noise
    val noiseDecibels: Int? = null, // dB(A)
    val noiseClass: String? = null, // A-G
    // Wet grip (tyres)
    val wetGripClass: String? = null, // A-G
    // Sustainability
    val repairabilityIndex: Double? = null, // 0-10 scale
    // Regulation
    val implementingAct: String? = null, // EU regulation reference
    val onMarketStartYear: Int? = null,
    val productFicheUrl: String? = null, // Link to EPREL product fiche
    // EPREL category-specific details
    val eprelProductGroup: String? = null, // e.g., "lightsources", "electronicdisplays"
    val eprelDetails: Map<String, Any?> = emptyMap(), // All raw EPREL fields for the category
    // Source
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    // Legacy
    val characteristics: ProductCharacteristics = ProductCharacteristics(),
    val metrics: ProductMetrics? = null,
    val imageUrl: String? = null,
    val updatedAt: Instant = Instant.now()
)

/**
 * Product categories supported by the app.
 */
enum class ProductCategory {
    ELECTRONIQUE,
    ELECTROMENAGER,
    ECLAIRAGE,
    GAMING,
    CLIMATISATION,
    INFORMATIQUE,
    TELEPHONIE,
    AUDIO_VIDEO,
    OTHER
}

/**
 * Energy efficiency class (A to G)
 * As defined in EU energy labeling regulations
 */
enum class EnergyClass {
    A, B, C, D, E, F, G;

    companion object {
        fun fromString(value: String?): EnergyClass? {
            return value?.uppercase()?.let { 
                entries.find { entry -> entry.name == it }
            }
        }
    }
}

/**
 * Product characteristics including capacity and durability metrics
 */
data class ProductCharacteristics(
    val capacityKg: Int? = null,
    val enduranceHours: Int? = null, // Battery endurance in hours
    val dropResistanceClass: ResistanceClass? = null,
    val additionalInfo: Map<String, String> = emptyMap()
)

/**
 * Drop/shock resistance class (A to E)
 */
enum class ResistanceClass {
    A, B, C, D, E
}

/**
 * Detailed product metrics from the 'product_metrics' collection
 */
data class ProductMetrics(
    val version: Int,
    val completeness: Double, // 0-1 score indicating data completeness
    val scores: ProductScores,
    val sources: List<MetricSource>,
    val fetchedAt: Instant
)

/**
 * Aggregated scores for various sustainability metrics
 */
data class ProductScores(
    val energy: EnergyScore?,
    val carbon: CarbonScore?,
    val durability: DurabilityScore?,
    val repairability: RepairabilityScore?
)

/**
 * Energy efficiency score details
 */
data class EnergyScore(
    val value: String, // Energy class letter
    val kwhPerYear: Int? // Estimated annual consumption
)

/**
 * Carbon footprint score details
 */
data class CarbonScore(
    val value: Double, // Score 0-100
    val kgLifetime: Double? // CO2 kg over product lifetime
)

/**
 * Durability/longevity score details
 */
data class DurabilityScore(
    val value: Double // Score 0-100
)

/**
 * Repairability score details
 */
data class RepairabilityScore(
    val value: Double, // Index 0-10
    val repairabilityClass: String? // Class letter
)

/**
 * Source of metric data
 */
data class MetricSource(
    val type: String, // e.g., "ADEME", "manufacturer", "EU_database"
    val fetchedAt: Instant
)

/**
 * Calculated global score (A to G or numeric)
 * Combining all metrics weighted by user preferences
 */
data class GlobalScore(
    val letter: String, // A, B, C, D, E, F, G
    val label: String, // "Excellent", "Très bon", "Bon", "Moyen", "Passable", "Médiocre", "Mauvais"
    val numericValue: Double // 0-100
)

/**
 * Calculates the global score label based on the letter grade
 */
fun String.toScoreLabel(): String {
    return when (this.uppercase()) {
        "A" -> "Excellent"
        "B" -> "Très bon"
        "C" -> "Bon"
        "D" -> "Moyen"
        "E" -> "Passable"
        "F" -> "Médiocre"
        "G" -> "Mauvais"
        else -> "Non évalué"
    }
}
