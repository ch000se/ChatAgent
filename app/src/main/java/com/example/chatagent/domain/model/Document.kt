package com.example.chatagent.domain.model

data class Document(
    val id: String,
    val fileName: String,
    val content: String,
    val contentType: String,
    val fileSize: Long,
    val uploadedAt: Long,
    val indexed: Boolean = false,
    val indexedAt: Long? = null,
    val chunkCount: Int = 0
)

data class DocumentChunk(
    val id: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
    val embedding: List<Float>? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DocumentSearchResult(
    val chunk: DocumentChunk,
    val document: Document,
    val similarity: Float,
    val rank: Int
)

data class IndexingProgress(
    val documentId: String,
    val fileName: String,
    val totalChunks: Int,
    val processedChunks: Int,
    val currentStatus: String
)
