package com.example.chatagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val jsonResponseJson: String? = null,
    val tokenUsageJson: String? = null,
    val isSummary: Boolean = false,
    val summarizedMessageCount: Int? = null,
    val originalTokenCount: Int? = null,
    val sourcesJson: String? = null
)
