package com.wattson.data.repository

import com.wattson.domain.model.EnergyClass
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.ScanSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanSnapshotProductMapperTest {

    @Test
    fun `snapshot mapper preserves product details`() {
        val snapshot = ScanSnapshot(
            productName = "Eco Washer",
            brand = "Wattson",
            model = "W-100",
            category = ProductCategory.ELECTROMENAGER,
            commercialName = "Eco Washer 100",
            energyClass = EnergyClass.A,
            kwhPerYear = 42,
            repairabilityIndex = 8.5,
            sourceName = "EPREL"
        )

        val product = snapshot.toProduct(
            productId = "product-1",
            gtin = "1234567890123"
        )

        assertEquals("product-1", product.id)
        assertEquals("1234567890123", product.gtin)
        assertEquals("Eco Washer", product.name)
        assertEquals(ProductCategory.ELECTROMENAGER, product.category)
        assertEquals(EnergyClass.A, product.energyLabel)
        assertEquals(42, product.kwhPerYear)
        assertEquals(8.5, product.repairabilityIndex)
        assertEquals("EPREL", product.sourceName)
    }

    @Test
    fun `placeholder product keeps fallback semantics`() {
        val product = placeholderScannedProduct("1234567890123")

        assertEquals("1234567890123", product.id)
        assertEquals("1234567890123", product.gtin)
        assertEquals("Produit scanne (1234567890123)", product.name)
        assertEquals("Non repertorie", product.brand)
        assertEquals(ProductCategory.OTHER, product.category)
        assertNull(product.energyLabel)
    }
}
