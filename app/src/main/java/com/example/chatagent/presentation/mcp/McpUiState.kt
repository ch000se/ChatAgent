package com.example.chatagent.presentation.mcp

import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.data.remote.dto.CallToolResult
import com.example.chatagent.data.remote.dto.McpTool

data class McpUiState(
    val serverUrl: String = "https://mcp.kiwi.com",
    val connectionState: McpClient.ConnectionState = McpClient.ConnectionState.Disconnected,
    val tools: List<McpTool> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTool: McpTool? = null,
    val toolArguments: Map<String, Any> = emptyMap(),
    val isExecutingTool: Boolean = false,
    val lastToolResult: CallToolResult? = null
)
