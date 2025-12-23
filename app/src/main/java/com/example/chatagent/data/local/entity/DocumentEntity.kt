package com.example.chatagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val content: String,
    val contentType: String, // "text", "pdf", "markdown", etc.
    val fileSize: Long,
    val uploadedAt: Long,
    val indexed: Boolean = false,
    val indexedAt: Long? = null,
    val chunkCount: Int = 0
)
