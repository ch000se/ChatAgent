package com.example.chatagent.data.repository

import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.repository.ChatRepository
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService
) : ChatRepository {

    private val conversationHistory = mutableListOf<MessageDto>()

    override suspend fun sendMessage(message: String): Result<Message> {
        return try {
            conversationHistory.add(
                MessageDto(role = "user", content = message)
            )

            val request = ChatRequest(
                messages = conversationHistory.toList()
            )

            val response = apiService.sendMessage(request)

            val assistantMessage = response.content.firstOrNull()?.text ?: "No response"

            conversationHistory.add(
                MessageDto(role = "assistant", content = assistantMessage)
            )

            val agentMessage = Message(
                id = response.id,
                content = assistantMessage,
                isFromUser = false
            )

            Result.success(agentMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}