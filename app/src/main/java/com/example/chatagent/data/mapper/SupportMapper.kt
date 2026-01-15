package com.example.chatagent.data.mapper

import com.example.chatagent.data.local.entity.SupportTicketEntity
import com.example.chatagent.data.local.entity.SupportUserEntity
import com.example.chatagent.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SupportMapper {
    private val gson = Gson()

    fun toEntity(ticket: SupportTicket): SupportTicketEntity {
        return SupportTicketEntity(
            id = ticket.id,
            userId = ticket.userId,
            title = ticket.title,
            description = ticket.description,
            category = ticket.category.name,
            status = ticket.status.name,
            priority = ticket.priority.name,
            createdAt = ticket.createdAt,
            updatedAt = ticket.updatedAt,
            resolvedAt = ticket.resolvedAt,
            assignedTo = ticket.assignedTo,
            tagsJson = if (ticket.tags.isNotEmpty()) gson.toJson(ticket.tags) else null,
            relatedIssuesJson = if (ticket.relatedIssues.isNotEmpty()) gson.toJson(ticket.relatedIssues) else null
        )
    }

    fun fromEntity(entity: SupportTicketEntity): SupportTicket {
        val tagsType = object : TypeToken<List<String>>() {}.type
        val tags = entity.tagsJson?.let { gson.fromJson<List<String>>(it, tagsType) } ?: emptyList()
        val relatedIssues = entity.relatedIssuesJson?.let { gson.fromJson<List<String>>(it, tagsType) } ?: emptyList()

        return SupportTicket(
            id = entity.id,
            userId = entity.userId,
            title = entity.title,
            description = entity.description,
            category = SupportCategory.valueOf(entity.category),
            status = TicketStatus.valueOf(entity.status),
            priority = TicketPriority.valueOf(entity.priority),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            resolvedAt = entity.resolvedAt,
            assignedTo = entity.assignedTo,
            tags = tags,
            relatedIssues = relatedIssues
        )
    }

    fun toUserEntity(user: SupportUser): SupportUserEntity {
        return SupportUserEntity(
            id = user.id,
            email = user.email,
            name = user.name,
            subscription = user.subscription.name,
            registeredAt = user.registeredAt,
            lastActiveAt = user.lastActiveAt,
            totalTickets = user.totalTickets,
            resolvedTickets = user.resolvedTickets,
            metadataJson = if (user.metadata.isNotEmpty()) gson.toJson(user.metadata) else null
        )
    }

    fun fromUserEntity(entity: SupportUserEntity): SupportUser {
        val metadataType = object : TypeToken<Map<String, String>>() {}.type
        val metadata = entity.metadataJson?.let { gson.fromJson<Map<String, String>>(it, metadataType) } ?: emptyMap()

        return SupportUser(
            id = entity.id,
            email = entity.email,
            name = entity.name,
            subscription = SubscriptionType.valueOf(entity.subscription),
            registeredAt = entity.registeredAt,
            lastActiveAt = entity.lastActiveAt,
            totalTickets = entity.totalTickets,
            resolvedTickets = entity.resolvedTickets,
            metadata = metadata
        )
    }
}
