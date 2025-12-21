package com.example.chatagent.presentation.pipeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.data.remote.client.MultiMcpClient
import com.example.chatagent.domain.model.*
import com.example.chatagent.domain.usecase.ExecutePipelineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val executePipelineUseCase: ExecutePipelineUseCase,
    private val multiMcpClient: MultiMcpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PipelineUiState())
    val uiState: StateFlow<PipelineUiState> = _uiState.asStateFlow()

    init {
        // Initialize with demo pipelines
        loadDemoPipelines()

        // Observe server states
        viewModelScope.launch {
            multiMcpClient.serverStates.collect { serverStates ->
                _uiState.update { it.copy(serverStates = serverStates) }
            }
        }
    }

    /**
     * Execute a pipeline
     */
    fun executePipeline(config: PipelineConfig) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExecuting = true,
                    currentPipeline = config,
                    executionProgress = emptyList(),
                    executionResult = null,
                    error = null
                )
            }

            executePipelineUseCase.execute(config).collect { progress ->
                when (progress) {
                    is ExecutePipelineUseCase.PipelineProgress.Started -> {
                        addProgressMessage("Pipeline started: ${config.name}")
                    }

                    is ExecutePipelineUseCase.PipelineProgress.ConnectingToServers -> {
                        addProgressMessage("Connecting to ${progress.serverUrls.size} MCP server(s)...")
                    }

                    is ExecutePipelineUseCase.PipelineProgress.ServersConnected -> {
                        addProgressMessage("Connected to all servers")
                    }

                    is ExecutePipelineUseCase.PipelineProgress.StepStarted -> {
                        addProgressMessage("Executing: ${progress.step.name}")
                    }

                    is ExecutePipelineUseCase.PipelineProgress.StepCompleted -> {
                        addProgressMessage("✓ ${progress.step.name} completed")
                        addProgressMessage("  Output: ${progress.result.output?.take(100)}...")
                    }

                    is ExecutePipelineUseCase.PipelineProgress.StepFailed -> {
                        addProgressMessage("✗ ${progress.step.name} failed: ${progress.error}")
                    }

                    is ExecutePipelineUseCase.PipelineProgress.Completed -> {
                        addProgressMessage("Pipeline completed successfully!")
                        _uiState.update {
                            it.copy(
                                isExecuting = false,
                                executionResult = progress.result
                            )
                        }
                    }

                    is ExecutePipelineUseCase.PipelineProgress.Failed -> {
                        addProgressMessage("Pipeline failed: ${progress.error}")
                        _uiState.update {
                            it.copy(
                                isExecuting = false,
                                error = progress.error
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Select a pipeline for execution
     */
    fun selectPipeline(pipeline: PipelineConfig?) {
        _uiState.update { it.copy(selectedPipeline = pipeline) }
    }

    /**
     * Clear execution results
     */
    fun clearResults() {
        _uiState.update {
            it.copy(
                executionProgress = emptyList(),
                executionResult = null,
                error = null,
                currentPipeline = null
            )
        }
    }

    /**
     * Update custom search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(customSearchQuery = query) }
    }

    /**
     * Execute pipeline with custom search query
     */
    fun executeWithCustomQuery() {
        val query = _uiState.value.customSearchQuery
        if (query.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a search query") }
            return
        }

        // Create pipeline with custom query
        val customPipeline = createSearchAndSavePipeline(query)
        executePipeline(customPipeline)
    }

    /**
     * Disconnect from all servers
     */
    fun disconnectAll() {
        multiMcpClient.disconnectAll()
    }

    private fun addProgressMessage(message: String) {
        _uiState.update {
            it.copy(
                executionProgress = it.executionProgress + ProgressMessage(
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Load demo pipeline configurations
     */
    private fun loadDemoPipelines() {
        val demoPipelines = listOf(
            createSearchAndSavePipeline(),
            createWebScraperPipeline()
        )
        _uiState.update { it.copy(availablePipelines = demoPipelines) }
    }

    /**
     * Demo: Search web, summarize, and save to file
     */
    private fun createSearchAndSavePipeline(customQuery: String? = null): PipelineConfig {
        val searchQuery = customQuery ?: "latest AI developments 2024"

        return PipelineConfig(
            name = if (customQuery != null) "Custom Search: $searchQuery" else "Web Search & Save",
            description = "Search the web, create a summary, and save to file",
            steps = listOf(
                PipelineStep(
                    name = "Search Web",
                    description = "Search for articles using Real Web Search",
                    serverUrl = "http://10.0.2.2:3000",  // Real Web Search MCP
                    toolName = "brave_web_search",
                    arguments = mapOf(
                        "query" to searchQuery,
                        "count" to 3
                    ),
                    order = 1
                ),
                PipelineStep(
                    name = "Create Summary",
                    description = "Summarize search results",
                    serverUrl = "http://10.0.2.2:3000",
                    toolName = "summarize",
                    arguments = mapOf(
                        "text" to "\${PREVIOUS_OUTPUT}",
                        "max_length" to 500
                    ),
                    order = 2
                ),
                PipelineStep(
                    name = "Save to File",
                    description = "Save summary to a file",
                    serverUrl = "http://10.0.2.2:3001",  // File System MCP
                    toolName = "write_file",
                    arguments = mapOf(
                        "path" to "/sdcard/Download/ai_summary.txt",
                        "content" to "\${PREVIOUS_OUTPUT}"
                    ),
                    order = 3
                )
            )
        )
    }

    /**
     * Demo: Advanced web scraper pipeline
     */
    private fun createWebScraperPipeline(): PipelineConfig {
        return PipelineConfig(
            name = "Web Scraper Pipeline",
            description = "Fetch web content, extract data, and export to JSON",
            steps = listOf(
                PipelineStep(
                    name = "Fetch Web Page",
                    description = "Fetch content from a URL",
                    serverUrl = "http://10.0.2.2:3002",
                    toolName = "fetch_url",
                    arguments = mapOf(
                        "url" to "https://example.com"
                    ),
                    order = 1
                ),
                PipelineStep(
                    name = "Extract Data",
                    description = "Extract structured data from HTML",
                    serverUrl = "http://10.0.2.2:3002",
                    toolName = "extract_data",
                    arguments = mapOf(
                        "html" to "\${PREVIOUS_OUTPUT}",
                        "selector" to "article"
                    ),
                    order = 2
                ),
                PipelineStep(
                    name = "Export to JSON",
                    description = "Save extracted data as JSON",
                    serverUrl = "http://10.0.2.2:3001",
                    toolName = "write_file",
                    arguments = mapOf(
                        "path" to "/sdcard/Download/scraped_data.json",
                        "content" to "\${PREVIOUS_OUTPUT}"
                    ),
                    order = 3
                )
            )
        )
    }
}

data class PipelineUiState(
    val availablePipelines: List<PipelineConfig> = emptyList(),
    val selectedPipeline: PipelineConfig? = null,
    val currentPipeline: PipelineConfig? = null,
    val isExecuting: Boolean = false,
    val executionProgress: List<ProgressMessage> = emptyList(),
    val executionResult: PipelineExecutionResult? = null,
    val serverStates: Map<String, MultiMcpClient.ServerState> = emptyMap(),
    val error: String? = null,
    val customSearchQuery: String = "latest AI developments 2024"
)

data class ProgressMessage(
    val message: String,
    val timestamp: Long
)
