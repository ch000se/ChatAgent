package com.example.chatagent.presentation.rag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen for demonstrating RAG vs non-RAG comparison
 *
 * Shows two response modes side-by-side:
 * 🟦 Without RAG - LLM without context
 * 🟩 With RAG - LLM with relevant documents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagComparisonScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RagComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var userQuery by remember { mutableStateOf("") }
    var topK by remember { mutableStateOf(3) }
    var similarityThreshold by remember { mutableStateOf(0.0f) }
    var useReranking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAG Comparison Demo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RAG Comparison Tool",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Compare AI agent responses with RAG and without RAG. " +
                                    "Enter a question and press 'Compare' to see the difference.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Query input field
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = userQuery,
                            onValueChange = { userQuery = it },
                            label = { Text("Your question") },
                            placeholder = { Text("Example: How many vacation days at CompanyX?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Top-K settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top-K chunks:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(120.dp)
                            )
                            Slider(
                                value = topK.toFloat(),
                                onValueChange = { topK = it.toInt() },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$topK",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(30.dp)
                            )
                        }

                        // Similarity Threshold settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Similarity threshold:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(120.dp)
                            )
                            Slider(
                                value = similarityThreshold,
                                onValueChange = { similarityThreshold = it },
                                valueRange = 0f..0.9f,
                                steps = 17, // 0.05 increments
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format("%.2f", similarityThreshold),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(50.dp)
                            )
                        }

                        // Reranking toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Use Reranking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Improves relevance by rescoring chunks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useReranking,
                                onCheckedChange = { useReranking = it }
                            )
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.compareResponses(
                                        userQuery,
                                        topK,
                                        similarityThreshold,
                                        useReranking
                                    )
                                },
                                enabled = userQuery.isNotBlank() && !uiState.isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Compare")
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearResults() },
                                enabled = !uiState.isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Processing query...")
                        }
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Comparison results table
            if (uiState.responseWithoutRAG != null || uiState.responseWithRAG != null) {
                item {
                    Text(
                        text = "Comparison Results:",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Response WITHOUT RAG
                item {
                    ResponseCard(
                        title = "WITHOUT RAG",
                        icon = Icons.Default.Psychology,
                        color = Color(0xFF2196F3), // Blue
                        responseData = uiState.responseWithoutRAG,
                        showContext = false
                    )
                }

                // Response WITH RAG
                item {
                    ResponseCard(
                        title = "WITH RAG",
                        icon = Icons.Default.Science,
                        color = Color(0xFF4CAF50), // Green
                        responseData = uiState.responseWithRAG,
                        showContext = true
                    )
                }

                // Relevance analysis (only for RAG)
                uiState.responseWithRAG?.let { ragResponse ->
                    if (ragResponse.relevantChunks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Found Relevant Chunks:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(ragResponse.relevantChunks) { chunk ->
                            ChunkCard(chunk = chunk)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    responseData: ResponseData?,
    showContext: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Divider(color = color.copy(alpha = 0.3f))

            if (responseData != null) {
                // Answer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Answer:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = responseData.answer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Context (only for RAG)
                if (showContext && responseData.context != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Context Used:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFE0B2), // Light orange - improved visibility
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = responseData.context,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF3E2723) // Darker brown for better contrast
                            )
                        }
                    }
                }

                // Statistics
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (responseData.executionTimeMs > 0) {
                        Text(
                            text = "Execution time: ${responseData.executionTimeMs} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Show filtering stats for RAG responses
                    if (showContext) {
                        if (responseData.totalChunksFound > 0) {
                            Text(
                                text = "Chunks found: ${responseData.totalChunksFound} → After filtering: ${responseData.chunksAfterFiltering} → Used: ${responseData.relevantChunks.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (responseData.similarityThreshold > 0f) {
                            Text(
                                text = "Similarity threshold: ${String.format("%.2f", responseData.similarityThreshold)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (responseData.usedReranking) {
                            Text(
                                text = "Reranking: ENABLED",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Waiting for response...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChunkCard(chunk: com.example.chatagent.domain.model.DocumentSearchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                chunk.similarity > 0.7 -> Color(0xFFC8E6C9) // High relevance - medium green (improved visibility)
                chunk.similarity > 0.4 -> Color(0xFFFFF59D) // Medium - medium yellow (improved visibility)
                else -> Color(0xFFFFCDD2) // Low - medium red (improved visibility)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chunk #${chunk.chunk.chunkIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20) // Dark green for better contrast
                )
                Surface(
                    color = Color(0xFF1976D2), // Darker blue for better visibility
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", chunk.similarity * 100)}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Document: ${chunk.document.fileName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF424242) // Darker gray for better readability
            )

            Text(
                text = chunk.chunk.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )
        }
    }
}
