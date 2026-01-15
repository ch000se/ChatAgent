package com.example.chatagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_users")
data class SupportUserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val name: String,
    val subscription: String,
    val registeredAt: Long,
    val lastActiveAt: Long,
    val totalTickets: Int = 0,
    val resolvedTickets: Int = 0,
    val metadataJson: String? = null
)
