package com.wattson.ui.screens.documents

import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentMetadata
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.OcrStatus
import com.wattson.domain.model.ProductCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentsPresentationFactoryTest {

    @Test
    fun `create groups documents by year and computes summary`() {
        val documents = listOf(
            testDocument(
                id = "1",
                gtin = "111",
                uploadedAt = Instant.parse("2026-03-01T10:00:00Z"),
                warrantyEndDate = LocalDate.parse("2026-12-31")
            ),
            testDocument(
                id = "2",
                gtin = "222",
                uploadedAt = Instant.parse("2025-01-10T10:00:00Z")
            ),
            testDocument(
                id = "3",
                gtin = "111",
                uploadedAt = Instant.parse("2025-02-10T10:00:00Z")
            )
        )

        val presentation = DocumentsPresentationFactory.create(
            documents = documents,
            today = LocalDate.parse("2026-03-25"),
            zoneId = ZoneId.of("UTC")
        )

        assertEquals(listOf(2026, 2025), presentation.availableYears)
        assertEquals(setOf(2026), presentation.expandedYears)
        assertEquals(3, presentation.totalDocuments)
        assertEquals(2, presentation.totalDevices)
        assertEquals(1, presentation.activeWarranties)
        assertEquals(1, presentation.documentsByYear.getValue(2026).size)
    }

    @Test
    fun `filter by query matches product merchant and type`() {
        val receipt = testDocument(
            id = "1",
            productName = "Lave-vaisselle",
            merchant = "Boulanger"
        )
        val manual = testDocument(
            id = "2",
            productName = "Guide TV",
            type = DocumentType.MANUEL
        )

        assertEquals(
            listOf(receipt),
            DocumentsPresentationFactory.filterByQuery(listOf(receipt, manual), "boul")
        )
        assertEquals(
            listOf(manual),
            DocumentsPresentationFactory.filterByQuery(listOf(receipt, manual), "manuel")
        )
    }

    @Test
    fun `recent pending OCR detection only keeps fresh processing documents`() {
        val now = Instant.parse("2026-03-25T10:00:00Z")
        val recentPending = testDocument(
            id = "1",
            uploadedAt = now.minusSeconds(30),
            ocrStatus = OcrStatus.PROCESSING
        )
        val oldPending = testDocument(
            id = "2",
            uploadedAt = now.minusSeconds(500),
            ocrStatus = OcrStatus.PROCESSING
        )

        assertTrue(
            DocumentsPresentationFactory.hasRecentPendingOcr(
                documents = listOf(recentPending, oldPending),
                now = now
            )
        )
    }

    @Test
    fun `upload validator maps quota and size failures`() {
        assertEquals(
            "Fichier trop volumineux (max 10 Mo).",
            DocumentUploadValidator.validate(
                fileName = "invoice.pdf",
                mimeType = "application/pdf",
                fileSize = 10L * 1024L * 1024L + 1L
            )
        )
        assertEquals(
            "Limite de documents atteinte.",
            DocumentUploadValidator.mapErrorMessage("HTTP 429 quota exceeded")
        )
    }

    private fun testDocument(
        id: String,
        productName: String = "Produit",
        type: DocumentType = DocumentType.FACTURE,
        gtin: String? = null,
        merchant: String? = null,
        uploadedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        warrantyEndDate: LocalDate? = null,
        ocrStatus: OcrStatus? = null
    ): Document {
        return Document(
            id = id,
            userId = "user",
            type = type,
            productName = productName,
            productCategory = ProductCategory.ELECTROMENAGER,
            gtin = gtin,
            fileUrl = "https://example.com/$id",
            thumbnailUrl = null,
            documentDate = LocalDate.parse("2026-01-01"),
            metadata = DocumentMetadata(
                merchant = merchant,
                warrantyEndDate = warrantyEndDate
            ),
            ocrStatus = ocrStatus,
            uploadedAt = uploadedAt
        )
    }
}
