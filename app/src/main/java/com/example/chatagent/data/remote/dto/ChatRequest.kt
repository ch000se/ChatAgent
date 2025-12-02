package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model")
    val model: String = "claude-sonnet-4-5-20250929",

    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,

    @SerializedName("messages")
    val messages: List<MessageDto>
)

data class MessageDto(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
)