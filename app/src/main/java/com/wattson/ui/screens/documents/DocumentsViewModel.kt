package com.wattson.ui.screens.documents

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.R
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.DocumentRepository
import com.wattson.domain.model.Document
import com.wattson.domain.model.getDocumentLimit
import com.wattson.ui.i18n.UiText
import com.wattson.ui.i18n.toUiTextOr
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentsUiState(
    val isLoading: Boolean = false,
    val documents: List<Document> = emptyList(),
    val documentsByYear: Map<Int, List<Document>> = emptyMap(),
    val filteredDocuments: List<Document> = emptyList(),
    val filteredDocumentsByYear: Map<Int, List<Document>> = emptyMap(),
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
            DocumentsIntent.LoadDocuments -> loadDocuments()
            DocumentsIntent.RefreshDocuments -> refreshDocuments()
            is DocumentsIntent.SearchDocuments -> searchDocuments(intent.query)
            is DocumentsIntent.FilterByYear -> filterByYear(intent.year)
            is DocumentsIntent.ToggleYearExpanded -> toggleYearExpanded(intent.year)
            is DocumentsIntent.OpenDocument -> openDocument(intent.documentId)
            DocumentsIntent.StartUpload -> startUpload()
            is DocumentsIntent.UploadDocument -> uploadDocument(
                uri = intent.uri,
                fileName = intent.fileName,
                mimeType = intent.mimeType,
                fileSize = intent.fileSize
            )

            is DocumentsIntent.DeleteDocument -> requestDeleteDocument(intent.document)
            DocumentsIntent.ConfirmDelete -> confirmDelete()
            DocumentsIntent.CancelDelete -> cancelDelete()
            DocumentsIntent.DismissError -> dismissError()
            DocumentsIntent.NavigateToPremium -> navigateToPremium()
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
                    onSuccess = ::processDocuments,
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
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.toUiTextOr(
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
        val currentState = _uiState.value
        val presentation = DocumentsPresentationFactory.create(documents)
        val filteredDocuments = DocumentsPresentationFactory.filter(
            documents = documents,
            query = currentState.searchQuery,
            year = currentState.selectedYear
        )
        val filteredDocumentsByYear = DocumentsPresentationFactory.groupByYear(filteredDocuments)

        _uiState.update {
            it.copy(
                isLoading = false,
                documents = documents,
                documentsByYear = presentation.documentsByYear,
                filteredDocuments = filteredDocuments,
                filteredDocumentsByYear = filteredDocumentsByYear,
                availableYears = presentation.availableYears,
                expandedYears = resolveExpandedYears(
                    selectedYear = currentState.selectedYear,
                    currentExpandedYears = currentState.expandedYears,
                    defaultExpandedYears = presentation.expandedYears,
                    availableYears = filteredDocumentsByYear.keys
                ),
                totalDocuments = presentation.totalDocuments,
                totalDevices = presentation.totalDevices,
                activeWarranties = presentation.activeWarranties,
                isUploadInProgress = false,
                uploadProgress = 0f
            )
        }
    }

    private fun searchDocuments(query: String) {
        _uiState.update { state ->
            val filteredDocuments = DocumentsPresentationFactory.filter(
                documents = state.documents,
                query = query,
                year = state.selectedYear
            )
            val filteredDocumentsByYear = DocumentsPresentationFactory.groupByYear(filteredDocuments)

            state.copy(
                searchQuery = query,
                filteredDocuments = filteredDocuments,
                filteredDocumentsByYear = filteredDocumentsByYear,
                expandedYears = resolveExpandedYears(
                    selectedYear = state.selectedYear,
                    currentExpandedYears = state.expandedYears,
                    defaultExpandedYears = defaultExpandedYears(filteredDocumentsByYear.keys),
                    availableYears = filteredDocumentsByYear.keys
                )
            )
        }
    }

    private fun filterByYear(year: Int?) {
        _uiState.update { state ->
            val filteredDocuments = DocumentsPresentationFactory.filter(
                documents = state.documents,
                query = state.searchQuery,
                year = year
            )
            val filteredDocumentsByYear = DocumentsPresentationFactory.groupByYear(filteredDocuments)

            state.copy(
                selectedYear = year,
                filteredDocuments = filteredDocuments,
                filteredDocumentsByYear = filteredDocumentsByYear,
                expandedYears = resolveExpandedYears(
                    selectedYear = year,
                    currentExpandedYears = state.expandedYears,
                    defaultExpandedYears = defaultExpandedYears(filteredDocumentsByYear.keys),
                    availableYears = filteredDocumentsByYear.keys
                )
            )
        }
    }

    private fun toggleYearExpanded(year: Int) {
        _uiState.update { state ->
            val expandedYears = if (state.expandedYears.contains(year)) {
                state.expandedYears - year
            } else {
                state.expandedYears + year
            }

            state.copy(expandedYears = expandedYears)
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

    private fun uploadDocument(
        uri: String,
        fileName: String,
        mimeType: String?,
        fileSize: Long
    ) {
        val validationError = DocumentUploadValidator.validate(fileName, mimeType, fileSize)
        if (validationError != null) {
            viewModelScope.launch {
                _events.emit(
                    DocumentsEvent.ShowUploadError(UiText.DynamicString(validationError))
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isUploadInProgress = true, uploadProgress = 0.1f)
            }

            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.uploadDocument(
                    userId = userId,
                    uri = Uri.parse(uri),
                    documentType = "OTHER"
                )

                result.fold(
                    onSuccess = {
                        _events.emit(DocumentsEvent.ShowUploadSuccess(fileName))
                        loadDocuments()
                        startPostUploadRefreshWindow()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(isUploadInProgress = false, uploadProgress = 0f)
                        }
                        emitUploadFailure(DocumentUploadValidator.mapErrorMessage(error.message))
                    }
                )
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(isUploadInProgress = false, uploadProgress = 0f)
                }
                emitUploadFailure(DocumentUploadValidator.mapErrorMessage(exception.message))
            }
        }
    }

    private suspend fun emitUploadFailure(message: String) {
        if (message.contains("Limite de documents", ignoreCase = true)) {
            _events.emit(DocumentsEvent.ShowQuotaExceeded)
        } else {
            _events.emit(DocumentsEvent.ShowUploadError(UiText.DynamicString(message)))
        }
    }

    private fun requestDeleteDocument(document: Document) {
        _uiState.update { it.copy(showDeleteConfirmation = document) }
    }

    private fun confirmDelete() {
        val document = _uiState.value.showDeleteConfirmation ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(showDeleteConfirmation = null, isLoading = true)
            }

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
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.toUiTextOr(
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
            val pollingDelays = listOf(800L, 1500L, 2500L, 4000L, 6500L, 10000L)

            for (delayMs in pollingDelays) {
                delay(delayMs)
                reloadDocumentsSilently()

                if (!DocumentsPresentationFactory.hasRecentPendingOcr(_uiState.value.documents)) {
                    break
                }
            }
        }
    }

    /**
     * Polling after an upload should not surface transient backend failures to the user.
     */
    private suspend fun reloadDocumentsSilently() {
        try {
            val userId = authRepository.getCurrentUserId()
            val result = documentRepository.getDocuments(userId)
            result.onSuccess(::processDocuments)
        } catch (_: Exception) {
            // Silent by design.
        }
    }

    private fun downloadDocument(document: Document) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDownloadUrl(userId, document.id)

                result.fold(
                    onSuccess = { downloadInfo ->
                        val fileName = appContext.getString(
                            R.string.document_generic_filename,
                            document.id
                        )
                        _events.emit(
                            DocumentsEvent.StartDownload(
                                url = downloadInfo.url,
                                filename = fileName
                            )
                        )
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
            } catch (exception: Exception) {
                _events.emit(
                    DocumentsEvent.ShowDownloadError(
                        exception.toUiTextOr(
                            UiText.StringResource(R.string.error_download_generic)
                        )
                    )
                )
            }
        }
    }

    private fun resolveExpandedYears(
        selectedYear: Int?,
        currentExpandedYears: Set<Int>,
        defaultExpandedYears: Set<Int>,
        availableYears: Set<Int>
    ): Set<Int> {
        if (selectedYear != null) {
            return selectedYear.takeIf(availableYears::contains)?.let(::setOf) ?: emptySet()
        }

        val preservedExpandedYears = currentExpandedYears.intersect(availableYears)
        if (preservedExpandedYears.isNotEmpty()) {
            return preservedExpandedYears
        }

        val defaultVisibleYears = defaultExpandedYears.intersect(availableYears)
        if (defaultVisibleYears.isNotEmpty()) {
            return defaultVisibleYears
        }

        return availableYears.firstOrNull()?.let(::setOf) ?: emptySet()
    }

    private fun defaultExpandedYears(availableYears: Set<Int>): Set<Int> {
        if (availableYears.isEmpty()) {
            return emptySet()
        }

        val currentYear = java.time.LocalDate.now().year
        return if (availableYears.contains(currentYear)) {
            setOf(currentYear)
        } else {
            availableYears.maxOrNull()?.let(::setOf) ?: emptySet()
        }
    }
}
