package com.example.chatagent.domain.usecase

import android.util.Log
import com.example.chatagent.domain.repository.DocumentRepository
import com.example.chatagent.domain.util.ProjectDocumentScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class IndexProjectDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val documentScanner: ProjectDocumentScanner
) {
    private val TAG = "IndexProjectDocs"
    private val PROJECT_DOC_PREFIX = "PROJECT_DOC_"

    suspend operator fun invoke(): Flow<IndexingStatus> = flow {
        emit(IndexingStatus.Scanning)

        // Scan for project documents
        val projectDocs = documentScanner.scanProjectDocuments()

        if (projectDocs.isEmpty()) {
            emit(IndexingStatus.Completed(0, 0))
            return@flow
        }

        emit(IndexingStatus.Found(projectDocs.size))

        var indexed = 0
        var skipped = 0

        for ((index, doc) in projectDocs.withIndex()) {
            emit(IndexingStatus.Indexing(index + 1, projectDocs.size, doc.fileName))

            try {
                // Check if already indexed by filename prefix
                val fileName = "$PROJECT_DOC_PREFIX${doc.fileName}"
                val existingDoc = documentRepository.getDocumentByFileName(fileName)

                if (existingDoc != null) {
                    skipped++
                    Log.d(TAG, "Skipped (already indexed): ${doc.fileName}")
                    continue
                }

                // Add document with special prefix to mark as project doc
                val addResult = documentRepository.addDocument(
                    fileName = fileName,
                    content = doc.content,
                    contentType = "text/markdown"
                )

                if (addResult.isSuccess) {
                    val document = addResult.getOrNull()!!

                    // Index immediately
                    documentRepository.indexDocument(document.id).collect { progress ->
                        // Silent indexing progress
                        Log.d(TAG, "Indexing ${doc.fileName}: ${progress.processedChunks}/${progress.totalChunks}")
                    }

                    indexed++
                    Log.d(TAG, "Indexed: ${doc.fileName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to index ${doc.fileName}", e)
            }
        }

        emit(IndexingStatus.Completed(indexed, skipped))
        Log.d(TAG, "Indexing completed: $indexed indexed, $skipped skipped")
    }

    sealed class IndexingStatus {
        object Scanning : IndexingStatus()
        data class Found(val count: Int) : IndexingStatus()
        data class Indexing(val current: Int, val total: Int, val fileName: String) : IndexingStatus()
        data class Completed(val indexed: Int, val skipped: Int) : IndexingStatus()
    }
}
