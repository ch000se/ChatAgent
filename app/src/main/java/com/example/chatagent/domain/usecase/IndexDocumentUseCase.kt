package com.example.chatagent.domain.usecase

import com.example.chatagent.domain.model.IndexingProgress
import com.example.chatagent.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IndexDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String): Flow<IndexingProgress> {
        return repository.indexDocument(documentId)
    }
}
