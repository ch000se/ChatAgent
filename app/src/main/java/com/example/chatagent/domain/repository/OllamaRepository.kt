package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.OllamaModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface OllamaRepository {
    suspend fun sendMessage(message: String, conversationHistory: List<Message>): Result<Message>
    suspend fun listModels(): Result<List<OllamaModel>>
    suspend fun checkConnection(): Result<String>
    fun getSelectedModel(): StateFlow<String>
    fun setSelectedModel(model: String)
    fun getServerUrl(): StateFlow<String>
    fun setServerUrl(url: String)
    fun getAllMessages(): Flow<List<Message>>
    fun clearConversationHistory()
    fun getInferenceStats(): StateFlow<OllamaInferenceStats>
}

data class OllamaInferenceStats(
    val totalDurationMs: Long = 0,
    val promptEvalCount: Int = 0,
    val evalCount: Int = 0,
    val tokensPerSecond: Double = 0.0
)
