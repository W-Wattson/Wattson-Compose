package com.wattson.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Represents a document stored in the user's Conciergerie, including metadata, OCR data, and soft-delete state.
 */
data class Document(
    val id: String,
    val userId: String,
    val type: DocumentType,
    val productName: String,
    val productCategory: ProductCategory,
    val gtin: String?,
    val fileUrl: String,
    val thumbnailUrl: String?,
    val documentDate: LocalDate,
    val metadata: DocumentMetadata = DocumentMetadata(),
    val ocrData: OcrData? = null,
    val ocrStatus: OcrStatus? = null,
    val uploadedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null, // Soft delete support
    val filename: String? = null,
    val mimeType: String? = null,
    val fileSize: Long = 0L
) {
    val isDeleted: Boolean get() = deletedAt != null

    /**
     * Returns human-readable file extension from filename or mimeType.
     */
    val fileExtension: String
        get() = filename?.substringAfterLast('.', "")?.uppercase()
            ?: mimeType?.let { mimeToExtension(it) }
            ?: "PDF"

    /**
     * Returns human-readable file size string.
     */
    val fileSizeFormatted: String
        get() = when {
            fileSize <= 0 -> ""
            fileSize < 1024 -> "$fileSize o"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} Ko"
            else -> String.format("%.1f Mo", fileSize / (1024.0 * 1024.0))
        }

    private fun mimeToExtension(mime: String): String = when (mime.lowercase()) {
        "application/pdf" -> "PDF"
        "image/jpeg", "image/jpg" -> "JPG"
        "image/png" -> "PNG"
        else -> mime.substringAfterLast('/').uppercase()
    }
}

/**
 * Document types handled by the app.
 */
enum class DocumentType {
    FACTURE,
    TICKET,
    GARANTIE,
    MANUEL,
    OTHER
}

/**
 * Document metadata extracted from the file or user input
 */
data class DocumentMetadata(
    val merchant: String? = null,
    val purchaseDate: LocalDate? = null,
    val totalAmount: Double? = null,
    val currency: String = "EUR",
    val warrantyStartDate: LocalDate? = null,
    val warrantyEndDate: LocalDate? = null,
    val warrantyType: WarrantyType? = null,
    val extensions: List<WarrantyExtension> = emptyList(),
    val reminders: List<DocumentReminder> = emptyList()
)

/**
 * Types of warranty coverage
 */
enum class WarrantyType {
    LEGAL,        // Garantie légale de conformité (2 ans)
    MANUFACTURER, // Garantie constructeur
    EXTENDED,     // Extension de garantie
    COMMERCIAL    // Garantie commerciale
}

/**
 * Warranty extension details
 */
data class WarrantyExtension(
    val provider: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String? = null
)

/**
 * Scheduled reminder for a document
 */
data class DocumentReminder(
    val id: String,
    val type: ReminderType,
    val scheduledDate: LocalDate,
    val isTriggered: Boolean = false
)

/**
 * Types of document reminders
 */
enum class ReminderType {
    WARRANTY_EXPIRING,     // Guarantee expiring soon
    WARRANTY_EXPIRED,      // Guarantee has expired
    MAINTENANCE_DUE,       // Scheduled maintenance
    CUSTOM                 // User-defined reminder
}

/**
 * OCR data extracted from receipt or invoice images.
 */


enum class OcrStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    PARTIAL,
    FAILED,
    UNKNOWN;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == PARTIAL || this == FAILED

    val isInProgress: Boolean
        get() = this == PENDING || this == PROCESSING
}

data class OcrData(
    val rawText: String,
    val merchant: String?,
    val totalAmount: Double?,
    val lines: List<OcrLine> = emptyList(),
    val confidence: Double // 0-1 confidence score
)

/**
 * Individual line item extracted from OCR
 */
data class OcrLine(
    val gtin: String?,
    val label: String,
    val quantity: Int,
    val unitPrice: Double?
)

/**
 * Document grouped by year for display
 */
data class DocumentsByYear(
    val year: Int,
    val documents: List<Document>,
    val count: Int = documents.size
)

/**
 * File type constraints enforced during document uploads.
 */
object DocumentConstraints {
    val ALLOWED_MIME_TYPES = listOf(
        "application/pdf",
        "image/jpeg",
        "image/jpg", 
        "image/png"
    )
    
    val ALLOWED_EXTENSIONS = listOf("pdf", "jpg", "jpeg", "png")
    
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
    const val MAX_FILE_SIZE_MB = 10
}

/**
 * Checks if a file extension is allowed
 */
fun String.isAllowedDocumentExtension(): Boolean {
    return this.lowercase() in DocumentConstraints.ALLOWED_EXTENSIONS
}

/**
 * Checks if a MIME type is allowed
 */
fun String.isAllowedMimeType(): Boolean {
    return this.lowercase() in DocumentConstraints.ALLOWED_MIME_TYPES
}
