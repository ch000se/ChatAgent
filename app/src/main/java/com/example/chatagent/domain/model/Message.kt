package com.example.chatagent.domain.model

import com.google.gson.annotations.SerializedName

data class Message(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val jsonResponse: AgentJsonResponse? = null,
    val tokenUsage: TokenUsage? = null,
    val isSummary: Boolean = false,
    val summarizedMessageCount: Int? = null,
    val originalTokenCount: Int? = null,
    val sources: List<DocumentSearchResult>? = null
)

data class AgentJsonResponse(
    @SerializedName("answer")
    val answer: String,

    @SerializedName("confidence")
    val confidence: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("reasoning")
    val reasoning: String? = null,

    @SerializedName("sources")
    val sources: List<String>? = null
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val cacheCreationInputTokens: Int? = null,
    val cacheReadInputTokens: Int? = null
)