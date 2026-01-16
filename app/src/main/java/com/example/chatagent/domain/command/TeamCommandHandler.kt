package com.example.chatagent.domain.command

import android.content.Context
import android.util.Log
import com.example.chatagent.data.local.dao.EpicDao
import com.example.chatagent.data.local.dao.SprintDao
import com.example.chatagent.data.local.dao.TaskDao
import com.example.chatagent.data.local.dao.TeamMemberDao
import com.example.chatagent.data.mapper.TeamMapper
import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.*
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Handles /team commands for team assistant functionality.
 * Provides task management, project status, priority recommendations, and AI-powered insights.
 */
class TeamCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val chatApiService: ChatApiService,
    private val taskDao: TaskDao,
    private val teamMemberDao: TeamMemberDao,
    private val sprintDao: SprintDao,
    private val epicDao: EpicDao
) : CommandHandler<Command.Team> {

    companion object {
        private const val TAG = "TeamCommandHandler"
        private const val TEAM_DATA_FILE = "team_data.json"
        private const val RAG_SEARCH_PREFIX = "project roadmap task"
    }

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override suspend fun handle(command: Command.Team): CommandResult {
        val startTime = System.currentTimeMillis()

        return try {
            Log.d(TAG, "Processing /team command: action=${command.action}, params='${command.params}'")

            // Load or initialize team data
            ensureTeamDataLoaded()

            val result = when (command.action) {
                TeamAction.STATUS -> handleStatusRequest()
                TeamAction.TASKS -> handleTasksRequest(command.params)
                TeamAction.PRIORITY -> handlePriorityRequest(command.params)
                TeamAction.CREATE -> handleCreateTask(command.params)
                TeamAction.UPDATE -> handleUpdateTask(command.params)
                TeamAction.ROADMAP -> handleRoadmapRequest()
                TeamAction.BLOCKERS -> handleBlockersRequest()
                TeamAction.DEADLINES -> handleDeadlinesRequest()
                TeamAction.WORKLOAD -> handleWorkloadRequest()
                TeamAction.STATS -> handleStatsRequest()
                TeamAction.HELP -> handleHelpRequest()
            }

            CommandResult(
                command = command,
                content = result,
                success = true,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "team",
                    matchCount = null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in team command", e)
            CommandResult(
                command = command,
                content = buildErrorResponse(e),
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "team",
                    matchCount = 0
                )
            )
        }
    }

    // ===== HANDLER METHODS =====

    private suspend fun handleStatusRequest(): String {
        val allTasks = taskDao.getAllTasksOnce()
        val tasks = TeamMapper.fromEntities(allTasks)

        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
        val inProgressTasks = tasks.count { it.status == TaskStatus.IN_PROGRESS }
        val todoTasks = tasks.count { it.status == TaskStatus.TODO }
        val blockedTasks = tasks.count { it.status == TaskStatus.BLOCKED }
        val backlogTasks = tasks.count { it.status == TaskStatus.BACKLOG }

        val completionPercentage = if (totalTasks > 0) (completedTasks * 100f / totalTasks) else 0f

        // Get upcoming deadlines (next 7 days)
        val weekFromNow = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000
        val upcomingDeadlines = taskDao.getUpcomingDeadlines(weekFromNow, 5)
        val overdueTasks = taskDao.getOverdueTasks(System.currentTimeMillis())

        // Get high priority active tasks
        val highPriorityTasks = taskDao.getHighPriorityActiveTasks(5)

        // Calculate risk level
        val riskLevel = when {
            overdueTasks.size > 3 || blockedTasks > totalTasks * 0.2 -> RiskLevel.HIGH
            overdueTasks.isNotEmpty() || blockedTasks > totalTasks * 0.1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Build status with AI insights
        val statusContext = buildStatusContext(
            totalTasks, completedTasks, inProgressTasks, todoTasks,
            blockedTasks, backlogTasks, completionPercentage, riskLevel,
            overdueTasks.size, upcomingDeadlines.size
        )

        val ragResults = searchDocumentsUseCase(
            query = "$RAG_SEARCH_PREFIX status progress",
            topK = 2
        ).getOrNull() ?: emptyList()

        val ragContext = buildRagContext(ragResults)

        return callTeamAssistantAI(
            action = "status",
            context = statusContext,
            ragContext = ragContext,
            additionalData = buildHighPriorityTasksList(highPriorityTasks)
        )
    }

    private suspend fun handleTasksRequest(params: String): String {
        val filterParams = parseTaskFilter(params)

        val tasks = when {
            filterParams.priority != null -> {
                taskDao.getTasksByPriority(filterParams.priority.name)
            }
            filterParams.status != null -> {
                taskDao.getTasksByStatus(filterParams.status.name)
            }
            filterParams.assignee != null -> {
                taskDao.getTasksByAssignee(filterParams.assignee)
            }
            filterParams.searchQuery != null -> {
                taskDao.searchTasks(filterParams.searchQuery, filterParams.limit)
            }
            else -> {
                taskDao.getAllTasksOnce().take(filterParams.limit)
            }
        }

        if (tasks.isEmpty()) {
            return "📭 No tasks found matching your criteria.\n\nTry:\n- `/team tasks` - show all tasks\n- `/team tasks priority high` - filter by priority\n- `/team tasks status in_progress` - filter by status"
        }

        val tasksList = TeamMapper.fromEntities(tasks)
        val tasksContext = buildTasksListContext(tasksList, filterParams)

        val ragResults = searchDocumentsUseCase(
            query = "$RAG_SEARCH_PREFIX ${params.ifEmpty { "list overview" }}",
            topK = 2
        ).getOrNull() ?: emptyList()

        val ragContext = buildRagContext(ragResults)

        return callTeamAssistantAI(
            action = "tasks",
            context = tasksContext,
            ragContext = ragContext,
            additionalData = "Filter applied: ${filterParams.description}"
        )
    }

    private suspend fun handlePriorityRequest(params: String): String {
        // Get tasks that need prioritization
        val activeTasks = taskDao.getAllTasksOnce()
            .filter { it.status !in listOf("COMPLETED", "CANCELLED") }

        if (activeTasks.isEmpty()) {
            return "✅ No active tasks to prioritize. All tasks are completed or cancelled."
        }

        val tasks = TeamMapper.fromEntities(activeTasks)

        // Build context for AI priority analysis
        val priorityContext = buildPriorityAnalysisContext(tasks)

        // Get project knowledge for informed recommendations
        val ragResults = searchDocumentsUseCase(
            query = "$RAG_SEARCH_PREFIX priority roadmap dependencies",
            topK = 3
        ).getOrNull() ?: emptyList()

        val ragContext = buildRagContext(ragResults)

        // Get additional context
        val blockedTasks = taskDao.getBlockedTasks()
        val upcomingDeadlines = taskDao.getUpcomingDeadlines(
            System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000, 10
        )

        val additionalContext = """
            |BLOCKED TASKS (${blockedTasks.size}):
            |${blockedTasks.take(5).joinToString("\n") { "- ${it.title} [${it.priority}]" }}
            |
            |UPCOMING DEADLINES (next 14 days):
            |${upcomingDeadlines.take(5).joinToString("\n") { "- ${it.title} - Due: ${formatDate(it.dueDate)}" }}
        """.trimMargin()

        return callTeamAssistantAI(
            action = "priority",
            context = priorityContext,
            ragContext = ragContext,
            additionalData = additionalContext
        )
    }

    private suspend fun handleCreateTask(params: String): String {
        if (params.isBlank()) {
            return """
                |📝 Create Task
                |
                |Usage: /team create <task title>
                |
                |Examples:
                |  /team create Implement user authentication
                |  /team create Fix bug in login flow
                |  /team create Add dark mode support
                |
                |The task will be created with default settings:
                |  - Status: TODO
                |  - Priority: MEDIUM
                |  - Category: FEATURE
            """.trimMargin()
        }

        // Create new task
        val newTask = TaskItem(
            title = params,
            description = "Created via /team create command",
            status = TaskStatus.TODO,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.FEATURE
        )

        val entity = TeamMapper.toEntity(newTask)
        taskDao.insertTask(entity)

        Log.d(TAG, "Created new task: ${newTask.id} - ${newTask.title}")

        return """
            |✅ Task Created Successfully
            |
            |📋 Task Details:
            |  ID: ${newTask.id.take(8)}...
            |  Title: ${newTask.title}
            |  Status: ${newTask.status}
            |  Priority: ${newTask.priority}
            |  Category: ${newTask.category}
            |  Created: ${dateTimeFormat.format(Date(newTask.createdAt))}
            |
            |💡 Next steps:
            |  - `/team tasks` - View all tasks
            |  - `/team update ${newTask.id.take(8)} priority high` - Update priority
            |  - `/team status` - Check project status
        """.trimMargin()
    }

    private suspend fun handleUpdateTask(params: String): String {
        if (params.isBlank()) {
            return """
                |✏️ Update Task
                |
                |Usage: /team update <task-id> <field> <value>
                |
                |Fields:
                |  - status: TODO, IN_PROGRESS, IN_REVIEW, BLOCKED, COMPLETED
                |  - priority: LOW, MEDIUM, HIGH, CRITICAL
                |  - assignee: <name>
                |
                |Examples:
                |  /team update abc123 status in_progress
                |  /team update abc123 priority high
                |  /team update abc123 assignee John
            """.trimMargin()
        }

        val parts = params.split(" ", limit = 3)
        if (parts.size < 3) {
            return "❌ Invalid format. Use: /team update <task-id> <field> <value>"
        }

        val taskIdPrefix = parts[0]
        val field = parts[1].lowercase()
        val value = parts[2]

        // Find task by ID prefix
        val allTasks = taskDao.getAllTasksOnce()
        val task = allTasks.find { it.id.startsWith(taskIdPrefix) }
            ?: return "❌ Task not found with ID starting with: $taskIdPrefix"

        when (field) {
            "status" -> {
                val status = runCatching { TaskStatus.valueOf(value.uppercase()) }
                    .getOrNull() ?: return "❌ Invalid status. Valid values: ${TaskStatus.entries.joinToString()}"

                if (status == TaskStatus.COMPLETED) {
                    taskDao.completeTask(task.id)
                } else {
                    taskDao.updateTaskStatus(task.id, status.name)
                }
            }
            "priority" -> {
                val priority = runCatching { TaskPriority.valueOf(value.uppercase()) }
                    .getOrNull() ?: return "❌ Invalid priority. Valid values: ${TaskPriority.entries.joinToString()}"

                taskDao.updateTaskPriority(task.id, priority.name)
            }
            "assignee" -> {
                taskDao.assignTask(task.id, value)
            }
            else -> {
                return "❌ Unknown field: $field. Valid fields: status, priority, assignee"
            }
        }

        return """
            |✅ Task Updated Successfully
            |
            |📋 ${task.title}
            |  Updated: $field → $value
            |  Time: ${dateTimeFormat.format(Date())}
        """.trimMargin()
    }

    private suspend fun handleRoadmapRequest(): String {
        val epics = epicDao.getActiveEpics()
        val allTasks = taskDao.getAllTasksOnce()

        if (epics.isEmpty() && allTasks.isEmpty()) {
            return "📍 No roadmap data available. Create tasks and epics to build your roadmap."
        }

        val roadmapContext = buildRoadmapContext(epics, allTasks)

        val ragResults = searchDocumentsUseCase(
            query = "$RAG_SEARCH_PREFIX roadmap milestones features",
            topK = 3
        ).getOrNull() ?: emptyList()

        val ragContext = buildRagContext(ragResults)

        return callTeamAssistantAI(
            action = "roadmap",
            context = roadmapContext,
            ragContext = ragContext,
            additionalData = "Total tasks: ${allTasks.size}, Active epics: ${epics.size}"
        )
    }

    private suspend fun handleBlockersRequest(): String {
        val blockedTasks = taskDao.getBlockedTasks()

        if (blockedTasks.isEmpty()) {
            return "✅ Great news! No blocked tasks at the moment."
        }

        val tasks = TeamMapper.fromEntities(blockedTasks)
        val blockersContext = buildBlockersContext(tasks)

        val ragResults = searchDocumentsUseCase(
            query = "$RAG_SEARCH_PREFIX blocked issues dependencies",
            topK = 2
        ).getOrNull() ?: emptyList()

        val ragContext = buildRagContext(ragResults)

        return callTeamAssistantAI(
            action = "blockers",
            context = blockersContext,
            ragContext = ragContext,
            additionalData = "Total blocked: ${blockedTasks.size}"
        )
    }

    private suspend fun handleDeadlinesRequest(): String {
        val twoWeeksFromNow = System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000
        val upcomingTasks = taskDao.getUpcomingDeadlines(twoWeeksFromNow, 15)
        val overdueTasks = taskDao.getOverdueTasks(System.currentTimeMillis())

        if (upcomingTasks.isEmpty() && overdueTasks.isEmpty()) {
            return "📅 No upcoming deadlines in the next 2 weeks."
        }

        val deadlinesContext = buildDeadlinesContext(
            TeamMapper.fromEntities(overdueTasks),
            TeamMapper.fromEntities(upcomingTasks)
        )

        return callTeamAssistantAI(
            action = "deadlines",
            context = deadlinesContext,
            ragContext = "",
            additionalData = "Overdue: ${overdueTasks.size}, Upcoming: ${upcomingTasks.size}"
        )
    }

    private suspend fun handleWorkloadRequest(): String {
        val allTasks = taskDao.getAllTasksOnce()
        val activeTasks = allTasks.filter { it.status !in listOf("COMPLETED", "CANCELLED") }

        // Group by assignee
        val tasksByAssignee = activeTasks.groupBy { it.assignee ?: "Unassigned" }

        val workloadContext = buildWorkloadContext(tasksByAssignee)

        return callTeamAssistantAI(
            action = "workload",
            context = workloadContext,
            ragContext = "",
            additionalData = "Total active tasks: ${activeTasks.size}"
        )
    }

    private suspend fun handleStatsRequest(): String {
        val allTasks = taskDao.getAllTasksOnce()
        val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000

        val createdThisWeek = taskDao.getTasksCreatedSince(oneWeekAgo)
        val completedThisWeek = taskDao.getTasksCompletedSince(oneWeekAgo)
        val avgCompletionTime = taskDao.getAverageCompletionTime() ?: 0L

        val tasks = TeamMapper.fromEntities(allTasks)

        val tasksByCategory = tasks.groupBy { it.category }.mapValues { it.value.size }
        val tasksByPriority = tasks.groupBy { it.priority }.mapValues { it.value.size }
        val tasksByStatus = tasks.groupBy { it.status }.mapValues { it.value.size }

        val statsContext = buildStatsContext(
            createdThisWeek, completedThisWeek, avgCompletionTime,
            tasksByCategory, tasksByPriority, tasksByStatus, allTasks.size
        )

        return callTeamAssistantAI(
            action = "stats",
            context = statsContext,
            ragContext = "",
            additionalData = ""
        )
    }

    private fun handleHelpRequest(): String {
        return """
            |🤖 Team Assistant - Available Commands
            |
            |📊 PROJECT OVERVIEW
            |  /team status     - Show project status and key metrics
            |  /team stats      - Show detailed statistics
            |  /team roadmap    - View project roadmap and milestones
            |
            |📋 TASK MANAGEMENT
            |  /team tasks                  - List all tasks
            |  /team tasks priority high    - Filter by priority (low/medium/high/critical)
            |  /team tasks status todo      - Filter by status
            |  /team tasks <search query>   - Search tasks
            |
            |✏️ TASK OPERATIONS
            |  /team create <title>              - Create new task
            |  /team update <id> status <value>  - Update task status
            |  /team update <id> priority <value> - Update priority
            |
            |⚠️ ISSUES & DEADLINES
            |  /team blockers   - Show blocked tasks
            |  /team deadlines  - Show upcoming deadlines
            |
            |👥 TEAM
            |  /team workload   - Show team workload distribution
            |
            |🎯 AI RECOMMENDATIONS
            |  /team priority   - Get AI-powered priority recommendations
            |
            |💡 Examples:
            |  /team status
            |  /team tasks priority high
            |  /team create Implement dark mode
            |  /team priority
        """.trimMargin()
    }

    // ===== CONTEXT BUILDERS =====

    private fun buildStatusContext(
        total: Int, completed: Int, inProgress: Int, todo: Int,
        blocked: Int, backlog: Int, completionPct: Float, risk: RiskLevel,
        overdue: Int, upcoming: Int
    ): String {
        return """
            |=== PROJECT STATUS ===
            |
            |📊 Task Metrics:
            |  Total Tasks: $total
            |  Completed: $completed (${completionPct.toInt()}%)
            |  In Progress: $inProgress
            |  To Do: $todo
            |  Blocked: $blocked
            |  Backlog: $backlog
            |
            |⚠️ Risk Assessment:
            |  Risk Level: $risk
            |  Overdue Tasks: $overdue
            |  Upcoming Deadlines (7 days): $upcoming
            |
            |=== END STATUS ===
        """.trimMargin()
    }

    private fun buildTasksListContext(tasks: List<TaskItem>, filter: TaskFilter): String {
        val builder = StringBuilder()
        builder.append("=== TASK LIST ===\n\n")
        builder.append("Filter: ${filter.description}\n")
        builder.append("Found: ${tasks.size} tasks\n\n")

        tasks.forEachIndexed { index, task ->
            val priorityEmoji = when (task.priority) {
                TaskPriority.CRITICAL -> "🔴"
                TaskPriority.HIGH -> "🟠"
                TaskPriority.MEDIUM -> "🟡"
                TaskPriority.LOW -> "🟢"
            }

            val statusEmoji = when (task.status) {
                TaskStatus.COMPLETED -> "✅"
                TaskStatus.IN_PROGRESS -> "🔄"
                TaskStatus.BLOCKED -> "🚫"
                TaskStatus.IN_REVIEW -> "👀"
                else -> "📋"
            }

            builder.append("${index + 1}. $statusEmoji $priorityEmoji ${task.title}\n")
            builder.append("   ID: ${task.id.take(8)}... | Status: ${task.status} | Priority: ${task.priority}\n")
            if (task.assignee != null) {
                builder.append("   Assignee: ${task.assignee}\n")
            }
            if (task.dueDate != null) {
                builder.append("   Due: ${formatDate(task.dueDate)}\n")
            }
            builder.append("\n")
        }

        builder.append("=== END TASK LIST ===\n")
        return builder.toString()
    }

    private fun buildPriorityAnalysisContext(tasks: List<TaskItem>): String {
        val builder = StringBuilder()
        builder.append("=== PRIORITY ANALYSIS REQUEST ===\n\n")

        builder.append("📋 Active Tasks (${tasks.size}):\n\n")

        // Group by priority
        val byPriority = tasks.groupBy { it.priority }

        TaskPriority.entries.reversed().forEach { priority ->
            val priorityTasks = byPriority[priority] ?: emptyList()
            if (priorityTasks.isNotEmpty()) {
                builder.append("$priority (${priorityTasks.size}):\n")
                priorityTasks.take(5).forEach { task ->
                    builder.append("  - ${task.title}")
                    task.dueDate?.let { builder.append(" [Due: ${formatDate(it)}]") }
                    if (task.dependencies.isNotEmpty()) {
                        builder.append(" [Has ${task.dependencies.size} dependencies]")
                    }
                    builder.append("\n")
                }
                if (priorityTasks.size > 5) {
                    builder.append("  ... and ${priorityTasks.size - 5} more\n")
                }
                builder.append("\n")
            }
        }

        builder.append("=== END PRIORITY ANALYSIS ===\n")
        return builder.toString()
    }

    private fun buildRoadmapContext(epics: List<com.example.chatagent.data.local.entity.EpicEntity>, tasks: List<com.example.chatagent.data.local.entity.TaskEntity>): String {
        val builder = StringBuilder()
        builder.append("=== PROJECT ROADMAP ===\n\n")

        if (epics.isNotEmpty()) {
            builder.append("📍 Milestones/Epics:\n\n")
            epics.forEach { epic ->
                val epicTasks = tasks.filter { it.epicId == epic.id }
                val completedCount = epicTasks.count { it.status == "COMPLETED" }
                val progress = if (epicTasks.isNotEmpty()) (completedCount * 100 / epicTasks.size) else 0

                builder.append("🎯 ${epic.name}\n")
                builder.append("   Status: ${epic.status}\n")
                builder.append("   Target: ${epic.targetDate?.let { formatDate(it) } ?: "Not set"}\n")
                builder.append("   Progress: $progress% ($completedCount/${epicTasks.size} tasks)\n")
                builder.append("   ${epic.description}\n\n")
            }
        }

        // Group tasks by category
        val tasksByCategory = tasks.groupBy { it.category }
        builder.append("📊 Tasks by Category:\n")
        tasksByCategory.forEach { (category, categoryTasks) ->
            val completed = categoryTasks.count { it.status == "COMPLETED" }
            builder.append("  $category: ${categoryTasks.size} tasks ($completed completed)\n")
        }

        builder.append("\n=== END ROADMAP ===\n")
        return builder.toString()
    }

    private fun buildBlockersContext(tasks: List<TaskItem>): String {
        val builder = StringBuilder()
        builder.append("=== BLOCKED TASKS ===\n\n")
        builder.append("⚠️ ${tasks.size} tasks are currently blocked:\n\n")

        tasks.forEachIndexed { index, task ->
            builder.append("${index + 1}. 🚫 ${task.title}\n")
            builder.append("   Priority: ${task.priority}\n")
            builder.append("   Blocked by: ${task.blockedBy.joinToString(", ").ifEmpty { "Unknown" }}\n")
            if (task.assignee != null) {
                builder.append("   Assignee: ${task.assignee}\n")
            }
            builder.append("   Created: ${formatDate(task.createdAt)}\n\n")
        }

        builder.append("=== END BLOCKERS ===\n")
        return builder.toString()
    }

    private fun buildDeadlinesContext(overdue: List<TaskItem>, upcoming: List<TaskItem>): String {
        val builder = StringBuilder()
        builder.append("=== DEADLINES ===\n\n")

        if (overdue.isNotEmpty()) {
            builder.append("🔴 OVERDUE (${overdue.size}):\n")
            overdue.forEach { task ->
                builder.append("  ⚠️ ${task.title}\n")
                builder.append("     Was due: ${task.dueDate?.let { formatDate(it) }}\n")
                builder.append("     Priority: ${task.priority} | Status: ${task.status}\n\n")
            }
        }

        if (upcoming.isNotEmpty()) {
            builder.append("📅 UPCOMING (${upcoming.size}):\n")
            upcoming.forEach { task ->
                val daysUntil = task.dueDate?.let {
                    ((it - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                } ?: 0

                val urgencyEmoji = when {
                    daysUntil <= 1 -> "🔴"
                    daysUntil <= 3 -> "🟠"
                    daysUntil <= 7 -> "🟡"
                    else -> "🟢"
                }

                builder.append("  $urgencyEmoji ${task.title}\n")
                builder.append("     Due: ${task.dueDate?.let { formatDate(it) }} ($daysUntil days)\n")
                builder.append("     Priority: ${task.priority} | Status: ${task.status}\n\n")
            }
        }

        builder.append("=== END DEADLINES ===\n")
        return builder.toString()
    }

    private fun buildWorkloadContext(tasksByAssignee: Map<String, List<com.example.chatagent.data.local.entity.TaskEntity>>): String {
        val builder = StringBuilder()
        builder.append("=== TEAM WORKLOAD ===\n\n")

        tasksByAssignee.entries.sortedByDescending { it.value.size }.forEach { (assignee, tasks) ->
            val highPriority = tasks.count { it.priority in listOf("HIGH", "CRITICAL") }
            val inProgress = tasks.count { it.status == "IN_PROGRESS" }

            val workloadLevel = when {
                tasks.size > 10 -> "🔴 Overloaded"
                tasks.size > 5 -> "🟠 Heavy"
                tasks.size > 2 -> "🟡 Moderate"
                else -> "🟢 Light"
            }

            builder.append("👤 $assignee: ${tasks.size} tasks $workloadLevel\n")
            builder.append("   High Priority: $highPriority | In Progress: $inProgress\n")

            tasks.filter { it.status == "IN_PROGRESS" }.take(3).forEach { task ->
                builder.append("   🔄 ${task.title}\n")
            }
            builder.append("\n")
        }

        builder.append("=== END WORKLOAD ===\n")
        return builder.toString()
    }

    private fun buildStatsContext(
        createdThisWeek: Int,
        completedThisWeek: Int,
        avgCompletionTime: Long,
        tasksByCategory: Map<TaskCategory, Int>,
        tasksByPriority: Map<TaskPriority, Int>,
        tasksByStatus: Map<TaskStatus, Int>,
        totalTasks: Int
    ): String {
        val avgDays = avgCompletionTime / (24 * 60 * 60 * 1000)

        return """
            |=== PROJECT STATISTICS ===
            |
            |📈 This Week:
            |  Created: $createdThisWeek tasks
            |  Completed: $completedThisWeek tasks
            |  Velocity: ${if (createdThisWeek > 0) (completedThisWeek * 100 / createdThisWeek) else 0}%
            |
            |⏱️ Efficiency:
            |  Avg Completion Time: $avgDays days
            |
            |📊 By Status:
            |${tasksByStatus.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }}
            |
            |🎯 By Priority:
            |${tasksByPriority.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }}
            |
            |📁 By Category:
            |${tasksByCategory.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }}
            |
            |Total Tasks: $totalTasks
            |
            |=== END STATISTICS ===
        """.trimMargin()
    }

    private fun buildHighPriorityTasksList(tasks: List<com.example.chatagent.data.local.entity.TaskEntity>): String {
        if (tasks.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append("🎯 HIGH PRIORITY ACTIVE TASKS:\n")
        tasks.forEach { task ->
            builder.append("  - [${task.priority}] ${task.title}\n")
        }
        return builder.toString()
    }

    private fun buildRagContext(results: List<DocumentSearchResult>): String {
        if (results.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append("=== PROJECT KNOWLEDGE (from documentation) ===\n\n")

        results.forEachIndexed { index, result ->
            val similarity = (result.similarity * 100).toInt()
            builder.append("--- Doc ${index + 1} (relevance: $similarity%) ---\n")
            builder.append("${result.chunk.text.take(500)}\n\n")
        }

        builder.append("=== END PROJECT KNOWLEDGE ===\n")
        return builder.toString()
    }

    // ===== AI INTEGRATION =====

    private suspend fun callTeamAssistantAI(
        action: String,
        context: String,
        ragContext: String,
        additionalData: String
    ): String {
        val userMessage = """
            |Action: $action
            |
            |$context
            |
            |$ragContext
            |
            |$additionalData
            |
            |Please provide a clear, actionable response with recommendations.
        """.trimMargin()

        val request = ChatRequest(
            model = "claude-sonnet-4-5-20250929",
            system = SystemPrompts.TEAM_ASSISTANT.prompt,
            messages = listOf(
                MessageDto(role = "user", content = userMessage)
            ),
            maxTokens = 2048,
            temperature = 0.5
        )

        val response = chatApiService.sendMessage(request)
        return response.content.firstOrNull()?.text
            ?: "❌ Failed to generate team assistant response. Please try again."
    }

    // ===== UTILITY METHODS =====

    private suspend fun ensureTeamDataLoaded() {
        val existingTasks = taskDao.getAllTasksOnce()
        if (existingTasks.isEmpty()) {
            loadInitialTeamData()
        }
    }

    private suspend fun loadInitialTeamData() {
        try {
            val jsonString = context.assets.open(TEAM_DATA_FILE)
                .bufferedReader()
                .use { it.readText() }

            val teamData = gson.fromJson(jsonString, TeamDataJson::class.java)

            // Insert tasks
            teamData.tasks.forEach { taskJson ->
                val task = TaskItem(
                    id = taskJson.id,
                    title = taskJson.title,
                    description = taskJson.description,
                    assignee = taskJson.assignee,
                    status = runCatching { TaskStatus.valueOf(taskJson.status) }.getOrDefault(TaskStatus.TODO),
                    priority = runCatching { TaskPriority.valueOf(taskJson.priority) }.getOrDefault(TaskPriority.MEDIUM),
                    category = runCatching { TaskCategory.valueOf(taskJson.category) }.getOrDefault(TaskCategory.FEATURE),
                    dueDate = taskJson.dueDate,
                    estimatedHours = taskJson.estimatedHours,
                    tags = taskJson.tags,
                    dependencies = taskJson.dependencies,
                    blockedBy = taskJson.blockedBy,
                    createdAt = taskJson.createdAt ?: System.currentTimeMillis(),
                    updatedAt = taskJson.updatedAt ?: System.currentTimeMillis()
                )
                taskDao.insertTask(TeamMapper.toEntity(task))
            }

            Log.d(TAG, "Loaded ${teamData.tasks.size} tasks from team_data.json")
        } catch (e: Exception) {
            Log.w(TAG, "Could not load team_data.json, starting with empty data", e)
        }
    }

    private fun parseTaskFilter(params: String): TaskFilter {
        val parts = params.lowercase().split(" ").filter { it.isNotBlank() }

        var priority: TaskPriority? = null
        var status: TaskStatus? = null
        var assignee: String? = null
        var searchQuery: String? = null

        var i = 0
        while (i < parts.size) {
            when (parts[i]) {
                "priority" -> {
                    if (i + 1 < parts.size) {
                        priority = runCatching { TaskPriority.valueOf(parts[i + 1].uppercase()) }.getOrNull()
                        i++
                    }
                }
                "status" -> {
                    if (i + 1 < parts.size) {
                        status = runCatching { TaskStatus.valueOf(parts[i + 1].uppercase().replace("-", "_")) }.getOrNull()
                        i++
                    }
                }
                "assignee", "assigned" -> {
                    if (i + 1 < parts.size) {
                        assignee = parts[i + 1]
                        i++
                    }
                }
                else -> {
                    // If not a known filter, treat as search query
                    searchQuery = parts.subList(i, parts.size).joinToString(" ")
                    break
                }
            }
            i++
        }

        val description = buildString {
            if (priority != null) append("priority=$priority ")
            if (status != null) append("status=$status ")
            if (assignee != null) append("assignee=$assignee ")
            if (searchQuery != null) append("search='$searchQuery' ")
            if (isEmpty()) append("all tasks")
        }.trim()

        return TaskFilter(
            priority = priority,
            status = status,
            assignee = assignee,
            searchQuery = searchQuery,
            description = description
        )
    }

    private fun formatDate(timestamp: Long?): String {
        return timestamp?.let { dateFormat.format(Date(it)) } ?: "N/A"
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.Team
    }

    private fun buildErrorResponse(error: Exception): String {
        return """
            |❌ Team Assistant Error
            |
            |An error occurred: ${error.message}
            |
            |💡 Try these commands:
            |  /team help - Show all available commands
            |  /team status - Check project status
            |  /team tasks - List tasks
            |
            |If the problem persists, check your database connection.
        """.trimMargin()
    }
}

// Data classes for JSON parsing
private data class TaskFilter(
    val priority: TaskPriority? = null,
    val status: TaskStatus? = null,
    val assignee: String? = null,
    val searchQuery: String? = null,
    val limit: Int = 20,
    val description: String = "all tasks"
)

private data class TeamDataJson(
    val tasks: List<TaskJson>,
    val members: List<MemberJson>? = null,
    val sprints: List<SprintJson>? = null,
    val epics: List<EpicJson>? = null
)

private data class TaskJson(
    val id: String,
    val title: String,
    val description: String,
    val assignee: String?,
    val status: String,
    val priority: String,
    val category: String,
    val dueDate: Long?,
    val estimatedHours: Int?,
    val tags: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val blockedBy: List<String> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

private data class MemberJson(
    val id: String,
    val name: String,
    val role: String,
    val email: String?
)

private data class SprintJson(
    val id: String,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val status: String
)

private data class EpicJson(
    val id: String,
    val name: String,
    val description: String,
    val targetDate: Long?,
    val status: String
)
