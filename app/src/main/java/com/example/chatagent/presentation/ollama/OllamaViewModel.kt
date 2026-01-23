package com.example.chatagent.presentation.ollama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.OllamaModel
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

        // Observe inference stats
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
            val currentMessages = _uiState.value.messages.dropLast(1) // Exclude the message we just added

            ollamaRepository.sendMessage(messageText, currentMessages)
                .onSuccess { assistantMessage ->
                    _uiState.update { currentState ->
                        val newMessages = currentState.messages + assistantMessage

                        // Calculate cumulative token usage
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
    val tokensPerSecond: Double = 0.0
)
