package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

// JSON-RPC 2.0 Request
data class JsonRpcRequest(
    @SerializedName("jsonrpc") val jsonrpc: String = "2.0",
    @SerializedName("id") val id: String,
    @SerializedName("method") val method: String,
    @SerializedName("params") val params: Any? = null
)

// JSON-RPC 2.0 Response
data class JsonRpcResponse<T>(
    @SerializedName("jsonrpc") val jsonrpc: String,
    @SerializedName("id") val id: String?,
    @SerializedName("result") val result: T?,
    @SerializedName("error") val error: JsonRpcError?
)

data class JsonRpcError(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Any?
)

// MCP Initialize Request
data class InitializeParams(
    @SerializedName("protocolVersion") val protocolVersion: String = "2024-11-05", // Используем версию, которую поддерживает сервер
    @SerializedName("capabilities") val capabilities: ClientCapabilities = ClientCapabilities(),
    @SerializedName("clientInfo") val clientInfo: ClientInfo
)

data class ClientCapabilities(
    @SerializedName("roots") val roots: RootsCapability? = RootsCapability(),
    @SerializedName("sampling") val sampling: Map<String, Any>? = emptyMap()
)

data class RootsCapability(
    @SerializedName("listChanged") val listChanged: Boolean = true
)

data class ClientInfo(
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String
)

// MCP Initialize Result
data class InitializeResult(
    @SerializedName("protocolVersion") val protocolVersion: String,
    @SerializedName("capabilities") val capabilities: ServerCapabilities,
    @SerializedName("serverInfo") val serverInfo: ServerInfo
)

data class ServerCapabilities(
    @SerializedName("tools") val tools: ToolsCapability? = null,
    @SerializedName("prompts") val prompts: PromptsCapability? = null,
    @SerializedName("resources") val resources: ResourcesCapability? = null
)

data class ToolsCapability(
    @SerializedName("listChanged") val listChanged: Boolean = false
)

data class PromptsCapability(
    @SerializedName("listChanged") val listChanged: Boolean = false
)

data class ResourcesCapability(
    @SerializedName("listChanged") val listChanged: Boolean = false
)

data class ServerInfo(
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String
)

// MCP Tools List Request
data class ListToolsParams(
    @SerializedName("cursor") val cursor: String? = null
)

// MCP Tools List Result
data class ListToolsResult(
    @SerializedName("tools") val tools: List<McpTool>,
    @SerializedName("nextCursor") val nextCursor: String? = null
)

data class McpTool(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("inputSchema") val inputSchema: ToolInputSchema
)

data class ToolInputSchema(
    @SerializedName("type") val type: String,
    @SerializedName("properties") val properties: Map<String, Any>?,
    @SerializedName("required") val required: List<String>?
)

// MCP Tool Call Request
data class CallToolParams(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: Map<String, Any>? = null
)

// MCP Tool Call Result
data class CallToolResult(
    @SerializedName("content") val content: List<ToolContent>,
    @SerializedName("isError") val isError: Boolean? = false
)

data class ToolContent(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String?,
    @SerializedName("data") val data: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null
)