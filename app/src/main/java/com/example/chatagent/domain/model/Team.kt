package com.example.chatagent.domain.model

import java.util.UUID

/**
 * Domain models for Team Assistant feature.
 * Provides task management, project status tracking, and priority recommendations.
 */

// ===== ENUMS =====

enum class TaskStatus {
    BACKLOG,
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    BLOCKED,
    COMPLETED,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class TaskCategory {
    FEATURE,
    BUG,
    IMPROVEMENT,
    REFACTORING,
    DOCUMENTATION,
    TESTING,
    INFRASTRUCTURE,
    RESEARCH,
    OTHER
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

// ===== CORE MODELS =====

/**
 * Represents a task/issue in the project
 */
data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val assignee: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.FEATURE,
    val dueDate: Long? = null,
    val estimatedHours: Int? = null,
    val tags: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(), // IDs of tasks this depends on
    val blockedBy: List<String> = emptyList(), // IDs of blocking tasks
    val linkedTickets: List<String> = emptyList(), // Support ticket IDs
    val sprintId: String? = null,
    val epicId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Project-wide status metrics
 */
data class ProjectStatus(
    val projectName: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val inProgressTasks: Int,
    val todoTasks: Int,
    val blockedTasks: Int,
    val backlogTasks: Int,
    val completionPercentage: Float,
    val onSchedule: Boolean,
    val riskLevel: RiskLevel,
    val currentSprintProgress: SprintProgress? = null,
    val upcomingDeadlines: List<TaskItem> = emptyList(),
    val blockers: List<TaskItem> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Sprint progress tracking
 */
data class SprintProgress(
    val sprintId: String,
    val sprintName: String,
    val startDate: Long,
    val endDate: Long,
    val totalPoints: Int,
    val completedPoints: Int,
    val remainingTasks: Int,
    val velocity: Float,
    val burndownStatus: String // "ON_TRACK", "BEHIND", "AHEAD"
)

/**
 * AI-generated priority recommendation
 */
data class PriorityRecommendation(
    val taskId: String,
    val taskTitle: String,
    val currentPriority: TaskPriority,
    val recommendedPriority: TaskPriority,
    val reasoning: String,
    val dependentTasks: List<String>,
    val estimatedImpact: String,
    val suggestedAction: String,
    val urgencyScore: Float // 0.0 to 1.0
)

/**
 * Team member info
 */
data class TeamMember(
    val id: String,
    val name: String,
    val role: String,
    val email: String? = null,
    val assignedTasks: Int = 0,
    val completedTasksThisWeek: Int = 0,
    val workload: Float = 0f // 0.0 to 1.0
)

/**
 * Query parameters for team commands
 */
data class TeamQuery(
    val action: TeamAction,
    val priority: TaskPriority? = null,
    val status: TaskStatus? = null,
    val assignee: String? = null,
    val category: TaskCategory? = null,
    val searchQuery: String? = null,
    val limit: Int = 10
)

enum class TeamAction {
    STATUS,          // Show project status
    TASKS,           // List tasks with filters
    PRIORITY,        // Get priority recommendations
    CREATE,          // Create new task
    UPDATE,          // Update existing task
    ROADMAP,         // Show project roadmap
    BLOCKERS,        // Show blocked tasks
    DEADLINES,       // Show upcoming deadlines
    WORKLOAD,        // Show team workload
    STATS,           // Show project statistics
    HELP             // Show available commands
}

/**
 * Response from team assistant with context
 */
data class TeamResponse(
    val action: TeamAction,
    val content: String,
    val tasks: List<TaskItem>? = null,
    val status: ProjectStatus? = null,
    val recommendations: List<PriorityRecommendation>? = null,
    val ragSources: List<String>? = null,
    val success: Boolean = true,
    val error: String? = null
)

/**
 * Project roadmap milestone
 */
data class RoadmapMilestone(
    val id: String,
    val name: String,
    val description: String,
    val targetDate: Long,
    val status: String, // "PLANNED", "IN_PROGRESS", "COMPLETED", "DELAYED"
    val completionPercentage: Float,
    val keyTasks: List<String>,
    val dependencies: List<String>
)

/**
 * Project statistics summary
 */
data class ProjectStats(
    val tasksCreatedThisWeek: Int,
    val tasksCompletedThisWeek: Int,
    val averageCompletionTime: Float, // in hours
    val blockerResolutionRate: Float,
    val teamVelocity: Float,
    val tasksByCategory: Map<TaskCategory, Int>,
    val tasksByPriority: Map<TaskPriority, Int>,
    val tasksByStatus: Map<TaskStatus, Int>,
    val topContributors: List<TeamMember>
)
