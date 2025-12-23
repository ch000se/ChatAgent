package com.example.chatagent.data.repository

import android.util.Log
import com.example.chatagent.data.local.dao.DocumentDao
import com.example.chatagent.data.local.dao.EmbeddingDao
import com.example.chatagent.data.mapper.toDomain
import com.example.chatagent.data.mapper.toEntity
import com.example.chatagent.data.util.TfidfVectorizer
import com.example.chatagent.domain.model.Document
import com.example.chatagent.domain.model.DocumentChunk
import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.model.IndexingProgress
import com.example.chatagent.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val embeddingDao: EmbeddingDao
) : DocumentRepository {

    private val TAG = "DocumentRepositoryImpl"
    private val CHUNK_SIZE = 500
    private val CHUNK_OVERLAP = 50
    private val vectorizer = TfidfVectorizer()

    override suspend fun addDocument(
        fileName: String,
        content: String,
        contentType: String
    ): Result<Document> = withContext(Dispatchers.IO) {
        try {
            val document = Document(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                content = content,
                contentType = contentType,
                fileSize = content.length.toLong(),
                uploadedAt = System.currentTimeMillis(),
                indexed = false,
                indexedAt = null,
                chunkCount = 0
            )

            documentDao.insertDocument(document.toEntity())
            Log.d(TAG, "Document added: ${document.fileName}")
            Result.success(document)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding document", e)
            Result.failure(e)
        }
    }

    override suspend fun indexDocument(documentId: String): Flow<IndexingProgress> = flow {
        try {
            val documentEntity = documentDao.getDocumentById(documentId)
            if (documentEntity == null) {
                Log.e(TAG, "Document not found: $documentId")
                return@flow
            }

            val document = documentEntity.toDomain()
            val chunks = chunkText(document.content)

            emit(IndexingProgress(
                documentId = documentId,
                fileName = document.fileName,
                totalChunks = chunks.size,
                processedChunks = 0,
                currentStatus = "Starting indexing..."
            ))

            // Train vectorizer on all existing chunks + new chunks
            emit(IndexingProgress(
                documentId = documentId,
                fileName = document.fileName,
                totalChunks = chunks.size,
                processedChunks = 0,
                currentStatus = "Training vectorizer..."
            ))

            val allExistingChunks = embeddingDao.getAllEmbeddingsList()
                .map { it.toDomain().text }
            val allTexts = allExistingChunks + chunks

            vectorizer.fit(allTexts)
            Log.d(TAG, "Vectorizer trained on ${allTexts.size} chunks, vocabulary size: ${vectorizer.getVocabularySize()}")

            chunks.forEachIndexed { index, chunkText ->
                emit(IndexingProgress(
                    documentId = documentId,
                    fileName = document.fileName,
                    totalChunks = chunks.size,
                    processedChunks = index,
                    currentStatus = "Processing chunk ${index + 1}/${chunks.size}"
                ))

                val embedding = generateEmbedding(chunkText)

                val chunk = DocumentChunk(
                    id = UUID.randomUUID().toString(),
                    documentId = documentId,
                    chunkIndex = index,
                    text = chunkText,
                    embedding = embedding,
                    createdAt = System.currentTimeMillis()
                )

                embeddingDao.insertEmbedding(chunk.toEntity())
            }

            documentDao.updateIndexStatus(
                documentId = documentId,
                indexed = true,
                indexedAt = System.currentTimeMillis(),
                chunkCount = chunks.size
            )

            emit(IndexingProgress(
                documentId = documentId,
                fileName = document.fileName,
                totalChunks = chunks.size,
                processedChunks = chunks.size,
                currentStatus = "Indexing completed"
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Error indexing document", e)
            emit(IndexingProgress(
                documentId = documentId,
                fileName = "",
                totalChunks = 0,
                processedChunks = 0,
                currentStatus = "Error: ${e.message}"
            ))
        }
    }

    override suspend fun searchDocuments(
        query: String,
        topK: Int
    ): Result<List<DocumentSearchResult>> = withContext(Dispatchers.IO) {
        try {
            // Get all chunks
            val allChunkEntities = embeddingDao.getAllEmbeddingsList()

            if (allChunkEntities.isEmpty()) {
                Log.w(TAG, "No indexed documents found")
                return@withContext Result.success(emptyList())
            }

            // Train vectorizer on all chunk texts
            val allChunkTexts = allChunkEntities.map { it.toDomain().text }
            vectorizer.fit(allChunkTexts)
            Log.d(TAG, "Vectorizer trained for search on ${allChunkTexts.size} chunks, vocab size: ${vectorizer.getVocabularySize()}")

            Log.d(TAG, "=== SEARCH QUERY: '$query' ===")
            val queryEmbedding = generateEmbedding(query)

            // CRITICAL FIX: Regenerate embeddings with current vectorizer state
            // instead of using stored embeddings (which may have different vocabulary)
            val results = mutableListOf<DocumentSearchResult>()

            for (embeddingEntity in allChunkEntities) {
                val chunk = embeddingEntity.toDomain()

                // Generate fresh embedding with current vocabulary
                val chunkEmbedding = generateEmbedding(chunk.text)
                val similarity = cosineSimilarity(queryEmbedding, chunkEmbedding)

                val documentEntity = documentDao.getDocumentById(chunk.documentId)
                if (documentEntity != null) {
                    results.add(
                        DocumentSearchResult(
                            chunk = chunk,
                            document = documentEntity.toDomain(),
                            similarity = similarity,
                            rank = 0
                        )
                    )

                    // Log each chunk's similarity
                    Log.d(TAG, "Chunk #${chunk.chunkIndex} from '${documentEntity.fileName}': similarity=${String.format("%.4f", similarity)}, text='${chunk.text.take(60)}...'")
                }
            }

            val rankedResults = results
                .sortedByDescending { it.similarity }
                .take(topK)
                .mapIndexed { index, result ->
                    result.copy(rank = index + 1)
                }

            Log.d(TAG, "=== TOP $topK RESULTS ===")
            rankedResults.forEach { result ->
                Log.d(TAG, "Rank ${result.rank}: similarity=${String.format("%.4f", result.similarity)}, doc='${result.document.fileName}', chunk='${result.chunk.text.take(60)}...'")
            }
            Log.d(TAG, "Search completed: found ${rankedResults.size} results")
            Result.success(rankedResults)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents", e)
            Result.failure(e)
        }
    }

    override suspend fun getDocumentById(documentId: String): Result<Document?> =
        withContext(Dispatchers.IO) {
            try {
                val document = documentDao.getDocumentById(documentId)?.toDomain()
                Result.success(document)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting document", e)
                Result.failure(e)
            }
        }

    override suspend fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getIndexedDocuments(): Flow<List<Document>> {
        return documentDao.getIndexedDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                embeddingDao.deleteEmbeddingsByDocumentId(documentId)
                documentDao.deleteDocument(documentId)
                Log.d(TAG, "Document deleted: $documentId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting document", e)
                Result.failure(e)
            }
        }

    override suspend fun deleteAllDocuments(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                embeddingDao.deleteAllEmbeddings()
                documentDao.deleteAllDocuments()
                Log.d(TAG, "All documents deleted")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all documents", e)
                Result.failure(e)
            }
        }

    override suspend fun getDocumentsCount(): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val count = documentDao.getDocumentsCount()
                Result.success(count)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting documents count", e)
                Result.failure(e)
            }
        }

    override suspend fun getIndexedDocumentsCount(): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val count = documentDao.getIndexedDocumentsCount()
                Result.success(count)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting indexed documents count", e)
                Result.failure(e)
            }
        }

    private fun chunkText(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + CHUNK_SIZE, text.length)
            val chunk = text.substring(startIndex, endIndex)
            chunks.add(chunk)

            startIndex += CHUNK_SIZE - CHUNK_OVERLAP
        }

        return chunks
    }

    /**
     * Generates local TF-IDF embedding for text
     */
    private fun generateEmbedding(text: String): List<Float> {
        return vectorizer.transform(text)
    }

    private fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
        if (vec1.size != vec2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else {
            0f
        }
    }
}
