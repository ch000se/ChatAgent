package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.Message
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    suspend fun sendMessage(message: String): Result<Message>
    fun setSystemPrompt(prompt: String)
    fun getSystemPrompt(): StateFlow<String>
    fun clearConversationHistory()
}