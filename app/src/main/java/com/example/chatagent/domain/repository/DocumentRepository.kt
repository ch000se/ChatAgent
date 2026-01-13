package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.Document
import com.example.chatagent.domain.model.DocumentChunk
import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.model.IndexingProgress
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun addDocument(fileName: String, content: String, contentType: String): Result<Document>
    suspend fun indexDocument(documentId: String): Flow<IndexingProgress>
    suspend fun searchDocuments(query: String, topK: Int = 5): Result<List<DocumentSearchResult>>
    suspend fun getDocumentById(documentId: String): Result<Document?>
    suspend fun getDocumentByFileName(fileName: String): Document?
    suspend fun getAllDocuments(): Flow<List<Document>>
    suspend fun getIndexedDocuments(): Flow<List<Document>>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    suspend fun deleteAllDocuments(): Result<Unit>
    suspend fun getDocumentsCount(): Result<Int>
    suspend fun getIndexedDocumentsCount(): Result<Int>
}
