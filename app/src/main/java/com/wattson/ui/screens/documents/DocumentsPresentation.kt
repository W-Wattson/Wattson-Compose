package com.wattson.ui.screens.documents

import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentConstraints
import com.wattson.domain.model.OcrStatus
import com.wattson.domain.model.isAllowedMimeType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Aggregated document information derived from the raw backend list.
 */
internal data class DocumentsPresentation(
    val documentsByYear: Map<Int, List<Document>>,
    val availableYears: List<Int>,
    val expandedYears: Set<Int>,
    val totalDocuments: Int,
    val totalDevices: Int,
    val activeWarranties: Int
)

/**
 * Builds view-ready document aggregates while preserving the current screen behavior.
 */
internal object DocumentsPresentationFactory {
    fun create(
        documents: List<Document>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DocumentsPresentation {
        val documentsByYear = groupByYear(documents, zoneId)

        val availableYears = documentsByYear.keys.toList()
        val expandedYears = availableYears.defaultExpandedYears(today.year)
        val totalDevices = documents.mapNotNull(Document::gtin).distinct().size
        val activeWarranties = documents.count { document ->
            document.metadata.warrantyEndDate?.isAfter(today) ?: false
        }

        return DocumentsPresentation(
            documentsByYear = documentsByYear,
            availableYears = availableYears,
            expandedYears = expandedYears,
            totalDocuments = documents.size,
            totalDevices = totalDevices,
            activeWarranties = activeWarranties
        )
    }

    fun filter(
        documents: List<Document>,
        query: String,
        year: Int?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Document> {
        return filterByYear(
            documents = filterByQuery(documents, query),
            year = year,
            zoneId = zoneId
        )
    }

    fun groupByYear(
        documents: List<Document>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<Int, List<Document>> {
        return documents
            .groupBy { document -> document.uploadedAt.atZone(zoneId).year }
            .toSortedMap(reverseOrder())
    }

    fun filterByQuery(documents: List<Document>, query: String): List<Document> {
        if (query.isBlank()) {
            return documents
        }

        return documents.filter { document ->
            document.productName.contains(query, ignoreCase = true) ||
                document.metadata.merchant?.contains(query, ignoreCase = true) == true ||
                document.type.name.contains(query, ignoreCase = true)
        }
    }

    fun filterByYear(
        documents: List<Document>,
        year: Int?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Document> {
        if (year == null) {
            return documents
        }

        return documents.filter { document ->
            document.uploadedAt.atZone(zoneId).year == year
        }
    }

    fun hasRecentPendingOcr(
        documents: List<Document>,
        now: Instant = Instant.now()
    ): Boolean {
        val cutoff = now.minusSeconds(120)
        return documents.any { document ->
            document.uploadedAt.isAfter(cutoff) &&
                (
                    document.ocrStatus?.isInProgress == true ||
                        document.ocrStatus == OcrStatus.UNKNOWN
                    )
        }
    }
}

/**
 * Validates upload inputs and maps backend failures to user-facing messages.
 */
internal object DocumentUploadValidator {
    fun validate(fileName: String, mimeType: String?, fileSize: Long): String? {
        val normalizedMimeType = mimeType?.lowercase()

        if (normalizedMimeType.isNullOrBlank() || !normalizedMimeType.isAllowedMimeType()) {
            return "Format invalide. Formats acceptes : PDF, JPG, PNG."
        }

        if (fileSize <= 0L) {
            return "Impossible de lire ce fichier. Reessaie avec un autre document."
        }

        if (fileSize > DocumentConstraints.MAX_FILE_SIZE_BYTES) {
            return "Fichier trop volumineux (max ${DocumentConstraints.MAX_FILE_SIZE_MB} Mo)."
        }

        if (!fileName.contains('.')) {
            return "Nom de fichier invalide. Formats acceptes : PDF, JPG, PNG."
        }

        return null
    }

    fun mapErrorMessage(raw: String?): String {
        val source = raw.orEmpty()
        val code = Regex("\\b(400|401|402|403|404|413|429)\\b").find(source)?.value

        if (
            source.contains("quota", ignoreCase = true) ||
            source.contains("limit", ignoreCase = true)
        ) {
            return "Limite de documents atteinte."
        }

        return when (code) {
            "400" -> "Format invalide ou document non pris en charge."
            "401" -> "Session expiree, reconnecte-toi."
            "402" -> "Limite de documents atteinte."
            "403" -> "Acces refuse."
            "404" -> "Document introuvable."
            "413" -> "Fichier trop volumineux."
            "429" -> "Limite de documents atteinte."
            else -> "Une erreur est survenue pendant l'envoi."
        }
    }
}

private fun List<Int>.defaultExpandedYears(currentYear: Int): Set<Int> {
    if (isEmpty()) {
        return emptySet()
    }

    return if (contains(currentYear)) {
        setOf(currentYear)
    } else {
        setOf(first())
    }
}
