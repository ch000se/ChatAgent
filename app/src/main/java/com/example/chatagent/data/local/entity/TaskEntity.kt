package com.example.chatagent.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing tasks in the team management system.
 * Supports task tracking, dependencies, and sprint/epic organization.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["assignee"]),
        Index(value = ["sprintId"]),
        Index(value = ["epicId"]),
        Index(value = ["dueDate"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val assignee: String?,
    val status: String, // TaskStatus enum name
    val priority: String, // TaskPriority enum name
    val category: String, // TaskCategory enum name
    val dueDate: Long?,
    val estimatedHours: Int?,
    val tagsJson: String?, // JSON array of tags
    val dependenciesJson: String?, // JSON array of task IDs this depends on
    val blockedByJson: String?, // JSON array of blocking task IDs
    val linkedTicketsJson: String?, // JSON array of support ticket IDs
    val sprintId: String?,
    val epicId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?
)

/**
 * Room entity for storing team members.
 */
@Entity(
    tableName = "team_members",
    indices = [
        Index(value = ["role"])
    ]
)
data class TeamMemberEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String,
    val email: String?,
    val avatarUrl: String?,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Room entity for storing sprints.
 */
@Entity(
    tableName = "sprints",
    indices = [
        Index(value = ["status"])
    ]
)
data class SprintEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val startDate: Long,
    val endDate: Long,
    val status: String, // "PLANNING", "ACTIVE", "COMPLETED"
    val goalDescription: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Room entity for storing epics (large features/milestones).
 */
@Entity(
    tableName = "epics",
    indices = [
        Index(value = ["status"])
    ]
)
data class EpicEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val targetDate: Long?,
    val status: String, // "PLANNED", "IN_PROGRESS", "COMPLETED", "DELAYED"
    val color: String?, // Hex color for UI
    val createdAt: Long,
    val updatedAt: Long
)
