package com.example.chatagent.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.data.remote.dto.McpTool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class McpViewModel @Inject constructor(
    private val mcpClient: McpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(McpUiState())
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mcpClient.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        viewModelScope.launch {
            mcpClient.tools.collect { tools ->
                _uiState.update { it.copy(tools = tools) }
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = mcpClient.connect(_uiState.value.serverUrl)

            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    listTools()
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Connection failed"
                        )
                    }
                }
            )
        }
    }

    fun disconnect() {
        mcpClient.disconnect()
        _uiState.update { it.copy(tools = emptyList(), errorMessage = null, selectedTool = null, lastToolResult = null) }
    }

    fun listTools() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = mcpClient.listTools()

            result.fold(
                onSuccess = { tools ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            tools = tools,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to fetch tools"
                        )
                    }
                }
            )
        }
    }

    fun callTool(toolName: String, arguments: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingTool = true, errorMessage = null) }

            val result = mcpClient.callTool(toolName, arguments)

            result.fold(
                onSuccess = { callResult ->
                    _uiState.update {
                        it.copy(
                            isExecutingTool = false,
                            lastToolResult = callResult,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isExecutingTool = false,
                            errorMessage = "Tool execution failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectTool(tool: McpTool?) {
        _uiState.update { it.copy(selectedTool = tool) }
    }

    fun updateToolArguments(arguments: Map<String, Any>) {
        _uiState.update { it.copy(toolArguments = arguments) }
    }

    fun clearToolResult() {
        _uiState.update { it.copy(lastToolResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
