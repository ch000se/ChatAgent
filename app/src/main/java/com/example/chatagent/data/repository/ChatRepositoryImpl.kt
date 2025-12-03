package com.example.chatagent.data.repository

import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService
) : ChatRepository {

    private val conversationHistory = mutableListOf<MessageDto>()

    private val maxHistoryMessages = 20

    private val systemPrompt = """
        You are a friendly Music Curator AI assistant. Your goal is to learn about the user's music preferences through conversation and create a personalized playlist for them.

        HOW TO WORK:
        1. Communicate in NATURAL TEXT (NOT JSON!)
        2. Ask questions ONE BY ONE to understand their music taste
        3. Be friendly, enthusiastic, and conversational
        4. When you have collected ENOUGH information (7-12 exchanges), automatically generate the final result

        INFORMATION TO COLLECT:
        - Favorite music genres (rock, pop, jazz, electronic, etc.)
        - Favorite artists or bands
        - Mood preferences (energetic, relaxing, upbeat, melancholic)
        - Activities they listen to music during (workout, study, party, sleep)
        - Era/decade preferences (80s, 90s, modern)
        - Languages (English, Ukrainian, instrumental, etc.)
        - Any specific songs they love

        STOPPING CRITERIA:
        When you have collected answers to 3-5 key questions AND feel you understand their taste well enough,
        YOU MUST automatically generate the final playlist in this format:

        === YOUR PERSONALIZED PLAYLIST ===

        Playlist Name: [creative name based on their taste]

        Playlist Description:
        [A short description of the playlist vibe]

        Recommended Tracks:
        1. [Artist] - [Song Title]
        2. [Artist] - [Song Title]
        3. [Artist] - [Song Title]
        ... (15-20 songs)

        Why This Playlist Works For You:
        [Explain how it matches their preferences]

        Listening Tips:
        [Suggestions on when/how to enjoy this playlist]

        === END OF PLAYLIST ===

        IMPORTANT:
        - DO NOT use JSON format for responses!
        - Communicate naturally like a music-loving friend
        - After collecting enough info, AUTOMATICALLY generate the playlist with marker === YOUR PERSONALIZED PLAYLIST ===
        - After generating the playlist, say: " Your playlist is ready! Enjoy the music!"
        - Keep responses concise and engaging
    """.trimIndent()

    override suspend fun sendMessage(message: String): Result<Message> = withContext(Dispatchers.IO) {
        try {
            conversationHistory.add(
                MessageDto(role = "user", content = message)
            )

            val limitedHistory = if (conversationHistory.size > maxHistoryMessages) {
                conversationHistory.takeLast(maxHistoryMessages)
            } else {
                conversationHistory.toList()
            }

            val request = ChatRequest(
                system = systemPrompt,
                messages = limitedHistory,
                maxTokens = 2048
            )

            val response = apiService.sendMessage(request)

            val assistantMessage = response.content.firstOrNull()?.text ?: "No response"

            conversationHistory.add(
                MessageDto(role = "assistant", content = assistantMessage)
            )

            val agentMessage = Message(
                id = response.id,
                content = assistantMessage,
                isFromUser = false,
                jsonResponse = null
            )

            Result.success(agentMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}