package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model")
    val model: String = "claude-sonnet-4-5-20250929",

    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,

    @SerializedName("temperature")
    val temperature: Double = 1.0,

    @SerializedName("system")
    val system: String? = null,

    @SerializedName("messages")
    val messages: List<MessageDto>,

    @SerializedName("tools")
    val tools: List<ClaudeToolDto>? = null
)

data class MessageDto(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: Any
)

data class ClaudeToolDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("input_schema")
    val inputSchema: Map<String, Any>
)

data class ContentBlock(
    @SerializedName("type")
    val type: String,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("input")
    val input: Map<String, Any>? = null,

    @SerializedName("tool_use_id")
    val toolUseId: String? = null,

    @SerializedName("content")
    val content: Any? = null
)