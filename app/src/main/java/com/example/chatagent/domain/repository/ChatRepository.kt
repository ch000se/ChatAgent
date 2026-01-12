package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.SummarizationConfig
import com.example.chatagent.domain.model.SummarizationStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    suspend fun sendMessage(message: String): Result<Message>
    fun getAllMessages(): Flow<List<Message>>
    fun setSystemPrompt(prompt: String)
    fun getSystemPrompt(): StateFlow<String>
    fun clearConversationHistory()
    fun setTemperature(temperature: Double)
    fun getTemperature(): StateFlow<Double>
    fun setSummarizationConfig(config: SummarizationConfig)
    fun getSummarizationConfig(): StateFlow<SummarizationConfig>
    fun getSummarizationStats(): StateFlow<SummarizationStats>
}