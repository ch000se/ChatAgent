package com.example.chatagent.presentation.analyst

import android.content.ContentResolver
import android.content.res.AssetManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.AnalystFile
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.OllamaGenerationConfig
import com.example.chatagent.domain.model.OllamaModel
import com.example.chatagent.domain.repository.OllamaRepository
import com.example.chatagent.domain.usecase.BuildAnalystPromptUseCase
import com.example.chatagent.domain.usecase.ParseAnalystFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AnalystViewModel @Inject constructor(
    private val ollamaRepository: OllamaRepository,
    private val parseFileUseCase: ParseAnalystFileUseCase,
    private val buildPromptUseCase: BuildAnalystPromptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalystUiState())
    val uiState: StateFlow<AnalystUiState> = _uiState.asStateFlow()

    val selectedModel: StateFlow<String> = ollamaRepository.getSelectedModel()

    private var lastUri: Uri? = null
    private var lastContentResolver: ContentResolver? = null

    init {
        checkConnection()
        loadModels()

        viewModelScope.launch {
            ollamaRepository.getInferenceStats().collect { stats ->
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
                            connectionError = error.message ?: "Failed to connect"
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
                    _uiState.update { it.copy(availableModels = models, isLoadingModels = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingModels = false, error = "Failed to load models: ${error.message}")
                    }
                }
        }
    }

    fun selectModel(model: String) {
        ollamaRepository.setSelectedModel(model)
    }

    fun onFilePicked(uri: Uri, contentResolver: ContentResolver) {
        lastUri = uri
        lastContentResolver = contentResolver

        viewModelScope.launch {
            _uiState.update { it.copy(isParsingFile = true, fileError = null) }

            val maxChars = _uiState.value.generationConfig.contextWindow * 3

            parseFileUseCase(uri, contentResolver, maxChars)
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            loadedFile = file,
                            isParsingFile = false,
                            messages = emptyList()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isParsingFile = false,
                            fileError = error.message ?: "Failed to parse file"
                        )
                    }
                }
        }
    }

    fun clearFile() {
        lastUri = null
        lastContentResolver = null
        _uiState.update { it.copy(loadedFile = null, messages = emptyList()) }
    }

    fun loadSampleFile(assetManager: AssetManager, assetFileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsingFile = true, fileError = null) }

            try {
                val rawText = assetManager.open(assetFileName).bufferedReader().use { it.readText() }
                val maxChars = _uiState.value.generationConfig.contextWindow * 3

                parseFileUseCase.fromRawText(rawText, assetFileName, maxChars)
                    .onSuccess { file ->
                        _uiState.update {
                            it.copy(loadedFile = file, isParsingFile = false, messages = emptyList())
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isParsingFile = false, fileError = error.message ?: "Failed to parse sample")
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isParsingFile = false, fileError = e.message ?: "Failed to load sample")
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        val file = _uiState.value.loadedFile ?: return
        if (messageText.isEmpty()) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = messageText,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
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
            val config = _uiState.value.generationConfig
            val promptResult = buildPromptUseCase(file, config.contextWindow)
            val conversationHistory = _uiState.value.messages.dropLast(1)

            ollamaRepository.sendMessageWithConfig(
                message = messageText,
                conversationHistory = conversationHistory,
                config = promptResult.recommendedConfig,
                systemPrompt = promptResult.systemPrompt
            )
                .onSuccess { assistantMessage ->
                    _uiState.update { state ->
                        val newMessages = state.messages + assistantMessage
                        val (inputTokens, outputTokens) = newMessages
                            .mapNotNull { it.tokenUsage }
                            .fold(0 to 0) { (input, output), usage ->
                                (input + usage.inputTokens) to (output + usage.outputTokens)
                            }
                        state.copy(
                            messages = newMessages,
                            isLoading = false,
                            totalInputTokens = inputTokens,
                            totalOutputTokens = outputTokens
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to get response"
                        )
                    }
                }
        }
    }

    fun updateContextWindow(value: Int) {
        _uiState.update { it.copy(generationConfig = it.generationConfig.copy(contextWindow = value)) }
        // Re-parse file with new context window
        val uri = lastUri
        val resolver = lastContentResolver
        if (uri != null && resolver != null) {
            onFilePicked(uri, resolver)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, fileError = null, connectionError = null) }
    }

    fun clearConversation() {
        _uiState.update { it.copy(messages = emptyList()) }
    }
}

data class AnalystUiState(
    val isConnected: Boolean = false,
    val isCheckingConnection: Boolean = false,
    val connectionError: String? = null,
    val ollamaVersion: String? = null,
    val availableModels: List<OllamaModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val loadedFile: AnalystFile? = null,
    val isParsingFile: Boolean = false,
    val fileError: String? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val generationConfig: OllamaGenerationConfig = OllamaGenerationConfig(
        temperature = 0.1,
        maxTokens = 1024,
        contextWindow = 4096,
        topP = 0.85,
        topK = 20,
        repeatPenalty = 1.15
    ),
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val tokensPerSecond: Double = 0.0,
    val lastInferenceTimeMs: Long = 0
)
