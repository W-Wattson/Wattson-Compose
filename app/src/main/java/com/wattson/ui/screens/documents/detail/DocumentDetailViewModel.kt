package com.wattson.ui.screens.documents.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentMetadata
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.WarrantyType
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
 * Displays full document information with actions.
 */
@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject use cases when implemented
    // private val getDocumentUseCase: GetDocumentUseCase,
    // private val deleteDocumentUseCase: DeleteDocumentUseCase
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
                // TODO: Replace with actual use case
                kotlinx.coroutines.delay(300)
                
                val document = getMockDocument()
                val warrantyInfo = calculateWarrantyInfo(document)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        document = document,
                        isWarrantyActive = warrantyInfo.first,
                        daysUntilWarrantyExpiry = warrantyInfo.second
                    )
                }

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
            _events.emit(DocumentDetailEvent.OpenDocument(document.fileUrl))
        }
    }

    private fun shareDocument() {
        viewModelScope.launch {
            val document = _uiState.value.document ?: return@launch
            _events.emit(DocumentDetailEvent.ShareDocument(document.fileUrl))
        }
    }

    private fun downloadDocument() {
        viewModelScope.launch {
            // TODO: Implement actual download
            _events.emit(DocumentDetailEvent.DownloadStarted)
        }
    }

    private fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    private fun confirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmation = false, isLoading = true) }

            try {
                // TODO: Replace with actual use case
                kotlinx.coroutines.delay(500)
                
                _events.emit(DocumentDetailEvent.DocumentDeleted)
                _events.emit(DocumentDetailEvent.NavigateBack)

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

    // Mock data for development
    private fun getMockDocument(): Document {
        return Document(
            id = documentId,
            userId = "user_123",
            type = DocumentType.GARANTIE,
            productName = "iPhone 15 Pro",
            productCategory = ProductCategory.ELECTRONIQUE,
            gtin = "0194253401148",
            fileUrl = "https://storage.wattson.app/documents/doc_${documentId}.pdf",
            thumbnailUrl = "https://storage.wattson.app/thumbnails/doc_${documentId}_thumb.jpg",
            documentDate = LocalDate.of(2025, 1, 15),
            metadata = DocumentMetadata(
                merchant = "Apple Store Lyon Part-Dieu",
                purchaseDate = LocalDate.of(2025, 1, 15),
                totalAmount = 1229.0,
                currency = "EUR",
                warrantyStartDate = LocalDate.of(2025, 1, 15),
                warrantyEndDate = LocalDate.of(2027, 1, 15),
                warrantyType = WarrantyType.MANUFACTURER
            )
        )
    }
}
