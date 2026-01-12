package com.example.chatagent.presentation.ragchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.repository.ChatRepository
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RagChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RagChatUiState())
    val uiState: StateFlow<RagChatUiState> = _uiState.asStateFlow()

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
            try {
                // Step 1: Search for relevant documents
                val searchResult = searchDocumentsUseCase(
                    query = messageText,
                    topK = 5
                )

                val searchResults = searchResult.getOrNull() ?: emptyList()

                // Step 2: Build RAG context from search results
                val ragContext = if (searchResults.isNotEmpty()) {
                    buildRagContext(searchResults)
                } else {
                    null
                }

                // Step 3: Prepare the message with RAG context
                val enhancedMessage = if (ragContext != null) {
                    """
                    Context from documents:
                    $ragContext

                    User question: $messageText

                    Please answer the user's question based on the context provided above. If the context doesn't contain relevant information, say so clearly.
                    """.trimIndent()
                } else {
                    messageText
                }

                // Step 4: Send enhanced message to LLM
                chatRepository.sendMessage(enhancedMessage)
                    .onSuccess { agentMessage ->
                        // Step 5: Attach sources to the response
                        val messageWithSources = agentMessage.copy(
                            sources = if (searchResults.isNotEmpty()) searchResults else null
                        )

                        _uiState.update { currentState ->
                            currentState.copy(
                                messages = currentState.messages + messageWithSources,
                                isLoading = false,
                                totalInputTokens = currentState.totalInputTokens + (agentMessage.tokenUsage?.inputTokens ?: 0),
                                totalOutputTokens = currentState.totalOutputTokens + (agentMessage.tokenUsage?.outputTokens ?: 0),
                                totalTokens = currentState.totalTokens + (agentMessage.tokenUsage?.totalTokens ?: 0)
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Unknown error"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun buildRagContext(searchResults: List<DocumentSearchResult>): String {
        return searchResults.joinToString("\n\n") { result ->
            """
            [Source: ${result.document.fileName} (Similarity: ${String.format("%.1f", result.similarity * 100)}%)]
            ${result.chunk.text}
            """.trimIndent()
        }
    }

    fun clearChat() {
        chatRepository.clearConversationHistory()
        _uiState.update {
            RagChatUiState()
        }
    }

    fun toggleSourcesExpanded(messageId: String) {
        _uiState.update { state ->
            val expandedSources = state.expandedSources.toMutableSet()
            if (expandedSources.contains(messageId)) {
                expandedSources.remove(messageId)
            } else {
                expandedSources.add(messageId)
            }
            state.copy(expandedSources = expandedSources)
        }
    }
}

data class RagChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalTokens: Int = 0,
    val expandedSources: Set<String> = emptySet()
)
