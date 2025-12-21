package com.example.chatagent.data.remote.client

import android.util.Log
import com.example.chatagent.data.remote.api.McpApiService
import com.example.chatagent.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for managing multiple MCP server connections simultaneously
 */
@Singleton
class MultiMcpClient @Inject constructor(
    private val mcpApiService: McpApiService,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "MultiMcpClient"
    }

    private val connections = mutableMapOf<String, ServerConnection>()

    private val _serverStates = MutableStateFlow<Map<String, ServerState>>(emptyMap())
    val serverStates: StateFlow<Map<String, ServerState>> = _serverStates.asStateFlow()

    data class ServerConnection(
        val url: String,
        val sessionId: String?,
        val serverInfo: ServerInfo,
        val tools: List<McpTool> = emptyList()
    )

    data class ServerState(
        val url: String,
        val status: ConnectionStatus,
        val serverInfo: ServerInfo? = null,
        val tools: List<McpTool> = emptyList(),
        val error: String? = null
    ) {
        enum class ConnectionStatus {
            DISCONNECTED,
            CONNECTING,
            CONNECTED,
            ERROR
        }
    }

    /**
     * Connect to a MCP server
     */
    suspend fun connectToServer(serverUrl: String): Result<ServerInfo> {
        return try {
            updateServerState(serverUrl, ServerState.ConnectionStatus.CONNECTING)
            Log.d(TAG, "Connecting to server: $serverUrl")

            val initRequest = JsonRpcRequest(
                id = UUID.randomUUID().toString(),
                method = "initialize",
                params = InitializeParams(
                    clientInfo = ClientInfo(
                        name = "ChatAgent-Pipeline",
                        version = "1.0.0"
                    )
                )
            )

            val response = mcpApiService.sendRequest(serverUrl, initRequest)

            if (response.isSuccessful && response.body() != null) {
                val rpcResponse = response.body()!!

                if (rpcResponse.error != null) {
                    val errorMsg = "MCP Error: ${rpcResponse.error.message}"
                    updateServerState(serverUrl, ServerState.ConnectionStatus.ERROR, error = errorMsg)
                    return Result.failure(Exception(errorMsg))
                }

                val sessionId = response.headers()["mcp-session-id"]
                val initResult = gson.fromJson(
                    gson.toJson(rpcResponse.result),
                    InitializeResult::class.java
                )

                val connection = ServerConnection(
                    url = serverUrl,
                    sessionId = sessionId,
                    serverInfo = initResult.serverInfo
                )

                connections[serverUrl] = connection
                updateServerState(
                    serverUrl,
                    ServerState.ConnectionStatus.CONNECTED,
                    serverInfo = initResult.serverInfo
                )

                // Automatically list tools after connection
                listToolsForServer(serverUrl)

                Log.d(TAG, "Connected to ${initResult.serverInfo.name}")
                Result.success(initResult.serverInfo)
            } else {
                val errorMsg = "Connection failed: ${response.message()}"
                updateServerState(serverUrl, ServerState.ConnectionStatus.ERROR, error = errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Connection error: ${e.message}"
            updateServerState(serverUrl, ServerState.ConnectionStatus.ERROR, error = errorMsg)
            Result.failure(e)
        }
    }

    /**
     * List tools available on a specific server
     */
    suspend fun listToolsForServer(serverUrl: String): Result<List<McpTool>> {
        val connection = connections[serverUrl]
            ?: return Result.failure(Exception("Not connected to server: $serverUrl"))

        return try {
            val request = JsonRpcRequest(
                id = UUID.randomUUID().toString(),
                method = "tools/list",
                params = ListToolsParams()
            )

            val response = if (connection.sessionId != null) {
                mcpApiService.sendRequestWithSession(serverUrl, connection.sessionId, request)
            } else {
                mcpApiService.sendRequest(serverUrl, request)
            }

            if (response.isSuccessful && response.body() != null) {
                val rpcResponse = response.body()!!

                if (rpcResponse.error != null) {
                    return Result.failure(Exception("MCP Error: ${rpcResponse.error.message}"))
                }

                val toolsResult = gson.fromJson(
                    gson.toJson(rpcResponse.result),
                    ListToolsResult::class.java
                )

                // Update connection with tools
                connections[serverUrl] = connection.copy(tools = toolsResult.tools)
                updateServerState(
                    serverUrl,
                    ServerState.ConnectionStatus.CONNECTED,
                    serverInfo = connection.serverInfo,
                    tools = toolsResult.tools
                )

                Log.d(TAG, "Retrieved ${toolsResult.tools.size} tools from $serverUrl")
                Result.success(toolsResult.tools)
            } else {
                Result.failure(Exception("Failed to get tools: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing tools for $serverUrl", e)
            Result.failure(e)
        }
    }

    /**
     * Call a tool on a specific server
     */
    suspend fun callTool(
        serverUrl: String,
        toolName: String,
        arguments: Map<String, Any>
    ): Result<CallToolResult> {
        val connection = connections[serverUrl]
            ?: return Result.failure(Exception("Not connected to server: $serverUrl"))

        return try {
            Log.d(TAG, "Calling tool '$toolName' on $serverUrl with args: $arguments")

            val request = JsonRpcRequest(
                id = UUID.randomUUID().toString(),
                method = "tools/call",
                params = CallToolParams(
                    name = toolName,
                    arguments = arguments
                )
            )

            val response = if (connection.sessionId != null) {
                mcpApiService.sendRequestWithSession(serverUrl, connection.sessionId, request)
            } else {
                mcpApiService.sendRequest(serverUrl, request)
            }

            if (response.isSuccessful && response.body() != null) {
                val rpcResponse = response.body()!!

                if (rpcResponse.error != null) {
                    return Result.failure(Exception("MCP Error: ${rpcResponse.error.message}"))
                }

                val toolResult = gson.fromJson(
                    gson.toJson(rpcResponse.result),
                    CallToolResult::class.java
                )

                Log.d(TAG, "Tool call successful")
                Result.success(toolResult)
            } else {
                Result.failure(Exception("Tool call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling tool on $serverUrl", e)
            Result.failure(e)
        }
    }

    /**
     * Disconnect from a specific server
     */
    fun disconnectServer(serverUrl: String) {
        connections.remove(serverUrl)
        updateServerState(serverUrl, ServerState.ConnectionStatus.DISCONNECTED)
        Log.d(TAG, "Disconnected from $serverUrl")
    }

    /**
     * Disconnect from all servers
     */
    fun disconnectAll() {
        connections.keys.toList().forEach { disconnectServer(it) }
    }

    /**
     * Get connection info for a server
     */
    fun getServerConnection(serverUrl: String): ServerConnection? {
        return connections[serverUrl]
    }

    /**
     * Get all connected servers
     */
    fun getConnectedServers(): List<String> {
        return connections.keys.toList()
    }

    /**
     * Check if connected to a server
     */
    fun isConnectedTo(serverUrl: String): Boolean {
        return connections.containsKey(serverUrl)
    }

    private fun updateServerState(
        url: String,
        status: ServerState.ConnectionStatus,
        serverInfo: ServerInfo? = null,
        tools: List<McpTool> = emptyList(),
        error: String? = null
    ) {
        val currentStates = _serverStates.value.toMutableMap()
        currentStates[url] = ServerState(
            url = url,
            status = status,
            serverInfo = serverInfo,
            tools = tools,
            error = error
        )
        _serverStates.value = currentStates
    }
}
