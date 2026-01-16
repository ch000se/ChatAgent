package com.example.chatagent.data.mapper

import com.example.chatagent.data.local.entity.EpicEntity
import com.example.chatagent.data.local.entity.SprintEntity
import com.example.chatagent.data.local.entity.TaskEntity
import com.example.chatagent.data.local.entity.TeamMemberEntity
import com.example.chatagent.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Mapper for converting between Team domain models and database entities.
 */
object TeamMapper {

    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type

    // ===== TASK MAPPING =====

    fun toEntity(task: TaskItem): TaskEntity {
        return TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            assignee = task.assignee,
            status = task.status.name,
            priority = task.priority.name,
            category = task.category.name,
            dueDate = task.dueDate,
            estimatedHours = task.estimatedHours,
            tagsJson = if (task.tags.isNotEmpty()) gson.toJson(task.tags) else null,
            dependenciesJson = if (task.dependencies.isNotEmpty()) gson.toJson(task.dependencies) else null,
            blockedByJson = if (task.blockedBy.isNotEmpty()) gson.toJson(task.blockedBy) else null,
            linkedTicketsJson = if (task.linkedTickets.isNotEmpty()) gson.toJson(task.linkedTickets) else null,
            sprintId = task.sprintId,
            epicId = task.epicId,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            completedAt = task.completedAt
        )
    }

    fun fromEntity(entity: TaskEntity): TaskItem {
        return TaskItem(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            assignee = entity.assignee,
            status = runCatching { TaskStatus.valueOf(entity.status) }.getOrDefault(TaskStatus.TODO),
            priority = runCatching { TaskPriority.valueOf(entity.priority) }.getOrDefault(TaskPriority.MEDIUM),
            category = runCatching { TaskCategory.valueOf(entity.category) }.getOrDefault(TaskCategory.OTHER),
            dueDate = entity.dueDate,
            estimatedHours = entity.estimatedHours,
            tags = entity.tagsJson?.let { parseStringList(it) } ?: emptyList(),
            dependencies = entity.dependenciesJson?.let { parseStringList(it) } ?: emptyList(),
            blockedBy = entity.blockedByJson?.let { parseStringList(it) } ?: emptyList(),
            linkedTickets = entity.linkedTicketsJson?.let { parseStringList(it) } ?: emptyList(),
            sprintId = entity.sprintId,
            epicId = entity.epicId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            completedAt = entity.completedAt
        )
    }

    fun fromEntities(entities: List<TaskEntity>): List<TaskItem> {
        return entities.map { fromEntity(it) }
    }

    fun toEntities(tasks: List<TaskItem>): List<TaskEntity> {
        return tasks.map { toEntity(it) }
    }

    // ===== TEAM MEMBER MAPPING =====

    fun toMemberEntity(member: TeamMember): TeamMemberEntity {
        return TeamMemberEntity(
            id = member.id,
            name = member.name,
            role = member.role,
            email = member.email,
            avatarUrl = null,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun fromMemberEntity(entity: TeamMemberEntity): TeamMember {
        return TeamMember(
            id = entity.id,
            name = entity.name,
            role = entity.role,
            email = entity.email,
            assignedTasks = 0,
            completedTasksThisWeek = 0,
            workload = 0f
        )
    }

    // ===== SPRINT MAPPING =====

    fun toSprintEntity(
        id: String,
        name: String,
        description: String?,
        startDate: Long,
        endDate: Long,
        status: String
    ): SprintEntity {
        return SprintEntity(
            id = id,
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            status = status,
            goalDescription = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun fromSprintEntity(entity: SprintEntity, completedPoints: Int, totalPoints: Int, remainingTasks: Int): SprintProgress {
        val durationDays = (entity.endDate - entity.startDate) / (1000 * 60 * 60 * 24)
        val elapsedDays = (System.currentTimeMillis() - entity.startDate) / (1000 * 60 * 60 * 24)
        val expectedProgress = if (durationDays > 0) (elapsedDays.toFloat() / durationDays) else 0f
        val actualProgress = if (totalPoints > 0) (completedPoints.toFloat() / totalPoints) else 0f

        val burndownStatus = when {
            actualProgress >= expectedProgress + 0.1f -> "AHEAD"
            actualProgress <= expectedProgress - 0.1f -> "BEHIND"
            else -> "ON_TRACK"
        }

        return SprintProgress(
            sprintId = entity.id,
            sprintName = entity.name,
            startDate = entity.startDate,
            endDate = entity.endDate,
            totalPoints = totalPoints,
            completedPoints = completedPoints,
            remainingTasks = remainingTasks,
            velocity = actualProgress * 100,
            burndownStatus = burndownStatus
        )
    }

    // ===== EPIC MAPPING =====

    fun toEpicEntity(milestone: RoadmapMilestone): EpicEntity {
        return EpicEntity(
            id = milestone.id,
            name = milestone.name,
            description = milestone.description,
            targetDate = milestone.targetDate,
            status = milestone.status,
            color = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun fromEpicEntity(entity: EpicEntity, completionPercentage: Float, keyTasks: List<String>): RoadmapMilestone {
        return RoadmapMilestone(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            targetDate = entity.targetDate ?: 0L,
            status = entity.status,
            completionPercentage = completionPercentage,
            keyTasks = keyTasks,
            dependencies = emptyList()
        )
    }

    // ===== UTILITY =====

    private fun parseStringList(json: String): List<String> {
        return try {
            gson.fromJson(json, stringListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun stringListToJson(list: List<String>): String? {
        return if (list.isNotEmpty()) gson.toJson(list) else null
    }
}
