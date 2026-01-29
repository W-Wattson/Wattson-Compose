package com.wattson.ui.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wattson.domain.model.Document
import com.wattson.domain.model.DocumentMetadata
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.util.UUID
import javax.inject.Inject

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
    data class UploadDocument(val uri: String, val fileName: String) : DocumentsIntent
    data class DeleteDocument(val document: Document) : DocumentsIntent
    data object ConfirmDelete : DocumentsIntent
    data object CancelDelete : DocumentsIntent
    data object DismissError : DocumentsIntent
    data object NavigateToPremium : DocumentsIntent
}

/**
 * ViewModel for the Documents screen (Conciergerie).
 * Manages document listing, upload, and deletion.
 */
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    // TODO: Inject use cases when implemented
    // private val getDocumentsUseCase: GetDocumentsUseCase,
    // private val uploadDocumentUseCase: UploadDocumentUseCase,
    // private val deleteDocumentUseCase: DeleteDocumentUseCase,
    // private val getUserQuotaUseCase: GetUserQuotaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DocumentsEvent>()
    val events = _events.asSharedFlow()

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
            is DocumentsIntent.UploadDocument -> uploadDocument(intent.uri, intent.fileName)
            is DocumentsIntent.DeleteDocument -> requestDeleteDocument(intent.document)
            is DocumentsIntent.ConfirmDelete -> confirmDelete()
            is DocumentsIntent.CancelDelete -> cancelDelete()
            is DocumentsIntent.DismissError -> dismissError()
            is DocumentsIntent.NavigateToPremium -> navigateToPremium()
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // TODO: Replace with actual use case
                // val documents = getDocumentsUseCase()
                
                // Mock data for development
                val documents = getMockDocuments()
                
                processDocuments(documents)
                
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
        viewModelScope.launch {
            // TODO: Check quota with actual use case
            // val quota = getUserQuotaUseCase()
            // if (quota.remaining <= 0) {
            //     _events.emit(DocumentsEvent.ShowQuotaExceeded)
            //     return@launch
            // }
            
            _events.emit(DocumentsEvent.ShowUploadPicker)
        }
    }

    private fun uploadDocument(uri: String, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadInProgress = true, uploadProgress = 0f) }

            try {
                // TODO: Replace with actual upload
                // Simulate upload progress
                for (i in 1..10) {
                    kotlinx.coroutines.delay(200)
                    _uiState.update { it.copy(uploadProgress = i / 10f) }
                }

                _uiState.update { it.copy(isUploadInProgress = false, uploadProgress = 0f) }
                _events.emit(DocumentsEvent.ShowUploadSuccess(fileName))
                
                // Refresh documents list
                loadDocuments()
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadInProgress = false, uploadProgress = 0f) }
                _events.emit(DocumentsEvent.ShowUploadError(e.message ?: "Erreur d'upload"))
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
                // TODO: Replace with actual delete
                kotlinx.coroutines.delay(500)
                
                _events.emit(DocumentsEvent.ShowDeleteSuccess(document.productName))
                loadDocuments()
                
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

    // Mock data for development
    private fun getMockDocuments(): List<Document> = listOf(
        Document(
            id = "doc_1",
            userId = "user_1",
            type = DocumentType.FACTURE,
            productName = "iPhone 15 Pro",
            productCategory = ProductCategory.ELECTRONIQUE,
            gtin = "3760000000001",
            fileUrl = "https://example.com/doc1.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2025, 1, 15),
            metadata = DocumentMetadata(
                merchant = "Apple Store",
                purchaseDate = LocalDate.of(2025, 1, 15)
            ),
            uploadedAt = Instant.parse("2025-01-15T10:00:00Z")
        ),
        Document(
            id = "doc_2",
            userId = "user_1",
            type = DocumentType.GARANTIE,
            productName = "MacBook Air M3",
            productCategory = ProductCategory.INFORMATIQUE,
            gtin = "3760000000002",
            fileUrl = "https://example.com/doc2.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2024, 11, 22),
            metadata = DocumentMetadata(
                merchant = "Fnac",
                purchaseDate = LocalDate.of(2024, 11, 22),
                warrantyEndDate = LocalDate.of(2026, 11, 22)
            ),
            uploadedAt = Instant.parse("2024-11-22T14:30:00Z")
        ),
        Document(
            id = "doc_3",
            userId = "user_1",
            type = DocumentType.FACTURE,
            productName = "Dyson V15",
            productCategory = ProductCategory.ELECTROMENAGER,
            gtin = "3760000000003",
            fileUrl = "https://example.com/doc3.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2024, 10, 8),
            metadata = DocumentMetadata(
                merchant = "Darty",
                purchaseDate = LocalDate.of(2024, 10, 8)
            ),
            uploadedAt = Instant.parse("2024-10-08T09:15:00Z")
        ),
        Document(
            id = "doc_4",
            userId = "user_1",
            type = DocumentType.GARANTIE,
            productName = "Samsung TV 65\"",
            productCategory = ProductCategory.AUDIO_VIDEO,
            gtin = "3760000000004",
            fileUrl = "https://example.com/doc4.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2024, 9, 14),
            metadata = DocumentMetadata(
                merchant = "Boulanger",
                purchaseDate = LocalDate.of(2024, 9, 14),
                warrantyEndDate = LocalDate.of(2026, 9, 14)
            ),
            uploadedAt = Instant.parse("2024-09-14T16:45:00Z")
        ),
        Document(
            id = "doc_5",
            userId = "user_1",
            type = DocumentType.FACTURE,
            productName = "Thermomix TM6",
            productCategory = ProductCategory.ELECTROMENAGER,
            gtin = "3760000000005",
            fileUrl = "https://example.com/doc5.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2024, 7, 3),
            metadata = DocumentMetadata(
                merchant = "Vorwerk",
                purchaseDate = LocalDate.of(2024, 7, 3)
            ),
            uploadedAt = Instant.parse("2024-07-03T11:20:00Z")
        ),
        Document(
            id = "doc_6",
            userId = "user_1",
            type = DocumentType.GARANTIE,
            productName = "PlayStation 5",
            productCategory = ProductCategory.GAMING,
            gtin = "3760000000006",
            fileUrl = "https://example.com/doc6.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2024, 5, 25),
            metadata = DocumentMetadata(
                merchant = "Micromania",
                purchaseDate = LocalDate.of(2024, 5, 25),
                warrantyEndDate = LocalDate.of(2026, 5, 25)
            ),
            uploadedAt = Instant.parse("2024-05-25T13:00:00Z")
        ),
        Document(
            id = "doc_7",
            userId = "user_1",
            type = DocumentType.FACTURE,
            productName = "Lave-linge Miele",
            productCategory = ProductCategory.ELECTROMENAGER,
            gtin = "3760000000007",
            fileUrl = "https://example.com/doc7.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2023, 12, 12),
            metadata = DocumentMetadata(
                merchant = "Darty",
                purchaseDate = LocalDate.of(2023, 12, 12)
            ),
            uploadedAt = Instant.parse("2023-12-12T10:30:00Z")
        ),
        Document(
            id = "doc_8",
            userId = "user_1",
            type = DocumentType.GARANTIE,
            productName = "iPad Pro 12.9",
            productCategory = ProductCategory.ELECTRONIQUE,
            gtin = "3760000000008",
            fileUrl = "https://example.com/doc8.pdf",
            thumbnailUrl = null,
            documentDate = LocalDate.of(2023, 10, 28),
            metadata = DocumentMetadata(
                merchant = "Apple Store",
                purchaseDate = LocalDate.of(2023, 10, 28),
                warrantyEndDate = LocalDate.of(2025, 10, 28)
            ),
            uploadedAt = Instant.parse("2023-10-28T15:45:00Z")
        )
    )
}
