
package com.example.chatagent.presentation.ollama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.OllamaComparisonResult
import com.example.chatagent.domain.model.OllamaGenerationConfig
import com.example.chatagent.domain.model.OllamaModel
import com.example.chatagent.domain.model.OllamaPromptTemplate
import com.example.chatagent.domain.repository.OllamaInferenceStats
import com.example.chatagent.domain.repository.OllamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OllamaViewModel @Inject constructor(
    private val ollamaRepository: OllamaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OllamaUiState())
    val uiState: StateFlow<OllamaUiState> = _uiState.asStateFlow()

    val selectedModel: StateFlow<String> = ollamaRepository.getSelectedModel()
    val inferenceStats: StateFlow<OllamaInferenceStats> = ollamaRepository.getInferenceStats()

    init {
        checkConnection()
        loadModels()

        viewModelScope.launch {
            inferenceStats.collect { stats ->
                _uiState.update {
                    it.copy(
                        lastInferenceTimeMs = stats.totalDurationMs,
                        tokensPerSecond = stats.tokensPerSecond
                    )
                }
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingConnection = true) }

            ollamaRepository.checkConnection()
                .onSuccess { version ->
                    _uiState.update {
                        it.copy(
                            isConnected = true,
                            ollamaVersion = version,
                            isCheckingConnection = false,
                            connectionError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            isCheckingConnection = false,
                            connectionError = error.message ?: "Failed to connect to Ollama"
                        )
                    }
                }
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }

            ollamaRepository.listModels()
                .onSuccess { models ->
                    _uiState.update {
                        it.copy(
                            availableModels = models,
                            isLoadingModels = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingModels = false,
                            error = "Failed to load models: ${error.message}"
                        )
                    }
                }
        }
    }

    fun selectModel(model: String) {
        ollamaRepository.setSelectedModel(model)
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // --- Generation Config ---

    fun updateTemperature(value: Double) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(temperature = value)) }
    }

    fun updateMaxTokens(value: Int) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(maxTokens = value)) }
    }

    fun updateContextWindow(value: Int) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(contextWindow = value)) }
    }

    fun updateTopP(value: Double) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(topP = value)) }
    }

    fun updateTopK(value: Int) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(topK = value)) }
    }

    fun updateRepeatPenalty(value: Double) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(repeatPenalty = value)) }
    }

    fun resetConfigToDefault() {
        _uiState.update { it.copy(generationConfig = OllamaGenerationConfig.DEFAULT) }
    }

    // --- Prompt Templates ---

    fun selectTemplate(template: OllamaPromptTemplate?) {
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                generationConfig = template?.recommendedConfig ?: it.generationConfig
            )
        }
    }

    // --- Send with Config ---

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty()) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = messageText,
            isFromUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val currentMessages = _uiState.value.messages.dropLast(1)
            val config = _uiState.value.generationConfig
            val systemPrompt = _uiState.value.selectedTemplate?.systemPrompt

            ollamaRepository.sendMessageWithConfig(messageText, currentMessages, config, systemPrompt)
                .onSuccess { assistantMessage ->
                    _uiState.update { currentState ->
                        val newMessages = currentState.messages + assistantMessage

                        val (inputTokens, outputTokens) = newMessages
                            .mapNotNull { it.tokenUsage }
                            .fold(0 to 0) { (input, output), usage ->
                                (input + usage.inputTokens) to (output + usage.outputTokens)
                            }

                        currentState.copy(
                            messages = newMessages,
                            isLoading = false,
                            totalInputTokens = inputTokens,
                            totalOutputTokens = outputTokens,
                            totalTokens = inputTokens + outputTokens
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to get response from Ollama"
                        )
                    }
                }
        }
    }

    // --- Comparison Mode ---

    fun runComparison() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty()) return

        _uiState.update {
            it.copy(
                inputText = "",
                isComparing = true,
                comparisonResult = null,
                error = null
            )
        }

        viewModelScope.launch {
            val defaultConfig = OllamaGenerationConfig.DEFAULT
            val optimizedConfig = _uiState.value.generationConfig
            val template = _uiState.value.selectedTemplate

            // Run with default config (no system prompt, default params)
            val defaultStart = System.currentTimeMillis()
            val defaultResult = ollamaRepository.sendMessageWithConfig(
                message = messageText,
                conversationHistory = emptyList(),
                config = defaultConfig,
                systemPrompt = null
            )
            val defaultDuration = System.currentTimeMillis() - defaultStart

            // Run with optimized config (with template + tuned params)
            val optimizedStart = System.currentTimeMillis()
            val optimizedResult = ollamaRepository.sendMessageWithConfig(
                message = messageText,
                conversationHistory = emptyList(),
                config = optimizedConfig,
                systemPrompt = template?.systemPrompt
            )
            val optimizedDuration = System.currentTimeMillis() - optimizedStart

            val defaultMsg = defaultResult.getOrNull()
            val optimizedMsg = optimizedResult.getOrNull()

            if (defaultMsg != null && optimizedMsg != null) {
                val comparison = OllamaComparisonResult(
                    defaultResponse = defaultMsg.content,
                    optimizedResponse = optimizedMsg.content,
                    defaultDurationMs = defaultDuration,
                    optimizedDurationMs = optimizedDuration,
                    defaultTokensPerSec = (defaultMsg.tokenUsage?.outputTokens ?: 0).let { tokens ->
                        if (defaultDuration > 0) tokens * 1000.0 / defaultDuration else 0.0
                    },
                    optimizedTokensPerSec = (optimizedMsg.tokenUsage?.outputTokens ?: 0).let { tokens ->
                        if (optimizedDuration > 0) tokens * 1000.0 / optimizedDuration else 0.0
                    },
                    defaultEvalCount = defaultMsg.tokenUsage?.outputTokens ?: 0,
                    optimizedEvalCount = optimizedMsg.tokenUsage?.outputTokens ?: 0,
                    configUsed = optimizedConfig,
                    templateUsed = template
                )

                _uiState.update {
                    it.copy(
                        isComparing = false,
                        comparisonResult = comparison
                    )
                }
            } else {
                val errorMsg = defaultResult.exceptionOrNull()?.message
                    ?: optimizedResult.exceptionOrNull()?.message
                    ?: "Comparison failed"
                _uiState.update {
                    it.copy(
                        isComparing = false,
                        error = errorMsg
                    )
                }
            }
        }
    }

    fun clearComparisonResult() {
        _uiState.update { it.copy(comparisonResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, connectionError = null) }
    }

    fun clearConversation() {
        ollamaRepository.clearConversationHistory()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                totalInputTokens = 0,
                totalOutputTokens = 0,
                totalTokens = 0
            )
        }
    }
}

data class OllamaUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val isCheckingConnection: Boolean = false,
    val connectionError: String? = null,
    val ollamaVersion: String? = null,
    val availableModels: List<OllamaModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalTokens: Int = 0,
    val lastInferenceTimeMs: Long = 0,
    val tokensPerSecond: Double = 0.0,
    // Optimization features
    val generationConfig: OllamaGenerationConfig = OllamaGenerationConfig.DEFAULT,
    val selectedTemplate: OllamaPromptTemplate? = null,
    val isComparing: Boolean = false,
    val comparisonResult: OllamaComparisonResult? = null
)
