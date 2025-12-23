package com.example.chatagent.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "embeddings",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class EmbeddingEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
    val embeddingVector: String, // JSON array of floats
    val createdAt: Long
)
