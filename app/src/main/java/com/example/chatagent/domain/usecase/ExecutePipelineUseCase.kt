package com.example.chatagent.domain.usecase

import android.util.Log
import com.example.chatagent.data.remote.client.MultiMcpClient
import com.example.chatagent.data.remote.dto.CallToolResult
import com.example.chatagent.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for executing a complete MCP pipeline
 * Orchestrates multiple MCP servers and chains their tools together
 */
class ExecutePipelineUseCase @Inject constructor(
    private val multiMcpClient: MultiMcpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "ExecutePipelineUseCase"
    }

    /**
     * Execute a pipeline with real-time progress updates
     */
    fun execute(config: PipelineConfig): Flow<PipelineProgress> = flow {
        val startTime = System.currentTimeMillis()
        val stepResults = mutableListOf<StepExecutionResult>()

        try {
            emit(PipelineProgress.Started(config))

            // Step 1: Connect to all required servers
            emit(PipelineProgress.ConnectingToServers(config.steps.map { it.serverUrl }.distinct()))

            val serverUrls = config.steps.map { it.serverUrl }.distinct()
            for (serverUrl in serverUrls) {
                if (!multiMcpClient.isConnectedTo(serverUrl)) {
                    val connectResult = multiMcpClient.connectToServer(serverUrl)
                    if (connectResult.isFailure) {
                        emit(
                            PipelineProgress.Failed(
                                config,
                                "Failed to connect to server: $serverUrl",
                                stepResults
                            )
                        )
                        return@flow
                    }
                }
            }

            emit(PipelineProgress.ServersConnected(serverUrls))

            // Step 2: Execute pipeline steps sequentially
            var previousOutput: String? = null

            for (step in config.steps.sortedBy { it.order }) {
                val stepStartTime = System.currentTimeMillis()
                emit(PipelineProgress.StepStarted(step))

                try {
                    // Prepare arguments, potentially using previous step's output
                    val arguments = prepareArguments(step, previousOutput)

                    // Execute the tool
                    val toolResult = multiMcpClient.callTool(
                        serverUrl = step.serverUrl,
                        toolName = step.toolName,
                        arguments = arguments
                    )

                    if (toolResult.isSuccess) {
                        val result = toolResult.getOrThrow()
                        val output = extractOutput(result)

                        val stepResult = StepExecutionResult(
                            stepId = step.id,
                            stepName = step.name,
                            status = PipelineStep.StepStatus.COMPLETED,
                            input = arguments,
                            output = output,
                            error = null,
                            duration = System.currentTimeMillis() - stepStartTime
                        )

                        stepResults.add(stepResult)
                        previousOutput = output

                        emit(PipelineProgress.StepCompleted(step, stepResult))
                    } else {
                        val error = toolResult.exceptionOrNull()?.message ?: "Unknown error"

                        val stepResult = StepExecutionResult(
                            stepId = step.id,
                            stepName = step.name,
                            status = PipelineStep.StepStatus.FAILED,
                            input = arguments,
                            output = null,
                            error = error,
                            duration = System.currentTimeMillis() - stepStartTime
                        )

                        stepResults.add(stepResult)
                        emit(PipelineProgress.StepFailed(step, error))

                        // Fail the entire pipeline if a step fails
                        emit(PipelineProgress.Failed(config, error, stepResults))
                        return@flow
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing step ${step.name}", e)

                    val stepResult = StepExecutionResult(
                        stepId = step.id,
                        stepName = step.name,
                        status = PipelineStep.StepStatus.FAILED,
                        input = step.arguments,
                        output = null,
                        error = e.message,
                        duration = System.currentTimeMillis() - stepStartTime
                    )

                    stepResults.add(stepResult)
                    emit(PipelineProgress.Failed(config, e.message ?: "Unknown error", stepResults))
                    return@flow
                }
            }

            // Step 3: Pipeline completed successfully
            val executionResult = PipelineExecutionResult(
                pipelineId = config.id,
                status = PipelineConfig.PipelineStatus.COMPLETED,
                stepResults = stepResults,
                finalOutput = previousOutput,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )

            emit(PipelineProgress.Completed(config, executionResult))

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline execution failed", e)
            emit(PipelineProgress.Failed(config, e.message ?: "Unknown error", stepResults))
        }
    }

    /**
     * Prepare arguments for a step, potentially using previous step's output
     */
    private fun prepareArguments(
        step: PipelineStep,
        previousOutput: String?
    ): Map<String, Any> {
        val arguments = step.arguments.toMutableMap()

        // If this step has a placeholder for previous output, replace it
        if (previousOutput != null) {
            arguments.forEach { (key, value) ->
                if (value is String && value == "\${PREVIOUS_OUTPUT}") {
                    arguments[key] = previousOutput
                }
            }
        }

        return arguments
    }

    /**
     * Extract text output from CallToolResult
     */
    private fun extractOutput(result: CallToolResult): String {
        return result.content.firstOrNull()?.text ?: ""
    }

    /**
     * Represents progress updates during pipeline execution
     */
    sealed class PipelineProgress {
        data class Started(val config: PipelineConfig) : PipelineProgress()
        data class ConnectingToServers(val serverUrls: List<String>) : PipelineProgress()
        data class ServersConnected(val serverUrls: List<String>) : PipelineProgress()
        data class StepStarted(val step: PipelineStep) : PipelineProgress()
        data class StepCompleted(val step: PipelineStep, val result: StepExecutionResult) :
            PipelineProgress()

        data class StepFailed(val step: PipelineStep, val error: String) : PipelineProgress()
        data class Completed(val config: PipelineConfig, val result: PipelineExecutionResult) :
            PipelineProgress()

        data class Failed(
            val config: PipelineConfig,
            val error: String,
            val completedSteps: List<StepExecutionResult>
        ) : PipelineProgress()
    }
}
