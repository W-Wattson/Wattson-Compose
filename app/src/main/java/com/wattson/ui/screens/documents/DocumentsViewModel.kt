package com.wattson.ui.screens.documents

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.DocumentRepository
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentConstraints
import com.wattson.domain.model.OcrStatus
import com.wattson.domain.model.getDocumentLimit
import com.wattson.domain.model.isAllowedMimeType
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

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
    val errorMessage: UiText? = null,
    val isUploadInProgress: Boolean = false,
    val uploadProgress: Float = 0f,
    val showDeleteConfirmation: Document? = null
)

sealed interface DocumentsEvent {
    data class NavigateToDocumentDetail(val documentId: String) : DocumentsEvent
    data object ShowUploadPicker : DocumentsEvent
    data class ShowUploadSuccess(val fileName: String) : DocumentsEvent
    data class ShowUploadError(val message: UiText) : DocumentsEvent
    data class ShowDeleteSuccess(val documentName: String) : DocumentsEvent
    data object NavigateToPremium : DocumentsEvent
    data object ShowQuotaExceeded : DocumentsEvent
    data class StartDownload(val url: String, val filename: String) : DocumentsEvent
    data class ShowDownloadError(val message: UiText) : DocumentsEvent
}

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

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DocumentsEvent>()
    val events = _events.asSharedFlow()

    private var postUploadRefreshJob: Job? = null

    init {
        loadDocuments()
    }

    fun onIntent(intent: DocumentsIntent) {
        when (intent) {
            is DocumentsIntent.LoadDocuments -> loadDocuments()
            is DocumentsIntent.RefreshDocuments -> refreshDocuments()
            is DocumentsIntent.SearchDocuments -> searchDocuments(intent.query)
            is DocumentsIntent.FilterByYear -> filterByYear(intent.year)
            is DocumentsIntent.ToggleYearExpanded -> toggleYearExpanded(intent.year)
            is DocumentsIntent.OpenDocument -> openDocument(intent.documentId)
            is DocumentsIntent.StartUpload -> startUpload()
            is DocumentsIntent.UploadDocument -> uploadDocument(
                intent.uri,
                intent.fileName,
                intent.mimeType,
                intent.fileSize
            )

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
                                errorMessage = error.toUiTextOr(
                                    UiText.StringResource(R.string.error_loading_generic)
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_loading_generic)
                        )
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
        val uniqueGtins = documents.mapNotNull { it.gtin }.distinct()
        val activeWarranties = documents.count { doc ->
            doc.metadata.warrantyEndDate?.isAfter(LocalDate.now()) ?: false
        }

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

            state.copy(searchQuery = query, filteredDocuments = filtered)
        }
    }

    private fun filterByYear(year: Int?) {
        _uiState.update { state ->
            val filtered = if (year == null) {
                state.documents
            } else {
                state.documents.filter { doc ->
                    java.time.ZonedDateTime.ofInstant(
                        doc.uploadedAt,
                        java.time.ZoneId.systemDefault()
                    ).year == year
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

                        if (isQuotaExceededMessage(error.message)) {
                            _events.emit(DocumentsEvent.ShowQuotaExceeded)
                        } else {
                            _events.emit(
                                DocumentsEvent.ShowUploadError(mapUploadErrorMessage(error.message))
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadInProgress = false, uploadProgress = 0f) }

                if (isQuotaExceededMessage(e.message)) {
                    _events.emit(DocumentsEvent.ShowQuotaExceeded)
                } else {
                    _events.emit(
                        DocumentsEvent.ShowUploadError(mapUploadErrorMessage(e.message))
                    )
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
                                errorMessage = error.toUiTextOr(
                                    UiText.StringResource(R.string.error_delete_generic)
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUiTextOr(
                            UiText.StringResource(R.string.error_delete_generic)
                        )
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
            val delays = listOf(800L, 1500L, 2500L, 4000L, 6500L, 10000L)
            for (delayMs in delays) {
                delay(delayMs)
                reloadDocumentsSilently()
                if (!hasRecentPendingOcr(_uiState.value.documents)) {
                    break
                }
            }
        }
    }

    private suspend fun reloadDocumentsSilently() {
        try {
            val userId = authRepository.getCurrentUserId()
            val result = documentRepository.getDocuments(userId)
            result.onSuccess { documents -> processDocuments(documents) }
        } catch (_: Exception) {
        }
    }

    private fun hasRecentPendingOcr(documents: List<Document>): Boolean {
        val cutoff = Instant.now().minusSeconds(120)
        return documents.any { doc ->
            doc.uploadedAt.isAfter(cutoff) &&
                (doc.ocrStatus?.isInProgress == true || doc.ocrStatus == OcrStatus.UNKNOWN)
        }
    }

    private fun validateFileBeforeUpload(
        fileName: String,
        mimeType: String?,
        fileSize: Long
    ): UiText? {
        val normalizedMime = mimeType?.lowercase()
        if (normalizedMime.isNullOrBlank() || !normalizedMime.isAllowedMimeType()) {
            return UiText.StringResource(R.string.documents_error_invalid_format)
        }

        if (fileSize <= 0L) {
            return UiText.StringResource(R.string.documents_error_unreadable_file)
        }

        if (fileSize > DocumentConstraints.MAX_FILE_SIZE_BYTES) {
            return UiText.StringResource(
                R.string.documents_error_file_too_large,
                DocumentConstraints.MAX_FILE_SIZE_MB
            )
        }

        if (!fileName.contains('.')) {
            return UiText.StringResource(R.string.documents_error_invalid_filename)
        }

        return null
    }

    private fun isQuotaExceededMessage(raw: String?): Boolean {
        val source = raw.orEmpty()
        return source.contains("quota", ignoreCase = true) ||
            source.contains("limit", ignoreCase = true)
    }

    private fun mapUploadErrorMessage(raw: String?): UiText {
        val source = raw.orEmpty()
        val code = Regex("\\b(400|401|402|403|404|413|429)\\b").find(source)?.value

        if (isQuotaExceededMessage(source)) {
            return UiText.StringResource(R.string.documents_error_quota_exceeded)
        }

        return when (code) {
            "400" -> UiText.StringResource(R.string.documents_error_invalid_or_unsupported)
            "401" -> UiText.StringResource(R.string.documents_error_session_expired)
            "402" -> UiText.StringResource(R.string.documents_error_quota_exceeded)
            "403" -> UiText.StringResource(R.string.documents_error_access_denied)
            "404" -> UiText.StringResource(R.string.documents_error_not_found)
            "413" -> UiText.StringResource(
                R.string.documents_error_file_too_large,
                DocumentConstraints.MAX_FILE_SIZE_MB
            )

            "429" -> UiText.StringResource(R.string.documents_error_quota_exceeded)
            else -> UiText.StringResource(R.string.documents_error_upload_generic)
        }
    }

    private fun downloadDocument(document: Document) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDownloadUrl(userId, document.id)

                result.fold(
                    onSuccess = { downloadInfo ->
                        val filename = appContext.getString(
                            R.string.document_generic_filename,
                            document.id
                        )
                        _events.emit(DocumentsEvent.StartDownload(downloadInfo.url, filename))
                    },
                    onFailure = { error ->
                        _events.emit(
                            DocumentsEvent.ShowDownloadError(
                                error.toUiTextOr(
                                    UiText.StringResource(R.string.error_download_generic)
                                )
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                _events.emit(
                    DocumentsEvent.ShowDownloadError(
                        e.toUiTextOr(UiText.StringResource(R.string.error_download_generic))
                    )
                )
            }
        }
    }
}
