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

@Singleton
class McpClient @Inject constructor(
    private val mcpApiService: McpApiService,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "McpClient"
    }

    private var serverUrl: String? = null
    private var isInitialized = false
    private var sessionId: String? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _tools = MutableStateFlow<List<McpTool>>(emptyList())
    val tools: StateFlow<List<McpTool>> = _tools.asStateFlow()

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val serverInfo: ServerInfo) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    suspend fun connect(url: String): Result<InitializeResult> {
        return try {
            _connectionState.value = ConnectionState.Connecting
            serverUrl = url

            Log.d(TAG, "Connecting to MCP server: $url")

            val initRequest = JsonRpcRequest(
                id = UUID.randomUUID().toString(),
                method = "initialize",
                params = InitializeParams(
                    clientInfo = ClientInfo(
                        name = "ChatAgent",
                        version = "1.0.0"
                    )
                )
            )

            val response = mcpApiService.sendRequest(url, initRequest)

            if (response.isSuccessful && response.body() != null) {
                val rpcResponse = response.body()!!

                if (rpcResponse.error != null) {
                    val errorMsg = "MCP Error: ${rpcResponse.error.message}"
                    Log.e(TAG, errorMsg)
                    _connectionState.value = ConnectionState.Error(errorMsg)
                    return Result.failure(Exception(errorMsg))
                }

                // Extract session ID from headers if present
                sessionId = response.headers()["mcp-session-id"]
                if (sessionId != null) {
                    Log.d(TAG, "Session ID: $sessionId")
                }

                val initResult = gson.fromJson(
                    gson.toJson(rpcResponse.result),
                    InitializeResult::class.java
                )

                isInitialized = true
                _connectionState.value = ConnectionState.Connected(initResult.serverInfo)
                Log.d(TAG, "Connected to ${initResult.serverInfo.name}")

                Result.success(initResult)
            } else {
                val errorMsg = "Connection failed: ${response.message()}"
                Log.e(TAG, errorMsg)
                _connectionState.value = ConnectionState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Connection error: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun listTools(): Result<List<McpTool>> {
        if (!isInitialized || serverUrl == null) {
            return Result.failure(Exception("Not connected to MCP server"))
        }

        return try {
            Log.d(TAG, "Requesting tools list")

            val request = JsonRpcRequest(
                id = UUID.randomUUID().toString(),
                method = "tools/list",
                params = ListToolsParams()
            )

            val response = if (sessionId != null) {
                mcpApiService.sendRequestWithSession(serverUrl!!, sessionId!!, request)
            } else {
                mcpApiService.sendRequest(serverUrl!!, request)
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

                _tools.value = toolsResult.tools
                Log.d(TAG, "Retrieved ${toolsResult.tools.size} tools")
                Result.success(toolsResult.tools)
            } else {
                Result.failure(Exception("Failed to get tools: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing tools", e)
            Result.failure(e)
        }
    }

    suspend fun callTool(toolName: String, arguments: Map<String, Any>): Result<CallToolResult> {
        if (!isInitialized || serverUrl == null) {
            return Result.failure(Exception("Not connected to MCP server"))
        }

        return try {
            Log.d(TAG, "Calling tool: $toolName with args: $arguments")

            val request = JsonRpcRequest(
                id = UUID.randomUUID().toString(),
                method = "tools/call",
                params = CallToolParams(
                    name = toolName,
                    arguments = arguments
                )
            )

            val response = if (sessionId != null) {
                mcpApiService.sendRequestWithSession(serverUrl!!, sessionId!!, request)
            } else {
                mcpApiService.sendRequest(serverUrl!!, request)
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
            Log.e(TAG, "Error calling tool", e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        isInitialized = false
        serverUrl = null
        sessionId = null
        _connectionState.value = ConnectionState.Disconnected
        _tools.value = emptyList()
        Log.d(TAG, "Disconnected from MCP server")
    }
}
