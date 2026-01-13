package com.example.chatagent.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.SummarizationConfig
import com.example.chatagent.domain.model.SummarizationStats
import com.example.chatagent.domain.repository.ChatRepository
import com.example.chatagent.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val commandDispatcher: com.example.chatagent.domain.command.CommandDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentSystemPrompt: StateFlow<String> = chatRepository.getSystemPrompt()
    val currentTemperature: StateFlow<Double> = chatRepository.getTemperature()
    val summarizationConfig: StateFlow<SummarizationConfig> = chatRepository.getSummarizationConfig()
    val summarizationStats: StateFlow<SummarizationStats> = chatRepository.getSummarizationStats()

    init {
        // Observe summarization stats and update UI state
        viewModelScope.launch {
            summarizationStats.collect { stats ->
                _uiState.update {
                    it.copy(
                        totalSummarizations = stats.totalSummarizations,
                        tokensSaved = stats.tokensSaved,
                        compressionRatio = stats.compressionRatio
                    )
                }
            }
        }

        viewModelScope.launch {
            summarizationConfig.collect { config ->
                _uiState.update {
                    it.copy(summarizationEnabled = config.enabled)
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty()) return

        // Check if input is a command
        val command = com.example.chatagent.domain.util.CommandParser.parse(messageText)
        if (command != null) {
            handleCommand(command, messageText)
            return
        }

        // Regular message handling
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
            sendMessageUseCase(messageText)
                .onSuccess { agentMessage ->
                    _uiState.update { currentState ->
                        val newMessages = currentState.messages + agentMessage

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
                            error = exception.message ?: "Unknown error occurred"
                        )
                    }
                }
        }
    }

    private fun handleCommand(command: com.example.chatagent.domain.model.Command, rawInput: String) {
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = rawInput,
            isFromUser = true,
            isCommand = true
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
            try {
                val result = commandDispatcher.dispatch(command)

                val commandResponse = Message(
                    id = UUID.randomUUID().toString(),
                    content = result.content,
                    isFromUser = false,
                    isCommand = true,
                    sources = result.metadata?.sources,
                    commandMetadata = result.metadata
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + commandResponse,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Command execution failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setSystemPrompt(prompt: String) {
        chatRepository.setSystemPrompt(prompt)
    }

    fun setTemperature(temperature: Double) {
        chatRepository.setTemperature(temperature)
        _uiState.update { it.copy(currentTemperature = temperature) }
    }

    fun clearConversation() {
        chatRepository.clearConversationHistory()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                totalInputTokens = 0,
                totalOutputTokens = 0,
                totalTokens = 0
            )
        }
    }

    fun toggleSummarization(enabled: Boolean) {
        val currentConfig = summarizationConfig.value
        chatRepository.setSummarizationConfig(
            currentConfig.copy(enabled = enabled)
        )
    }
}
