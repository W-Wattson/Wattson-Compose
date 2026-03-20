package com.wattson.ui.screens.documents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.DocumentRepository
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentConstraints
import com.wattson.domain.model.OcrStatus
import com.wattson.domain.model.getDocumentLimit
import com.wattson.domain.model.isAllowedMimeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * UI State for the Documents screen (Conciergerie).
 */
data class DocumentsUiState(
    val isLoading: Boolean = false,
    val documents: List<Document> = emptyList(),
    val documentsByYear: Map<Int, List<Document>> = emptyMap(),
    val filteredDocuments: List<Document> = emptyList(),
    val searchQuery: String = "",
    val selectedYear: Int? = null,
    val availableYears: List<Int> = emptyList(),
    val expandedYears: Set<Int> = emptySet(),
    val totalDocuments: Int = 0,
    val totalDevices: Int = 0,
    val activeWarranties: Int = 0,
    val errorMessage: String? = null,
    val isUploadInProgress: Boolean = false,
    val uploadProgress: Float = 0f,
    val showDeleteConfirmation: Document? = null
)

/**
 * One-shot events for documents screen.
 */
sealed interface DocumentsEvent {
    data class NavigateToDocumentDetail(val documentId: String) : DocumentsEvent
    data object ShowUploadPicker : DocumentsEvent
    data class ShowUploadSuccess(val fileName: String) : DocumentsEvent
    data class ShowUploadError(val message: String) : DocumentsEvent
    data class ShowDeleteSuccess(val documentName: String) : DocumentsEvent
    data object NavigateToPremium : DocumentsEvent
    data object ShowQuotaExceeded : DocumentsEvent
    data class StartDownload(val url: String, val filename: String) : DocumentsEvent
    data class ShowDownloadError(val message: String) : DocumentsEvent
}

/**
 * User intents for documents screen.
 */
sealed interface DocumentsIntent {
    data object LoadDocuments : DocumentsIntent
    data object RefreshDocuments : DocumentsIntent
    data class SearchDocuments(val query: String) : DocumentsIntent
    data class FilterByYear(val year: Int?) : DocumentsIntent
    data class ToggleYearExpanded(val year: Int) : DocumentsIntent
    data class OpenDocument(val documentId: String) : DocumentsIntent
    data object StartUpload : DocumentsIntent
    data class UploadDocument(
        val uri: String,
        val fileName: String,
        val mimeType: String?,
        val fileSize: Long
    ) : DocumentsIntent
    data class DeleteDocument(val document: Document) : DocumentsIntent
    data object ConfirmDelete : DocumentsIntent
    data object CancelDelete : DocumentsIntent
    data object DismissError : DocumentsIntent
    data object NavigateToPremium : DocumentsIntent
    data class DownloadDocument(val document: Document) : DocumentsIntent
}

/**
 * ViewModel for the Documents screen (Conciergerie).
 * Manages document listing, upload, and deletion via backend API.
 */
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DocumentsEvent>()
    val events = _events.asSharedFlow()

    private var postUploadRefreshJob: Job? = null

    init {
        loadDocuments()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: DocumentsIntent) {
        when (intent) {
            is DocumentsIntent.LoadDocuments -> loadDocuments()
            is DocumentsIntent.RefreshDocuments -> refreshDocuments()
            is DocumentsIntent.SearchDocuments -> searchDocuments(intent.query)
            is DocumentsIntent.FilterByYear -> filterByYear(intent.year)
            is DocumentsIntent.ToggleYearExpanded -> toggleYearExpanded(intent.year)
            is DocumentsIntent.OpenDocument -> openDocument(intent.documentId)
            is DocumentsIntent.StartUpload -> startUpload()
            is DocumentsIntent.UploadDocument -> uploadDocument(intent.uri, intent.fileName, intent.mimeType, intent.fileSize)
            is DocumentsIntent.DeleteDocument -> requestDeleteDocument(intent.document)
            is DocumentsIntent.ConfirmDelete -> confirmDelete()
            is DocumentsIntent.CancelDelete -> cancelDelete()
            is DocumentsIntent.DismissError -> dismissError()
            is DocumentsIntent.NavigateToPremium -> navigateToPremium()
            is DocumentsIntent.DownloadDocument -> downloadDocument(intent.document)
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDocuments(userId)

                result.fold(
                    onSuccess = { documents ->
                        processDocuments(documents)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Erreur lors du chargement"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur lors du chargement"
                    )
                }
            }
        }
    }

    private fun refreshDocuments() {
        loadDocuments()
    }

    private fun processDocuments(documents: List<Document>) {
        val documentsByYear = documents.groupBy {
            java.time.ZonedDateTime.ofInstant(it.uploadedAt, java.time.ZoneId.systemDefault()).year
        }.toSortedMap(reverseOrder())

        val availableYears = documentsByYear.keys.toList()

        // Calculate stats
        val uniqueGtins = documents.mapNotNull { it.gtin }.distinct()
        val activeWarranties = documents.count { doc ->
            doc.metadata.warrantyEndDate?.isAfter(LocalDate.now()) ?: false
        }

        // Auto-expand current year
        val currentYear = LocalDate.now().year
        val expandedYears = if (availableYears.contains(currentYear)) {
            setOf(currentYear)
        } else {
            availableYears.firstOrNull()?.let { setOf(it) } ?: emptySet()
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                documents = documents,
                documentsByYear = documentsByYear,
                filteredDocuments = documents,
                availableYears = availableYears,
                expandedYears = expandedYears,
                totalDocuments = documents.size,
                totalDevices = uniqueGtins.size,
                activeWarranties = activeWarranties
            )
        }
    }

    private fun searchDocuments(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.documents
            } else {
                state.documents.filter { doc ->
                    doc.productName.contains(query, ignoreCase = true) ||
                    doc.metadata.merchant?.contains(query, ignoreCase = true) == true ||
                    doc.type.name.contains(query, ignoreCase = true)
                }
            }

            state.copy(
                searchQuery = query,
                filteredDocuments = filtered
            )
        }
    }

    private fun filterByYear(year: Int?) {
        _uiState.update { state ->
            val filtered = if (year == null) {
                state.documents
            } else {
                state.documents.filter { doc ->
                    java.time.ZonedDateTime.ofInstant(doc.uploadedAt, java.time.ZoneId.systemDefault()).year == year
                }
            }

            state.copy(
                selectedYear = year,
                filteredDocuments = filtered,
                expandedYears = if (year != null) setOf(year) else state.expandedYears
            )
        }
    }

    private fun toggleYearExpanded(year: Int) {
        _uiState.update { state ->
            val newExpanded = if (state.expandedYears.contains(year)) {
                state.expandedYears - year
            } else {
                state.expandedYears + year
            }
            state.copy(expandedYears = newExpanded)
        }
    }

    private fun openDocument(documentId: String) {
        viewModelScope.launch {
            _events.emit(DocumentsEvent.NavigateToDocumentDetail(documentId))
        }
    }

    private fun startUpload() {
        val currentUser = authRepository.currentUser.value
        val documentLimit = currentUser?.subscriptionType?.getDocumentLimit()

        if (documentLimit != null && _uiState.value.totalDocuments >= documentLimit) {
            viewModelScope.launch {
                _events.emit(DocumentsEvent.ShowQuotaExceeded)
            }
            return
        }

        viewModelScope.launch {
            _events.emit(DocumentsEvent.ShowUploadPicker)
        }
    }

    private fun uploadDocument(uri: String, fileName: String, mimeType: String?, fileSize: Long) {
        val validationError = validateFileBeforeUpload(fileName, mimeType, fileSize)
        if (validationError != null) {
            viewModelScope.launch { _events.emit(DocumentsEvent.ShowUploadError(validationError)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadInProgress = true, uploadProgress = 0.1f) }

            try {
                val userId = authRepository.getCurrentUserId()
                val androidUri = Uri.parse(uri)

                val result = documentRepository.uploadDocument(
                    userId = userId,
                    uri = androidUri,
                    documentType = "OTHER"
                )

                result.fold(
                    onSuccess = {
                        _events.emit(DocumentsEvent.ShowUploadSuccess(fileName))
                        loadDocuments()
                        startPostUploadRefreshWindow()
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isUploadInProgress = false, uploadProgress = 0f) }

                        val message = mapUploadErrorMessage(error.message)
                        if (message.contains("Limite de documents", ignoreCase = true)) {
                            viewModelScope.launch { _events.emit(DocumentsEvent.ShowQuotaExceeded) }
                        } else {
                            viewModelScope.launch { _events.emit(DocumentsEvent.ShowUploadError(message)) }
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadInProgress = false, uploadProgress = 0f) }

                val message = mapUploadErrorMessage(e.message)
                if (message.contains("Limite de documents", ignoreCase = true)) {
                    _events.emit(DocumentsEvent.ShowQuotaExceeded)
                } else {
                    _events.emit(DocumentsEvent.ShowUploadError(message))
                }
            }
        }
    }

    private fun requestDeleteDocument(document: Document) {
        _uiState.update { it.copy(showDeleteConfirmation = document) }
    }

    private fun confirmDelete() {
        val document = _uiState.value.showDeleteConfirmation ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmation = null, isLoading = true) }

            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.deleteDocument(userId, document.id)

                result.fold(
                    onSuccess = {
                        _events.emit(DocumentsEvent.ShowDeleteSuccess(document.productName))
                        loadDocuments()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Erreur lors de la suppression"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur lors de la suppression"
                    )
                }
            }
        }
    }

    private fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun navigateToPremium() {
        viewModelScope.launch {
            _events.emit(DocumentsEvent.NavigateToPremium)
        }
    }

    private fun startPostUploadRefreshWindow() {
        postUploadRefreshJob?.cancel()
        postUploadRefreshJob = viewModelScope.launch {
            // Refresh plusieurs fois pendant ~20s max
            val delays = listOf(800L, 1500L, 2500L, 4000L, 6500L, 10000L)
            for (d in delays) {
                delay(d)
                reloadDocumentsSilently()

                // Stop si aucun document récent n'est encore en OCR pending/processing
                if (!hasRecentPendingOcr(_uiState.value.documents)) break
            }
        }
    }

    private suspend fun reloadDocumentsSilently() {
        try {
            val userId = authRepository.getCurrentUserId()
            val result = documentRepository.getDocuments(userId)

            result.onSuccess { documents ->
                processDocuments(documents)
            }
            // En cas d'erreur, on ne spam pas l'UI (silencieux)
        } catch (_: Exception) {
            // silencieux
        }
    }

    private fun hasRecentPendingOcr(documents: List<Document>): Boolean {
        val cutoff = Instant.now().minusSeconds(120) // 2 minutes
        return documents.any { doc ->
            doc.uploadedAt.isAfter(cutoff) && (doc.ocrStatus?.isInProgress == true || doc.ocrStatus == OcrStatus.UNKNOWN)
        }
    }

    private fun validateFileBeforeUpload(fileName: String, mimeType: String?, fileSize: Long): String? {
        val normalizedMime = mimeType?.lowercase()
        if (normalizedMime.isNullOrBlank() || !normalizedMime.isAllowedMimeType()) {
            return "Format invalide. Formats acceptés : PDF, JPG, PNG."
        }

        if (fileSize <= 0L) {
            return "Impossible de lire ce fichier. Réessaie avec un autre document."
        }

        if (fileSize > DocumentConstraints.MAX_FILE_SIZE_BYTES) {
            return "Fichier trop volumineux (max ${DocumentConstraints.MAX_FILE_SIZE_MB} Mo)."
        }

        if (!fileName.contains('.')) {
            return "Nom de fichier invalide. Formats acceptés : PDF, JPG, PNG."
        }

        return null
    }

    private fun mapUploadErrorMessage(raw: String?): String {
        val source = raw.orEmpty()
        val code = Regex("\\b(400|401|402|403|404|413|429)\\b").find(source)?.value

        if (source.contains("quota", ignoreCase = true) || source.contains("limit", ignoreCase = true)) {
            return "Limite de documents atteinte."
        }

        return when (code) {
            "400" -> "Format invalide ou document non pris en charge."
            "401" -> "Session expirée, reconnecte-toi."
            "402" -> "Limite de documents atteinte."
            "403" -> "Accès refusé."
            "404" -> "Document introuvable."
            "413" -> "Fichier trop volumineux."
            "429" -> "Limite de documents atteinte."
            else -> "Une erreur est survenue pendant l'envoi."
        }
    }

    private fun downloadDocument(document: Document) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDownloadUrl(userId, document.id)

                result.fold(
                    onSuccess = { downloadInfo ->
                        val filename = "document_${document.id}"
                        _events.emit(DocumentsEvent.StartDownload(downloadInfo.url, filename))
                    },
                    onFailure = { error ->
                        _events.emit(DocumentsEvent.ShowDownloadError(error.message ?: "Erreur download"))
                    }
                )
            } catch (e: Exception) {
                _events.emit(DocumentsEvent.ShowDownloadError(e.message ?: "Erreur download"))
            }
        }
    }
}
