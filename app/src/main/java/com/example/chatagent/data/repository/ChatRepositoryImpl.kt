package com.example.chatagent.data.repository

import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.AgentJsonResponse
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.repository.ChatRepository
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService
) : ChatRepository {

    private val conversationHistory = mutableListOf<MessageDto>()
    private val gson = Gson()

    private val systemPrompt = """
        You are an AI assistant that ALWAYS responds in JSON format.

        CRITICAL RULES:
        - Your response MUST be ONLY valid JSON
        - DO NOT wrap JSON in markdown code blocks (no ```json or ```)
        - DO NOT include any text before or after the JSON
        - Output raw JSON directly

        Response format:
        {
          "answer": "Your detailed answer here",
          "confidence": "high|medium|low",
          "category": "question_category",
          "reasoning": "Brief explanation of your answer",
          "sources": ["source1", "source2"]
        }

        Example 1:
        Question: "What is the capital of Ukraine?"
        Response:
        {
          "answer": "The capital of Ukraine is Kyiv (also spelled Kiev). It is the largest city in Ukraine and serves as the country's political, economic, and cultural center.",
          "confidence": "high",
          "category": "geography",
          "reasoning": "This is a well-established geographical fact.",
          "sources": ["general_knowledge"]
        }

        Example 2:
        Question: "How to make borscht?"
        Response:
        {
          "answer": "Borscht is a traditional Ukrainian soup. Main ingredients: beets, cabbage, potatoes, carrots, onions, tomato paste. Start by making a meat broth, then add vegetables sequentially. Add beets last to preserve the color.",
          "confidence": "high",
          "category": "cooking",
          "reasoning": "Traditional recipe with standard ingredients and cooking method.",
          "sources": ["culinary_knowledge"]
        }

        Always follow this exact format!
    """.trimIndent()

    override suspend fun sendMessage(message: String): Result<Message> {
        return try {
            conversationHistory.add(
                MessageDto(role = "user", content = message)
            )

            val request = ChatRequest(
                system = systemPrompt,
                messages = conversationHistory.toList()
            )

            val response = apiService.sendMessage(request)

            val assistantMessage = response.content.firstOrNull()?.text ?: "No response"

            // Очистити JSON від можливих markdown блоків
            val cleanedJson = assistantMessage
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            conversationHistory.add(
                MessageDto(role = "assistant", content = cleanedJson)
            )

            // Спроба парсити JSON відповідь
            val jsonResponse = try {
                gson.fromJson(cleanedJson, AgentJsonResponse::class.java)
            } catch (e: Exception) {
                null
            }

            val agentMessage = Message(
                id = response.id,
                content = cleanedJson,
                isFromUser = false,
                jsonResponse = jsonResponse
            )

            Result.success(agentMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}