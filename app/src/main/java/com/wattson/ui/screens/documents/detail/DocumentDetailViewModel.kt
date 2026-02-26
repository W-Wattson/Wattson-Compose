package com.wattson.ui.screens.documents.detail

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.data.repository.AuthRepository
import com.wattson.data.repository.DocumentRepository
import com.wattson.domain.model.Document
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI State for the Document Detail screen.
 */
data class DocumentDetailUiState(
    val isLoading: Boolean = false,
    val document: Document? = null,
    val isWarrantyActive: Boolean = false,
    val daysUntilWarrantyExpiry: Int? = null,
    val showDeleteConfirmation: Boolean = false,
    val showShareSheet: Boolean = false,
    val errorMessage: String? = null
)

/**
 * One-shot events for document detail screen.
 */
sealed interface DocumentDetailEvent {
    data object NavigateBack : DocumentDetailEvent
    data class OpenDocument(val url: String) : DocumentDetailEvent
    data class ShareDocument(val url: String) : DocumentDetailEvent
    data class ShowError(val message: String) : DocumentDetailEvent
    data object DocumentDeleted : DocumentDetailEvent
    data object DownloadStarted : DocumentDetailEvent
}

/**
 * User intents for document detail screen.
 */
sealed interface DocumentDetailIntent {
    data object LoadDocument : DocumentDetailIntent
    data object OpenDocument : DocumentDetailIntent
    data object ShareDocument : DocumentDetailIntent
    data object DownloadDocument : DocumentDetailIntent
    data object RequestDelete : DocumentDetailIntent
    data object ConfirmDelete : DocumentDetailIntent
    data object DismissDeleteDialog : DocumentDetailIntent
    data object DismissError : DocumentDetailIntent
    data object NavigateBack : DocumentDetailIntent
}

/**
 * ViewModel for the Document Detail screen.
 * Displays full document information with actions, backed by real API.
 */
@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val documentId: String = checkNotNull(savedStateHandle["documentId"])

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DocumentDetailEvent>()
    val events = _events.asSharedFlow()

    init {
        loadDocument()
    }

    /**
     * Process user intents.
     */
    fun onIntent(intent: DocumentDetailIntent) {
        when (intent) {
            is DocumentDetailIntent.LoadDocument -> loadDocument()
            is DocumentDetailIntent.OpenDocument -> openDocument()
            is DocumentDetailIntent.ShareDocument -> shareDocument()
            is DocumentDetailIntent.DownloadDocument -> downloadDocument()
            is DocumentDetailIntent.RequestDelete -> requestDelete()
            is DocumentDetailIntent.ConfirmDelete -> confirmDelete()
            is DocumentDetailIntent.DismissDeleteDialog -> dismissDeleteDialog()
            is DocumentDetailIntent.DismissError -> dismissError()
            is DocumentDetailIntent.NavigateBack -> navigateBack()
        }
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDocumentById(userId, documentId)

                result.fold(
                    onSuccess = { document ->
                        val warrantyInfo = calculateWarrantyInfo(document)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                document = document,
                                isWarrantyActive = warrantyInfo.first,
                                daysUntilWarrantyExpiry = warrantyInfo.second
                            )
                        }
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

    private fun openDocument() {
        viewModelScope.launch {
            val document = _uiState.value.document ?: return@launch
            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDownloadUrl(userId, document.id)
                result.fold(
                    onSuccess = { downloadInfo ->
                        _events.emit(DocumentDetailEvent.OpenDocument(downloadInfo.url))
                    },
                    onFailure = { error ->
                        _events.emit(DocumentDetailEvent.ShowError(
                            error.message ?: "Impossible d'obtenir l'URL du document"
                        ))
                    }
                )
            } catch (e: Exception) {
                _events.emit(DocumentDetailEvent.ShowError(
                    e.message ?: "Erreur lors de l'ouverture"
                ))
            }
        }
    }

    private fun shareDocument() {
        viewModelScope.launch {
            val document = _uiState.value.document ?: return@launch
            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDownloadUrl(userId, document.id)
                result.fold(
                    onSuccess = { downloadInfo ->
                        _events.emit(DocumentDetailEvent.ShareDocument(downloadInfo.url))
                    },
                    onFailure = { error ->
                        _events.emit(DocumentDetailEvent.ShowError(
                            error.message ?: "Impossible de partager le document"
                        ))
                    }
                )
            } catch (e: Exception) {
                _events.emit(DocumentDetailEvent.ShowError(
                    e.message ?: "Erreur lors du partage"
                ))
            }
        }
    }

    private fun downloadDocument() {
        viewModelScope.launch {
            val document = _uiState.value.document ?: return@launch
            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.getDownloadUrl(userId, document.id)
                result.fold(
                    onSuccess = { downloadInfo ->
                        try {
                            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val uri = Uri.parse(downloadInfo.url)
                            val filename = document.filename ?: "document_${document.id}"

                            val request = DownloadManager.Request(uri).apply {
                                setTitle(filename)
                                setDescription("Wattson — Téléchargement")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                // Allow all network types
                                setAllowedOverMetered(true)
                                setAllowedOverRoaming(true)
                            }

                            downloadManager.enqueue(request)
                            _events.emit(DocumentDetailEvent.DownloadStarted)
                        } catch (e: Exception) {
                            // Fallback: open URL in browser
                            _events.emit(DocumentDetailEvent.OpenDocument(downloadInfo.url))
                        }
                    },
                    onFailure = { error ->
                        _events.emit(DocumentDetailEvent.ShowError(
                            error.message ?: "Impossible de télécharger"
                        ))
                    }
                )
            } catch (e: Exception) {
                _events.emit(DocumentDetailEvent.ShowError(
                    e.message ?: "Erreur lors du téléchargement"
                ))
            }
        }
    }

    private fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    private fun confirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmation = false, isLoading = true) }

            try {
                val userId = authRepository.getCurrentUserId()
                val result = documentRepository.deleteDocument(userId, documentId)

                result.fold(
                    onSuccess = {
                        _events.emit(DocumentDetailEvent.DocumentDeleted)
                        _events.emit(DocumentDetailEvent.NavigateBack)
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

    private fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _events.emit(DocumentDetailEvent.NavigateBack)
        }
    }

    private fun calculateWarrantyInfo(document: Document): Pair<Boolean, Int?> {
        val endDate = document.metadata.warrantyEndDate ?: return Pair(false, null)
        val today = LocalDate.now()

        val isActive = endDate.isAfter(today) || endDate.isEqual(today)
        val daysRemaining = if (isActive) {
            java.time.temporal.ChronoUnit.DAYS.between(today, endDate).toInt()
        } else null

        return Pair(isActive, daysRemaining)
    }
}
