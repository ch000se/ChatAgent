package com.example.chatagent.presentation.chat

import com.example.chatagent.domain.model.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val currentTemperature: Double = 1.0,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalTokens: Int = 0,
    val summarizationEnabled: Boolean = true,
    val totalSummarizations: Int = 0,
    val tokensSaved: Int = 0,
    val compressionRatio: Double = 0.0
)