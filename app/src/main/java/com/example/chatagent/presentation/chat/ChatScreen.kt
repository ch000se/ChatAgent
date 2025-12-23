package com.example.chatagent.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.SystemPrompts
import com.example.chatagent.domain.model.TokenTestScenarios
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToBenchmark: () -> Unit = {},
    onNavigateToMcp: () -> Unit = {},
    onNavigateToPipeline: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPrompt by viewModel.currentSystemPrompt.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showPromptDialog by remember { mutableStateOf(false) }
    var showTokenTestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }

    if (showPromptDialog) {
        SystemPromptDialog(
            currentPrompt = currentPrompt,
            onDismiss = { showPromptDialog = false },
            onPromptSelected = { prompt ->
                viewModel.setSystemPrompt(prompt)
                showPromptDialog = false
            }
        )
    }

    if (showTokenTestDialog) {
        TokenTestScenariosDialog(
            onDismiss = { showTokenTestDialog = false },
            onScenarioSelected = { scenario ->
                viewModel.onInputTextChanged(scenario)
                showTokenTestDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Agent") },
                actions = {
                    IconButton(onClick = onNavigateToDocuments) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Document Indexing"
                        )
                    }
                    IconButton(onClick = onNavigateToPipeline) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "MCP Pipeline"
                        )
                    }
                    IconButton(onClick = onNavigateToMcp) {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = "MCP Connection"
                        )
                    }
                    IconButton(onClick = { showTokenTestDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Token Test Scenarios"
                        )
                    }
                    IconButton(onClick = onNavigateToBenchmark) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Model Benchmark"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleSummarization(!uiState.summarizationEnabled) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = if (uiState.summarizationEnabled) {
                                "Disable Summarization"
                            } else {
                                "Enable Summarization"
                            },
                            tint = if (uiState.summarizationEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            }
                        )
                    }
                    IconButton(onClick = { showPromptDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Change System Prompt"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TemperatureSelector(
                currentTemperature = uiState.currentTemperature,
                onTemperatureSelected = { temperature ->
                    viewModel.setTemperature(temperature)
                    viewModel.clearConversation()
                },
                enabled = !uiState.isLoading
            )

            TokenUsageDisplay(
                inputTokens = uiState.totalInputTokens,
                outputTokens = uiState.totalOutputTokens,
                totalTokens = uiState.totalTokens
            )

            SummarizationStatsDisplay(
                enabled = uiState.summarizationEnabled,
                summarizationCount = uiState.totalSummarizations,
                tokensSaved = uiState.tokensSaved,
                compressionRatio = uiState.compressionRatio
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                if (uiState.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }

            uiState.error?.let { error ->
                ErrorMessage(error = error)
            }

            InputBar(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendMessage,
                enabled = !uiState.isLoading
            )
        }
    }
}

@Composable
fun TemperatureSelector(
    currentTemperature: Double,
    onTemperatureSelected: (Double) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Temperature:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            listOf(0.0, 0.7, 1.0).forEach { temp ->
                Button(
                    onClick = { onTemperatureSelected(temp) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (currentTemperature == temp) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (currentTemperature == temp) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = temp.toString(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun TokenUsageDisplay(
    inputTokens: Int,
    outputTokens: Int,
    totalTokens: Int
) {
    if (totalTokens > 0) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TokenMetric(
                    label = "Input",
                    value = inputTokens
                )
                Divider(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
                )
                TokenMetric(
                    label = "Output",
                    value = outputTokens
                )
                Divider(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
                )
                TokenMetric(
                    label = "Total",
                    value = totalTokens
                )
            }
        }
    }
}

@Composable
fun TokenMetric(
    label: String,
    value: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun SummarizationStatsDisplay(
    enabled: Boolean,
    summarizationCount: Int,
    tokensSaved: Int,
    compressionRatio: Double
) {
    if (!enabled || summarizationCount == 0) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryBooks,
                    contentDescription = "Summaries",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "$summarizationCount summaries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = "$tokensSaved tokens saved (${(compressionRatio * 100).toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isSummary -> MaterialTheme.colorScheme.tertiaryContainer
                    message.isFromUser -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Summary indicator
                if (message.isSummary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = "Summary",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Summary of ${message.summarizedMessageCount ?: 0} messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        message.originalTokenCount?.let { original ->
                            val saved = original - (message.tokenUsage?.totalTokens ?: 0)
                            if (saved > 0) {
                                Text(
                                    text = "• $saved tokens saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isFromUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        }
                    )

                    message.tokenUsage?.let { usage ->
                        Text(
                            text = "${usage.inputTokens}/${usage.outputTokens} tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isFromUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSendClick,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(error: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun SystemPromptDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onPromptSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select AI Personality") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SystemPrompts.allPrompts) { preset ->
                    val isSelected = currentPrompt == preset.prompt

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPromptSelected(preset.prompt) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TokenTestScenariosDialog(
    onDismiss: () -> Unit,
    onScenarioSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Token Test Scenarios") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TokenTestScenarios.allScenarios) { scenario ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onScenarioSelected(scenario.prompt) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = scenario.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scenario.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Est: ${scenario.estimatedInputTokens}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = scenario.expectedBehavior,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}