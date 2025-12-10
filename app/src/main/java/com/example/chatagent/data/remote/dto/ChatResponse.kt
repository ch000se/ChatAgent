package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: List<ContentDto>,

    @SerializedName("model")
    val model: String,

    @SerializedName("stop_reason")
    val stopReason: String? = null,

    @SerializedName("usage")
    val usage: UsageDto? = null
)

data class ContentDto(
    @SerializedName("type")
    val type: String,

    @SerializedName("text")
    val text: String
)

data class UsageDto(
    @SerializedName("input_tokens")
    val inputTokens: Int,

    @SerializedName("output_tokens")
    val outputTokens: Int,

    @SerializedName("cache_creation_input_tokens")
    val cacheCreationInputTokens: Int? = null,

    @SerializedName("cache_read_input_tokens")
    val cacheReadInputTokens: Int? = null
)