package com.example.chatagent.data.repository

import android.util.Log
import com.example.chatagent.data.local.dao.MessageDao
import com.example.chatagent.data.mapper.toEntity
import com.example.chatagent.data.remote.api.OllamaApiService
import com.example.chatagent.data.remote.dto.OllamaChatRequest
import com.example.chatagent.data.remote.dto.OllamaMessage
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.OllamaModel
import com.example.chatagent.domain.model.TokenUsage
import com.example.chatagent.domain.repository.OllamaInferenceStats
import com.example.chatagent.domain.repository.OllamaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.chatagent.data.mapper.toDomain

@Singleton
class OllamaRepositoryImpl @Inject constructor(
    private val ollamaApiService: OllamaApiService,
    private val messageDao: MessageDao
) : OllamaRepository {

    private val TAG = "OllamaRepository"

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _selectedModel = MutableStateFlow("llama3.2")
    private val _serverUrl = MutableStateFlow("http://localhost:11434/")
    private val _inferenceStats = MutableStateFlow(OllamaInferenceStats())

    override fun getSelectedModel(): StateFlow<String> = _selectedModel.asStateFlow()

    override fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    override fun getServerUrl(): StateFlow<String> = _serverUrl.asStateFlow()

    override fun setServerUrl(url: String) {
        _serverUrl.value = url
    }

    override fun getInferenceStats(): StateFlow<OllamaInferenceStats> = _inferenceStats.asStateFlow()

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            // Save user message to database
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = message,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(userMessage.toEntity())

            // Build conversation messages for Ollama
            val ollamaMessages = conversationHistory.map { msg ->
                OllamaMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.content
                )
            } + OllamaMessage(role = "user", content = message)

            val request = OllamaChatRequest(
                model = _selectedModel.value,
                messages = ollamaMessages,
                stream = false
            )

            Log.d(TAG, "Sending message to Ollama: model=${_selectedModel.value}")

            val response = ollamaApiService.chat(request)

            if (response.isSuccessful && response.body() != null) {
                val ollamaResponse = response.body()!!

                // Calculate tokens per second
                val tokensPerSecond = if (ollamaResponse.evalDuration != null && ollamaResponse.evalDuration > 0) {
                    (ollamaResponse.evalCount ?: 0) / (ollamaResponse.evalDuration / 1_000_000_000.0)
                } else 0.0

                // Update inference stats
                _inferenceStats.value = OllamaInferenceStats(
                    totalDurationMs = (ollamaResponse.totalDuration ?: 0) / 1_000_000,
                    promptEvalCount = ollamaResponse.promptEvalCount ?: 0,
                    evalCount = ollamaResponse.evalCount ?: 0,
                    tokensPerSecond = tokensPerSecond
                )

                val tokenUsage = TokenUsage(
                    inputTokens = ollamaResponse.promptEvalCount ?: 0,
                    outputTokens = ollamaResponse.evalCount ?: 0,
                    totalTokens = (ollamaResponse.promptEvalCount ?: 0) + (ollamaResponse.evalCount ?: 0)
                )

                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = ollamaResponse.message.content,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    tokenUsage = tokenUsage
                )

                // Save assistant message to database
                messageDao.insertMessage(assistantMessage.toEntity())

                Log.d(TAG, "Received response from Ollama: ${assistantMessage.content.take(100)}...")
                Result.success(assistantMessage)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Ollama API error: ${response.code()} - $errorBody")
                Result.failure(Exception("Ollama error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to Ollama", e)
            Result.failure(e)
        }
    }

    override suspend fun listModels(): Result<List<OllamaModel>> = withContext(Dispatchers.IO) {
        try {
            val response = ollamaApiService.listModels()

            if (response.isSuccessful && response.body() != null) {
                val models = response.body()!!.models.map { modelInfo ->
                    OllamaModel(
                        name = modelInfo.name,
                        size = modelInfo.size,
                        modifiedAt = modelInfo.modifiedAt,
                        parameterSize = modelInfo.details?.parameterSize,
                        quantizationLevel = modelInfo.details?.quantizationLevel,
                        family = modelInfo.details?.family
                    )
                }
                Log.d(TAG, "Listed ${models.size} models from Ollama")
                Result.success(models)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to list models: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to list models: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing Ollama models", e)
            Result.failure(e)
        }
    }

    override suspend fun checkConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = ollamaApiService.getVersion()

            if (response.isSuccessful && response.body() != null) {
                val version = response.body()!!.version
                Log.d(TAG, "Connected to Ollama version: $version")
                Result.success(version)
            } else {
                Result.failure(Exception("Failed to connect to Ollama"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Ollama connection", e)
            Result.failure(e)
        }
    }

    override fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun clearConversationHistory() {
        repositoryScope.launch {
            messageDao.deleteAllMessages()
        }
    }
}
