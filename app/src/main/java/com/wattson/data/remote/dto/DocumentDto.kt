package com.wattson.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentMetadata
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.OcrData
import com.wattson.domain.model.OcrStatus
import com.wattson.domain.model.ProductCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Document response from backend API.
 */
data class DocumentResponse(
    @SerializedName("id") val id: String,
    @SerializedName("filename") val filename: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("fileSize") val fileSize: Long,
    @SerializedName("documentType") val documentType: String?,
    @SerializedName("ocrData") val ocrData: OcrDataDto?,
    @SerializedName("uploadedAt") val uploadedAt: String?,
    @SerializedName("ocrStatus") val ocrStatus: String?
) {
    fun toDomain(userId: String): Document {
        val uploadInstant = uploadedAt?.let { parseInstant(it) } ?: Instant.now()
        val uploadDate = uploadInstant.atZone(ZoneId.systemDefault()).toLocalDate()

        val mappedType = mapDocumentType(documentType)
        val purchaseDate = ocrData?.purchaseDate?.let { parseLocalDate(it) }

        val warrantyStartDate = if (mappedType == DocumentType.GARANTIE) {
            purchaseDate
        } else null

        val warrantyEndDate = if (mappedType == DocumentType.GARANTIE && purchaseDate != null) {
            purchaseDate.plusMonths(24)
        } else null

        return Document(
            id = id,
            userId = userId,
            type = mappedType,
            productName = ocrData?.merchantName ?: filename ?: "Document sans nom",
            productCategory = ProductCategory.OTHER,
            gtin = ocrData?.extractedGtin,
            fileUrl = "",
            thumbnailUrl = null,
            documentDate = purchaseDate ?: uploadDate,
            metadata = DocumentMetadata(
                merchant = ocrData?.merchantName,
                purchaseDate = purchaseDate,
                totalAmount = ocrData?.totalAmount,
                currency = ocrData?.currency ?: "EUR",
                warrantyStartDate = warrantyStartDate,
                warrantyEndDate = warrantyEndDate
            ),
            ocrData = ocrData?.toDomain(),
            ocrStatus = mapOcrStatus(ocrStatus),
            uploadedAt = uploadInstant,
            filename = filename,
            mimeType = mimeType,
            fileSize = fileSize
        )
    }

    private fun mapDocumentType(type: String?): DocumentType {
        return when (type?.uppercase()) {
            "INVOICE" -> DocumentType.FACTURE
            "RECEIPT" -> DocumentType.TICKET
            "WARRANTY" -> DocumentType.GARANTIE
            "MANUAL" -> DocumentType.MANUEL
            else -> DocumentType.OTHER
        }
    }

    private fun mapOcrStatus(status: String?): OcrStatus? {
        return when (status?.uppercase()) {
            null -> null
            "PENDING", "QUEUED", "WAITING" -> OcrStatus.PENDING
            "PROCESSING", "IN_PROGRESS", "RUNNING" -> OcrStatus.PROCESSING
            "COMPLETED", "DONE", "SUCCESS" -> OcrStatus.COMPLETED
            "PARTIAL", "PARTIALLY_COMPLETED" -> OcrStatus.PARTIAL
            "FAILED", "ERROR" -> OcrStatus.FAILED
            else -> OcrStatus.UNKNOWN
        }
    }

    private fun parseInstant(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            Instant.now()
        }
    }

    private fun parseLocalDate(value: String): LocalDate {
        return try {
            LocalDate.parse(value)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}

/**
 * OCR data from document processing.
 */
data class OcrDataDto(
    @SerializedName("extractedGtin") val extractedGtin: String?,
    @SerializedName("merchantName") val merchantName: String?,
    @SerializedName("purchaseDate") val purchaseDate: String?,
    @SerializedName("totalAmount") val totalAmount: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("confidence") val confidence: Double
) {
    fun toDomain(): OcrData {
        return OcrData(
            rawText = "",
            merchant = merchantName,
            totalAmount = totalAmount,
            lines = emptyList(),
            confidence = confidence
        )
    }
}

/**
 * Response for document listing.
 */
data class DocumentListResponse(
    @SerializedName("documents") val documents: List<DocumentResponse>,
    @SerializedName("total") val total: Long,
    @SerializedName("limit") val limit: Int,
    @SerializedName("offset") val offset: Int
)

/**
 * Response for document download URL.
 */
data class DownloadUrlResponse(
    @SerializedName("url") val url: String,
    @SerializedName("expirationMinutes") val expirationMinutes: Int
)