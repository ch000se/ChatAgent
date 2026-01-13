package com.example.chatagent.data.repository

import android.util.Log
import com.example.chatagent.data.local.dao.MessageDao
import com.example.chatagent.data.mapper.toDomain
import com.example.chatagent.data.mapper.toEntity
import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.ClaudeToolDto
import com.example.chatagent.data.remote.dto.ContentBlock
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.SummarizationConfig
import com.example.chatagent.domain.model.SummarizationStats
import com.example.chatagent.domain.model.TokenUsage
import com.example.chatagent.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService,
    private val messageDao: MessageDao,
    private val mcpClient: McpClient
) : ChatRepository {

    private val TAG = "ChatRepositoryImpl"

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val conversationHistory = mutableListOf<MessageDto>()

    init {
        // Load messages from database on initialization
        repositoryScope.launch {
            loadMessagesFromDatabase()
        }
    }

    private val maxHistoryMessages = 20

    private val _summarizationConfig = MutableStateFlow(SummarizationConfig())
    private val _summarizationStats = MutableStateFlow(SummarizationStats())

    private val defaultSystemPrompt = """
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

    private val _currentSystemPrompt = MutableStateFlow(defaultSystemPrompt)
    private val _currentTemperature = MutableStateFlow(1.0)

    override fun setSystemPrompt(prompt: String) {
        _currentSystemPrompt.value = prompt
    }

    override fun getSystemPrompt(): StateFlow<String> = _currentSystemPrompt.asStateFlow()

    override fun setTemperature(temperature: Double) {
        _currentTemperature.value = temperature
    }

    override fun getTemperature(): StateFlow<Double> = _currentTemperature.asStateFlow()

    override fun setSummarizationConfig(config: SummarizationConfig) {
        _summarizationConfig.value = config
    }

    override fun getSummarizationConfig(): StateFlow<SummarizationConfig> =
        _summarizationConfig.asStateFlow()

    override fun getSummarizationStats(): StateFlow<SummarizationStats> =
        _summarizationStats.asStateFlow()

    private suspend fun loadMessagesFromDatabase() {
        val messages = messageDao.getAllMessages().first()
        conversationHistory.clear()
        conversationHistory.addAll(messages.map { entity ->
            MessageDto(
                role = if (entity.isFromUser) "user" else "assistant",
                content = entity.content
            )
        })
    }

    override fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun clearConversationHistory() {
        conversationHistory.clear()
        repositoryScope.launch {
            messageDao.deleteAllMessages()
        }
    }

    private suspend fun getAvailableMcpTools(): List<ClaudeToolDto> {
        return try {
            val connectionState = mcpClient.connectionState.value
            if (connectionState !is McpClient.ConnectionState.Connected) {
                return emptyList()
            }

            mcpClient.tools.value.map { mcpTool ->
                ClaudeToolDto(
                    name = mcpTool.name,
                    description = mcpTool.description ?: "No description",
                    inputSchema = mcpTool.inputSchema.toMap()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get MCP tools", e)
            emptyList()
        }
    }

    private fun com.example.chatagent.data.remote.dto.ToolInputSchema.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>("type" to this.type)
        this.properties?.let { map["properties"] = it }
        this.required?.let { map["required"] = it }
        return map
    }

    private fun contentToString(content: Any): String {
        return when (content) {
            is String -> content
            is List<*> -> content.joinToString("\n") { it.toString() }
            else -> content.toString()
        }
    }

    override suspend fun sendMessage(message: String): Result<Message> = withContext(Dispatchers.IO) {
        try {
            // Check if we should compress before adding new message
            if (shouldSummarize()) {
                compressConversationHistory()
            }

            // Save user message to database
            val userMessageEntity = Message(
                id = UUID.randomUUID().toString(),
                content = message,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(userMessageEntity.toEntity())

            conversationHistory.add(
                MessageDto(role = "user", content = message)
            )

            // Get available MCP tools
            val mcpTools = getAvailableMcpTools()
            Log.d(TAG, "Available MCP tools: ${mcpTools.size}")

            var finalResponse: Message? = null
            var continueLoop = true
            var loopCount = 0
            val maxLoops = 5

            while (continueLoop && loopCount < maxLoops) {
                loopCount++

                val limitedHistory = if (conversationHistory.size > maxHistoryMessages) {
                    conversationHistory.takeLast(maxHistoryMessages)
                } else {
                    conversationHistory.toList()
                }

                val request = ChatRequest(
                    // system = _currentSystemPrompt.value,
                    system = null,
                    messages = limitedHistory,
                    maxTokens = 2048,
                    temperature = _currentTemperature.value,
                    tools = if (mcpTools.isNotEmpty()) mcpTools else null
                )

                val response = apiService.sendMessage(request)

                // Check if response contains tool use
                val toolUseBlocks = response.content.filter { it.type == "tool_use" }

                if (toolUseBlocks.isNotEmpty()) {
                    Log.d(TAG, "Found ${toolUseBlocks.size} tool use blocks")

                    // Add assistant's tool use response to history
                    conversationHistory.add(
                        MessageDto(role = "assistant", content = response.content)
                    )

                    // Execute tools and collect results
                    val toolResults = mutableListOf<ContentBlock>()
                    toolUseBlocks.forEach { toolUse ->
                        val toolName = toolUse.name ?: return@forEach
                        val toolInput = toolUse.input ?: emptyMap()
                        val toolUseId = toolUse.id ?: return@forEach

                        Log.d(TAG, "Executing tool: $toolName with input: $toolInput")

                        val result = mcpClient.callTool(toolName, toolInput)
                        result.fold(
                            onSuccess = { callResult ->
                                val resultText = callResult.content.joinToString("\n") {
                                    it.text ?: ""
                                }
                                toolResults.add(
                                    ContentBlock(
                                        type = "tool_result",
                                        toolUseId = toolUseId,
                                        content = resultText
                                    )
                                )
                                Log.d(TAG, "Tool $toolName executed successfully")
                            },
                            onFailure = { error ->
                                toolResults.add(
                                    ContentBlock(
                                        type = "tool_result",
                                        toolUseId = toolUseId,
                                        content = "Error: ${error.message}"
                                    )
                                )
                                Log.e(TAG, "Tool $toolName failed", error)
                            }
                        )
                    }

                    // Add tool results to conversation
                    conversationHistory.add(
                        MessageDto(role = "user", content = toolResults)
                    )

                    // Continue loop to get final response from Claude
                } else {
                    // No tool use, this is the final response
                    val assistantMessage = response.content.firstOrNull()?.text ?: "No response"

                    conversationHistory.add(
                        MessageDto(role = "assistant", content = assistantMessage)
                    )

                    val tokenUsage = response.usage?.let { usage ->
                        TokenUsage(
                            inputTokens = usage.inputTokens,
                            outputTokens = usage.outputTokens,
                            totalTokens = usage.inputTokens + usage.outputTokens,
                            cacheCreationInputTokens = usage.cacheCreationInputTokens,
                            cacheReadInputTokens = usage.cacheReadInputTokens
                        )
                    }

                    finalResponse = Message(
                        id = response.id,
                        content = assistantMessage,
                        isFromUser = false,
                        jsonResponse = null,
                        tokenUsage = tokenUsage
                    )

                    continueLoop = false
                }
            }

            if (finalResponse == null) {
                return@withContext Result.failure(Exception("Max tool use loops exceeded"))
            }

            // Save assistant message to database
            messageDao.insertMessage(finalResponse.toEntity())

            Result.success(finalResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendMessage", e)
            Result.failure(e)
        }
    }

    private fun shouldSummarize(): Boolean {
        val config = _summarizationConfig.value
        if (!config.enabled) return false
        return conversationHistory.size >= config.triggerThreshold
    }

    private suspend fun compressConversationHistory() {
        val config = _summarizationConfig.value

        try {
            val splitIndex = (conversationHistory.size - config.retentionCount).coerceAtLeast(0)
            if (splitIndex <= 1) return

            val messagesToSummarize = conversationHistory.take(splitIndex)
            val messagesToKeep = conversationHistory.drop(splitIndex)

            // Calculate original token count (estimate: ~4 chars = 1 token)
            val originalTokenCount = messagesToSummarize.sumOf {
                contentToString(it.content).length / 4
            }

            // Create summarization request
            val summarizationPrompt = buildSummarizationPrompt(messagesToSummarize)
            val request = ChatRequest(
                system = "You are a conversation summarizer. Create concise summaries preserving key context.",
                messages = listOf(MessageDto(role = "user", content = summarizationPrompt)),
                maxTokens = 500,
                temperature = 0.3
            )

            val response = apiService.sendMessage(request)
            val summaryText = response.content.firstOrNull()?.text ?: return
            val summaryTokens = response.usage?.outputTokens ?: 0

            // Create summary message
            val summaryMessage = MessageDto(
                role = "assistant",
                content = "[SUMMARY of ${messagesToSummarize.size} messages]\n\n$summaryText"
            )

            // Replace old messages with summary in memory
            conversationHistory.clear()
            conversationHistory.add(summaryMessage)
            conversationHistory.addAll(messagesToKeep)

            // Update database: delete all messages and insert new ones
            messageDao.deleteAllMessages()

            // Insert summary message
            val summaryEntity = Message(
                id = UUID.randomUUID().toString(),
                content = contentToString(summaryMessage.content),
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                isSummary = true,
                summarizedMessageCount = messagesToSummarize.size,
                originalTokenCount = originalTokenCount
            )
            messageDao.insertMessage(summaryEntity.toEntity())

            // Insert remaining messages (approximating their original data)
            messagesToKeep.forEach { msg ->
                val msgEntity = Message(
                    id = UUID.randomUUID().toString(),
                    content = contentToString(msg.content),
                    isFromUser = msg.role == "user",
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(msgEntity.toEntity())
            }

            // Update stats
            val tokensSaved = originalTokenCount - summaryTokens
            val currentStats = _summarizationStats.value
            _summarizationStats.value = currentStats.copy(
                totalSummarizations = currentStats.totalSummarizations + 1,
                tokensSaved = currentStats.tokensSaved + tokensSaved,
                compressionRatio = if (originalTokenCount > 0) {
                    tokensSaved.toDouble() / originalTokenCount.toDouble()
                } else 0.0
            )

        } catch (e: Exception) {
            // On failure, keep original messages - don't lose data
        }
    }

    private fun buildSummarizationPrompt(messages: List<MessageDto>): String {
        val conversationText = messages.joinToString("\n\n") { msg ->
            "${msg.role.uppercase()}: ${contentToString(msg.content)}"
        }

        return """
            Summarize the following conversation, preserving all important context, decisions, and information:

            $conversationText

            Provide a concise summary that maintains key details while reducing length.
        """.trimIndent()
    }
}