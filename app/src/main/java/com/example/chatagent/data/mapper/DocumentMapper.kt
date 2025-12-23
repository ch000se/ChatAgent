package com.example.chatagent.data.mapper

import com.example.chatagent.data.local.entity.DocumentEntity
import com.example.chatagent.data.local.entity.EmbeddingEntity
import com.example.chatagent.domain.model.Document
import com.example.chatagent.domain.model.DocumentChunk
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun DocumentEntity.toDomain(): Document {
    return Document(
        id = id,
        fileName = fileName,
        content = content,
        contentType = contentType,
        fileSize = fileSize,
        uploadedAt = uploadedAt,
        indexed = indexed,
        indexedAt = indexedAt,
        chunkCount = chunkCount
    )
}

fun Document.toEntity(): DocumentEntity {
    return DocumentEntity(
        id = id,
        fileName = fileName,
        content = content,
        contentType = contentType,
        fileSize = fileSize,
        uploadedAt = uploadedAt,
        indexed = indexed,
        indexedAt = indexedAt,
        chunkCount = chunkCount
    )
}

fun EmbeddingEntity.toDomain(): DocumentChunk {
    val embeddingList: List<Float> = try {
        val type = object : TypeToken<List<Float>>() {}.type
        gson.fromJson(embeddingVector, type)
    } catch (e: Exception) {
        emptyList()
    }

    return DocumentChunk(
        id = id,
        documentId = documentId,
        chunkIndex = chunkIndex,
        text = text,
        embedding = embeddingList,
        createdAt = createdAt
    )
}

fun DocumentChunk.toEntity(): EmbeddingEntity {
    val embeddingJson = gson.toJson(embedding ?: emptyList<Float>())

    return EmbeddingEntity(
        id = id,
        documentId = documentId,
        chunkIndex = chunkIndex,
        text = text,
        embeddingVector = embeddingJson,
        createdAt = createdAt
    )
}
