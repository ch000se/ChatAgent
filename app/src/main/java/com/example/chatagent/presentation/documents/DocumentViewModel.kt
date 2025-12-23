package com.example.chatagent.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.repository.DocumentRepository
import com.example.chatagent.domain.usecase.IndexDocumentUseCase
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val indexDocumentUseCase: IndexDocumentUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
        loadDocumentCounts()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { documents ->
                _uiState.update { it.copy(documents = documents) }
            }
        }
    }

    private fun loadDocumentCounts() {
        viewModelScope.launch {
            documentRepository.getDocumentsCount().fold(
                onSuccess = { count ->
                    _uiState.update { it.copy(totalDocuments = count) }
                },
                onFailure = { /* ignore */ }
            )

            documentRepository.getIndexedDocumentsCount().fold(
                onSuccess = { count ->
                    _uiState.update { it.copy(indexedDocuments = count) }
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun onDocumentNameChanged(name: String) {
        _uiState.update { it.copy(documentName = name) }
    }

    fun onDocumentContentChanged(content: String) {
        _uiState.update { it.copy(documentContent = content) }
    }

    fun onDocumentTypeChanged(type: String) {
        _uiState.update { it.copy(documentType = type) }
    }

    fun showAddDocumentDialog() {
        _uiState.update {
            it.copy(
                showAddDocumentDialog = true,
                documentName = "",
                documentContent = "",
                documentType = "text"
            )
        }
    }

    fun hideAddDocumentDialog() {
        _uiState.update { it.copy(showAddDocumentDialog = false) }
    }

    fun addDocument() {
        val name = _uiState.value.documentName.trim()
        val content = _uiState.value.documentContent.trim()
        val type = _uiState.value.documentType

        if (name.isEmpty() || content.isEmpty()) {
            _uiState.update { it.copy(error = "Name and content cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            documentRepository.addDocument(name, content, type).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showAddDocumentDialog = false,
                            error = null
                        )
                    }
                    loadDocumentCounts()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to add document"
                        )
                    }
                }
            )
        }
    }

    fun indexDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isIndexing = true, indexingProgress = null, error = null) }

            indexDocumentUseCase(documentId).collect { progress ->
                _uiState.update { it.copy(indexingProgress = progress) }

                if (progress.currentStatus == "Indexing completed") {
                    _uiState.update { it.copy(isIndexing = false) }
                    loadDocumentCounts()
                } else if (progress.currentStatus.startsWith("Error:")) {
                    _uiState.update {
                        it.copy(
                            isIndexing = false,
                            error = progress.currentStatus
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchDocuments() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) {
            _uiState.update { it.copy(error = "Search query cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            searchDocumentsUseCase(query, topK = 5).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isSearching = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            error = error.message ?: "Failed to search documents"
                        )
                    }
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            documentRepository.deleteDocument(documentId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    loadDocumentCounts()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to delete document"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
