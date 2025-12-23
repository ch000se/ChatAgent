package com.example.chatagent.presentation.documents

import com.example.chatagent.domain.model.Document
import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.model.IndexingProgress

data class DocumentUiState(
    val documents: List<Document> = emptyList(),
    val selectedDocument: Document? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val indexingProgress: IndexingProgress? = null,
    val isIndexing: Boolean = false,

    val searchQuery: String = "",
    val searchResults: List<DocumentSearchResult> = emptyList(),
    val isSearching: Boolean = false,

    val totalDocuments: Int = 0,
    val indexedDocuments: Int = 0,

    val showAddDocumentDialog: Boolean = false,
    val documentContent: String = "",
    val documentName: String = "",
    val documentType: String = "text"
)
