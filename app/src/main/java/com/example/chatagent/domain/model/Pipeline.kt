package com.example.chatagent.domain.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Represents a single step in the MCP pipeline
 */
data class PipelineStep(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val serverUrl: String,
    val toolName: String,
    val arguments: Map<String, Any> = emptyMap(),
    val status: StepStatus = StepStatus.PENDING,
    val result: String? = null,
    val error: String? = null,
    val order: Int
) {
    enum class StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}

/**
 * Represents a complete MCP pipeline configuration
 */
data class PipelineConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val steps: List<PipelineStep>,
    val status: PipelineStatus = PipelineStatus.READY,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class PipelineStatus {
        READY,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}

/**
 * Result of pipeline execution
 */
data class PipelineExecutionResult(
    val pipelineId: String,
    val status: PipelineConfig.PipelineStatus,
    val stepResults: List<StepExecutionResult>,
    val finalOutput: String?,
    val startTime: Long,
    val endTime: Long?,
    val totalDuration: Long? = endTime?.let { it - startTime }
)

/**
 * Result of a single step execution
 */
data class StepExecutionResult(
    val stepId: String,
    val stepName: String,
    val status: PipelineStep.StepStatus,
    val input: Map<String, Any>,
    val output: String?,
    val error: String?,
    val duration: Long
)

/**
 * Search result from web search MCP server
 */
data class SearchResult(
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String,
    @SerializedName("description") val description: String?,
    @SerializedName("content") val content: String?
)

/**
 * Summary generated from search results
 */
data class Summary(
    val originalQuery: String,
    val sources: List<String>,
    val summaryText: String,
    val keyPoints: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * File save result from filesystem MCP server
 */
data class FileSaveResult(
    val filePath: String,
    val fileName: String,
    val size: Long,
    val success: Boolean,
    val message: String?
)
