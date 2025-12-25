package com.example.chatagent.presentation.rag

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.repository.ChatRepository
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for demonstrating comparison of responses with RAG and without RAG
 *
 * Demonstrates two modes of AI agent operation:
 * 1. 🟦 WITHOUT RAG - LLM responds based only on general knowledge
 * 2. 🟩 WITH RAG - LLM receives additional context from relevant chunks
 */
@HiltViewModel
class RagComparisonViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RagComparisonUiState())
    val uiState: StateFlow<RagComparisonUiState> = _uiState.asStateFlow()

    private val TAG = "RagComparisonVM"

    /**
     * 🔴 Mode 1: Answer WITHOUT RAG
     *
     * LLM receives only the user's question without additional context
     */
    fun queryWithoutRAG(userQuestion: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                Log.d(TAG, "=== QUERY WITHOUT RAG ===")
                Log.d(TAG, "User question: $userQuestion")

                // Simple prompt without context - explicitly allow using general knowledge
                val prompt = """
                    You are an AI assistant. Answer the user's question using your general knowledge and training data.

                    IMPORTANT INSTRUCTIONS:
                    - Use your own knowledge to provide a helpful answer
                    - DO NOT say "information not available in knowledge base"
                    - You are NOT restricted to any specific context or knowledge base
                    - Provide a complete and informative answer based on what you know

                    QUESTION: $userQuestion

                    ANSWER:
                """.trimIndent()

                Log.d(TAG, "Sending prompt to LLM (no context)...")

                // Call LLM without additional context
                chatRepository.sendMessage(prompt).fold(
                    onSuccess = { message ->
                        Log.d(TAG, "Response received (${message.content.length} chars)")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                responseWithoutRAG = ResponseData(
                                    answer = message.content,
                                    prompt = prompt,
                                    context = null,
                                    relevantChunks = emptyList(),
                                    executionTimeMs = 0 // TODO: measure time
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error querying without RAG", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error querying without RAG: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in queryWithoutRAG", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Exception: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 🟢 Mode 2: Answer WITH RAG (with filtering and reranking)
     *
     * Pipeline:
     * 1. Search for relevant chunks by query
     * 2. Filter chunks by similarity threshold
     * 3. Rerank filtered chunks
     * 4. Build context from top-K chunks
     * 5. Combine context with query
     * 6. Call LLM with enhanced prompt
     */
    fun queryWithRAG(
        userQuestion: String,
        topK: Int = 3,
        similarityThreshold: Float = 0.0f,
        useReranking: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                Log.d(TAG, "=== QUERY WITH RAG ===")
                Log.d(TAG, "User question: $userQuestion")
                Log.d(TAG, "Top-K: $topK")
                Log.d(TAG, "Similarity threshold: $similarityThreshold")
                Log.d(TAG, "Use reranking: $useReranking")

                val startTime = System.currentTimeMillis()

                // ===== STEP 1: Search for relevant chunks =====
                Log.d(TAG, "Step 1: Searching relevant chunks...")
                val searchResult = searchDocumentsUseCase(userQuestion, topK * 2) // Get more chunks for filtering

                searchResult.fold(
                    onSuccess = { rawChunks ->
                        Log.d(TAG, "Found ${rawChunks.size} raw chunks")

                        if (rawChunks.isEmpty()) {
                            Log.w(TAG, "No indexed documents found!")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Knowledge base is empty. Please index documents first."
                                )
                            }
                            return@launch
                        }

                        // ===== STEP 2: Filter by similarity threshold =====
                        Log.d(TAG, "Step 2: Filtering chunks by threshold...")
                        val filteredChunks = filterChunksBySimilarity(rawChunks, similarityThreshold)
                        Log.d(TAG, "After filtering: ${filteredChunks.size} chunks (removed ${rawChunks.size - filteredChunks.size})")

                        if (filteredChunks.isEmpty()) {
                            Log.w(TAG, "No chunks passed similarity threshold!")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "No relevant documents found above similarity threshold ($similarityThreshold)"
                                )
                            }
                            return@launch
                        }

                        // ===== STEP 3: Rerank chunks (if enabled) =====
                        val processedChunks = if (useReranking) {
                            Log.d(TAG, "Step 3: Reranking chunks...")
                            rerankChunks(filteredChunks, userQuestion)
                        } else {
                            Log.d(TAG, "Step 3: Skipping reranking (disabled)")
                            filteredChunks
                        }

                        // Take top-K after filtering and reranking
                        val finalChunks = processedChunks.take(topK)
                        Log.d(TAG, "Final chunks selected: ${finalChunks.size}")

                        // ===== STEP 4: Build context =====
                        Log.d(TAG, "Step 4: Building context from chunks...")
                        val context = buildContextFromChunks(finalChunks)

                        // ===== STEP 5: Create RAG prompt =====
                        Log.d(TAG, "Step 5: Creating RAG prompt...")
                        val prompt = buildRAGPrompt(context, userQuestion)

                        Log.d(TAG, "Step 6: Sending to LLM with context (${context.length} chars)...")

                        // ===== STEP 6: Call LLM with context =====
                        chatRepository.sendMessage(prompt).fold(
                            onSuccess = { message ->
                                val executionTime = System.currentTimeMillis() - startTime
                                Log.d(TAG, "Response received in ${executionTime}ms")

                                // Log found chunks for analysis
                                finalChunks.forEachIndexed { index, chunk ->
                                    Log.d(TAG, "Chunk #${index + 1}: similarity=${String.format("%.4f", chunk.similarity)}, doc=${chunk.document.fileName}")
                                }

                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        responseWithRAG = ResponseData(
                                            answer = message.content,
                                            prompt = prompt,
                                            context = context,
                                            relevantChunks = finalChunks,
                                            executionTimeMs = executionTime,
                                            totalChunksFound = rawChunks.size,
                                            chunksAfterFiltering = filteredChunks.size,
                                            similarityThreshold = similarityThreshold,
                                            usedReranking = useReranking
                                        )
                                    )
                                }
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Error calling LLM with RAG", error)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Error querying with RAG: ${error.message}"
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error searching documents", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error searching documents: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in queryWithRAG", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Exception: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Filters chunks by similarity threshold
     *
     * Removes chunks with similarity below threshold
     */
    private fun filterChunksBySimilarity(
        chunks: List<DocumentSearchResult>,
        threshold: Float
    ): List<DocumentSearchResult> {
        val filtered = chunks.filter { it.similarity >= threshold }

        Log.d(TAG, "Filtering: threshold=$threshold")
        chunks.forEach { chunk ->
            val status = if (chunk.similarity >= threshold) "PASS" else "FILTERED"
            Log.d(TAG, "  [$status] similarity=${String.format("%.4f", chunk.similarity)} - ${chunk.document.fileName}")
        }

        return filtered
    }

    /**
     * Reranks chunks using advanced scoring
     *
     * Strategy: Combines similarity score with additional factors:
     * - Document diversity bonus (prefer chunks from different documents)
     * - Length normalization (prefer chunks with optimal length)
     * - Position bonus (slightly prefer earlier chunks in document)
     */
    private fun rerankChunks(
        chunks: List<DocumentSearchResult>,
        query: String
    ): List<DocumentSearchResult> {
        val seenDocuments = mutableSetOf<String>()

        val reranked = chunks.map { chunk ->
            var score = chunk.similarity

            // Diversity bonus: +0.05 for first chunk from each document
            if (!seenDocuments.contains(chunk.document.id)) {
                score += 0.05f
                seenDocuments.add(chunk.document.id)
            }

            // Length normalization: optimal chunk length is 100-500 chars
            val lengthScore = when {
                chunk.chunk.text.length < 50 -> -0.05f  // Too short
                chunk.chunk.text.length > 1000 -> -0.03f // Too long
                else -> 0.02f // Good length
            }
            score += lengthScore

            // Position bonus: earlier chunks get small bonus (max +0.02)
            val positionBonus = maxOf(0f, (10 - chunk.chunk.chunkIndex) * 0.002f)
            score = score.coerceIn(0f, 1f)

            Log.d(TAG, "Rerank: ${chunk.document.fileName} chunk#${chunk.chunk.chunkIndex} " +
                    "original=${String.format("%.4f", chunk.similarity)} " +
                    "reranked=${String.format("%.4f", score)}")

            chunk.copy(similarity = score)
        }.sortedByDescending { it.similarity }

        return reranked
    }

    /**
     * Builds context from relevant chunks
     *
     * @param chunks Found relevant chunks (already sorted by similarity)
     * @return Text context for prompt
     */
    private fun buildContextFromChunks(chunks: List<DocumentSearchResult>): String {
        return chunks.joinToString("\n\n") { result ->
            """
            [Document: ${result.document.fileName}]
            [Relevance: ${String.format("%.2f", result.similarity * 100)}%]
            ${result.chunk.text}
            """.trimIndent()
        }
    }

    /**
     * Builds RAG prompt with context
     *
     * Instructions for LLM:
     * - Answer ONLY based on context
     * - If answer is not there — say so honestly
     * - Don't add knowledge from training data
     */
    private fun buildRAGPrompt(context: String, userQuestion: String): String {
        return """
            You are an AI assistant with access to a knowledge base.

            CRITICAL INSTRUCTIONS:
            - Answer ONLY based on the context provided below
            - If the answer is not in the context, clearly state "Information not available in knowledge base"
            - DO NOT add your own knowledge or assumptions
            - Cite the document name where the information was found

            CONTEXT FROM KNOWLEDGE BASE:
            $context

            USER QUESTION:
            $userQuestion

            YOUR ANSWER (based on context):
        """.trimIndent()
    }

    /**
     * Executes both queries in parallel for comparison
     */
    fun compareResponses(
        userQuestion: String,
        topK: Int = 3,
        similarityThreshold: Float = 0.0f,
        useReranking: Boolean = false
    ) {
        viewModelScope.launch {
            // Clear previous results
            _uiState.update {
                it.copy(
                    responseWithoutRAG = null,
                    responseWithRAG = null,
                    error = null
                )
            }

            // Launch both modes
            queryWithoutRAG(userQuestion)
            queryWithRAG(userQuestion, topK, similarityThreshold, useReranking)
        }
    }

    fun clearResults() {
        _uiState.update { RagComparisonUiState() }
    }
}

/**
 * UI State for RAG demonstration
 */
data class RagComparisonUiState(
    val isLoading: Boolean = false,
    val responseWithoutRAG: ResponseData? = null,
    val responseWithRAG: ResponseData? = null,
    val error: String? = null
)

/**
 * Data about response (with RAG or without)
 */
data class ResponseData(
    val answer: String,                              // LLM answer
    val prompt: String,                              // Prompt sent to LLM
    val context: String?,                            // Context from documents (null for mode without RAG)
    val relevantChunks: List<DocumentSearchResult>,  // Found chunks (for analysis)
    val executionTimeMs: Long,                       // Execution time
    val totalChunksFound: Int = 0,                   // Total chunks found before filtering
    val chunksAfterFiltering: Int = 0,               // Chunks remaining after filtering
    val similarityThreshold: Float = 0.0f,           // Similarity threshold used
    val usedReranking: Boolean = false               // Whether reranking was applied
)
