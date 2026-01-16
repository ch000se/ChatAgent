package com.example.chatagent.data.local.dao

import androidx.room.*
import com.example.chatagent.data.local.entity.EpicEntity
import com.example.chatagent.data.local.entity.SprintEntity
import com.example.chatagent.data.local.entity.TaskEntity
import com.example.chatagent.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for task management operations.
 * Supports CRUD, filtering, searching, and statistics queries.
 */
@Dao
interface TaskDao {

    // ===== BASIC CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // ===== QUERIES =====

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY priority DESC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY priority DESC, dueDate ASC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, dueDate ASC")
    suspend fun getTasksByStatus(status: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY dueDate ASC")
    suspend fun getTasksByPriority(priority: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE assignee = :assignee ORDER BY priority DESC, dueDate ASC")
    suspend fun getTasksByAssignee(assignee: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY priority DESC, dueDate ASC")
    suspend fun getTasksByCategory(category: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE sprintId = :sprintId ORDER BY priority DESC")
    suspend fun getTasksBySprint(sprintId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE epicId = :epicId ORDER BY priority DESC")
    suspend fun getTasksByEpic(epicId: String): List<TaskEntity>

    // ===== FILTERED QUERIES =====

    @Query("""
        SELECT * FROM tasks
        WHERE status = :status AND priority = :priority
        ORDER BY dueDate ASC
    """)
    suspend fun getTasksByStatusAndPriority(status: String, priority: String): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED')
        AND priority IN ('HIGH', 'CRITICAL')
        ORDER BY priority DESC, dueDate ASC
        LIMIT :limit
    """)
    suspend fun getHighPriorityActiveTasks(limit: Int = 10): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'BLOCKED'
        ORDER BY priority DESC, createdAt ASC
    """)
    suspend fun getBlockedTasks(): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE dueDate IS NOT NULL
        AND dueDate <= :endDate
        AND status NOT IN ('COMPLETED', 'CANCELLED')
        ORDER BY dueDate ASC
        LIMIT :limit
    """)
    suspend fun getUpcomingDeadlines(endDate: Long, limit: Int = 10): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE dueDate IS NOT NULL
        AND dueDate < :currentTime
        AND status NOT IN ('COMPLETED', 'CANCELLED')
        ORDER BY dueDate ASC
    """)
    suspend fun getOverdueTasks(currentTime: Long): List<TaskEntity>

    // ===== SEARCH =====

    @Query("""
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        OR tagsJson LIKE '%' || :query || '%'
        ORDER BY priority DESC, updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchTasks(query: String, limit: Int = 20): List<TaskEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    suspend fun getTaskCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE priority = :priority")
    suspend fun getTaskCountByPriority(priority: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'IN_PROGRESS'")
    suspend fun getInProgressTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'BLOCKED'")
    suspend fun getBlockedTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'TODO'")
    suspend fun getTodoTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'BACKLOG'")
    suspend fun getBacklogTaskCount(): Int

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE createdAt >= :startTime
    """)
    suspend fun getTasksCreatedSince(startTime: Long): Int

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE completedAt IS NOT NULL AND completedAt >= :startTime
    """)
    suspend fun getTasksCompletedSince(startTime: Long): Int

    @Query("""
        SELECT AVG(completedAt - createdAt) FROM tasks
        WHERE completedAt IS NOT NULL
    """)
    suspend fun getAverageCompletionTime(): Long?

    // ===== DEPENDENCY QUERIES =====

    @Query("""
        SELECT * FROM tasks
        WHERE dependenciesJson LIKE '%' || :taskId || '%'
    """)
    suspend fun getTasksDependingOn(taskId: String): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE blockedByJson LIKE '%' || :taskId || '%'
    """)
    suspend fun getTasksBlockedBy(taskId: String): List<TaskEntity>

    // ===== UPDATE STATUS =====

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE tasks
        SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt
        WHERE id = :taskId
    """)
    suspend fun completeTask(
        taskId: String,
        status: String = "COMPLETED",
        completedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE tasks SET priority = :priority, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskPriority(taskId: String, priority: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET assignee = :assignee, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun assignTask(taskId: String, assignee: String?, updatedAt: Long = System.currentTimeMillis())
}

/**
 * DAO for team member operations.
 */
@Dao
interface TeamMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<TeamMemberEntity>)

    @Update
    suspend fun updateMember(member: TeamMemberEntity)

    @Delete
    suspend fun deleteMember(member: TeamMemberEntity)

    @Query("SELECT * FROM team_members WHERE id = :memberId")
    suspend fun getMemberById(memberId: String): TeamMemberEntity?

    @Query("SELECT * FROM team_members WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveMembers(): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team_members ORDER BY name ASC")
    suspend fun getAllMembersOnce(): List<TeamMemberEntity>

    @Query("SELECT * FROM team_members WHERE role = :role ORDER BY name ASC")
    suspend fun getMembersByRole(role: String): List<TeamMemberEntity>
}

/**
 * DAO for sprint operations.
 */
@Dao
interface SprintDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSprint(sprint: SprintEntity)

    @Update
    suspend fun updateSprint(sprint: SprintEntity)

    @Delete
    suspend fun deleteSprint(sprint: SprintEntity)

    @Query("SELECT * FROM sprints WHERE id = :sprintId")
    suspend fun getSprintById(sprintId: String): SprintEntity?

    @Query("SELECT * FROM sprints WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSprint(): SprintEntity?

    @Query("SELECT * FROM sprints ORDER BY startDate DESC")
    fun getAllSprints(): Flow<List<SprintEntity>>

    @Query("SELECT * FROM sprints ORDER BY startDate DESC")
    suspend fun getAllSprintsOnce(): List<SprintEntity>
}

/**
 * DAO for epic operations.
 */
@Dao
interface EpicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpic(epic: EpicEntity)

    @Update
    suspend fun updateEpic(epic: EpicEntity)

    @Delete
    suspend fun deleteEpic(epic: EpicEntity)

    @Query("SELECT * FROM epics WHERE id = :epicId")
    suspend fun getEpicById(epicId: String): EpicEntity?

    @Query("SELECT * FROM epics WHERE status != 'COMPLETED' ORDER BY targetDate ASC")
    suspend fun getActiveEpics(): List<EpicEntity>

    @Query("SELECT * FROM epics ORDER BY targetDate ASC")
    fun getAllEpics(): Flow<List<EpicEntity>>
}
