package com.wattson.data.repository

import com.wattson.domain.model.Product
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.ScanSnapshot

/**
 * Rebuilds domain products from scan snapshots when the full catalog payload is unavailable.
 */
internal fun ScanSnapshot.toProduct(productId: String, gtin: String): Product {
    return Product(
        id = productId,
        gtin = gtin,
        name = productName,
        brand = brand,
        model = model,
        category = category,
        commercialName = commercialName,
        energyLabel = energyClass,
        kwhPerYear = kwhPerYear,
        energyEfficiencyIndex = energyEfficiencyIndex,
        powerStandbyMode = powerStandbyMode,
        powerOffMode = powerOffMode,
        noiseDecibels = noiseDecibels,
        noiseClass = noiseClass,
        wetGripClass = wetGripClass,
        repairabilityIndex = repairabilityIndex,
        eprelProductGroup = eprelProductGroup,
        eprelDetails = eprelDetails,
        implementingAct = implementingAct,
        onMarketStartYear = onMarketStartYear,
        productFicheUrl = productFicheUrl,
        sourceName = sourceName,
        sourceUrl = sourceUrl
    )
}

/**
 * Builds the placeholder product currently shown when the backend cannot resolve the scan.
 */
internal fun placeholderScannedProduct(gtin: String): Product {
    return Product(
        id = gtin,
        gtin = gtin,
        name = "Produit scanne ($gtin)",
        brand = "Non repertorie",
        model = null,
        category = ProductCategory.OTHER,
        energyLabel = null
    )
}
