package com.example.chatagent.domain.usecase

import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.repository.DocumentRepository
import javax.inject.Inject

class SearchDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(query: String, topK: Int = 5): Result<List<DocumentSearchResult>> {
        return repository.searchDocuments(query, topK)
    }
}
