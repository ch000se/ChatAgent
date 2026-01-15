package com.example.chatagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val priority: String,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long? = null,
    val assignedTo: String? = null,
    val tagsJson: String? = null,
    val relatedIssuesJson: String? = null
)
