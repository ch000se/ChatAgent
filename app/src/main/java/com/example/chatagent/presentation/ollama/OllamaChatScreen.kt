package com.example.chatagent.presentation.ollama

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.OllamaComparisonResult
import com.example.chatagent.domain.model.OllamaGenerationConfig
import com.example.chatagent.domain.model.OllamaModel
import com.example.chatagent.domain.model.OllamaPromptTemplate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OllamaChatScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: OllamaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showModelSelector by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showComparisonResult by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    LaunchedEffect(uiState.comparisonResult) {
        if (uiState.comparisonResult != null) {
            showComparisonResult = true
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation") },
            text = { Text("Are you sure you want to clear the conversation history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearConversation()
                        showClearDialog = false
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showComparisonResult && uiState.comparisonResult != null) {
        ComparisonResultDialog(
            result = uiState.comparisonResult!!,
            onDismiss = {
                showComparisonResult = false
                viewModel.clearComparisonResult()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ollama Chat")
                        Text(
                            text = if (uiState.selectedTemplate != null)
                                "Template: ${uiState.selectedTemplate!!.name}"
                            else "Local LLM - Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = if (showSettings) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { viewModel.checkConnection() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, "Clear")
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
            // Connection Status
            ConnectionStatusBar(
                isConnected = uiState.isConnected,
                isChecking = uiState.isCheckingConnection,
                version = uiState.ollamaVersion,
                error = uiState.connectionError,
                onRetry = { viewModel.checkConnection() }
            )

            // Model Selector
            ModelSelectorBar(
                selectedModel = selectedModel,
                availableModels = uiState.availableModels,
                isLoading = uiState.isLoadingModels,
                expanded = showModelSelector,
                onExpandChange = { showModelSelector = it },
                onModelSelected = { model ->
                    viewModel.selectModel(model)
                    showModelSelector = false
                },
                onRefresh = { viewModel.loadModels() }
            )

            // Settings Panel (expandable)
            AnimatedVisibility(visible = showSettings) {
                SettingsPanel(
                    config = uiState.generationConfig,
                    selectedTemplate = uiState.selectedTemplate,
                    onTemperatureChange = viewModel::updateTemperature,
                    onMaxTokensChange = viewModel::updateMaxTokens,
                    onContextWindowChange = viewModel::updateContextWindow,
                    onTopPChange = viewModel::updateTopP,
                    onTopKChange = viewModel::updateTopK,
                    onRepeatPenaltyChange = viewModel::updateRepeatPenalty,
                    onTemplateSelected = viewModel::selectTemplate,
                    onResetDefaults = viewModel::resetConfigToDefault
                )
            }

            // Model info (quantization)
            val currentModel = uiState.availableModels.find { it.name == selectedModel }
            if (currentModel != null && (currentModel.quantizationLevel != null || currentModel.parameterSize != null)) {
                ModelInfoBar(model = currentModel)
            }

            // Inference Stats
            if (uiState.lastInferenceTimeMs > 0) {
                InferenceStatsBar(
                    inferenceTimeMs = uiState.lastInferenceTimeMs,
                    tokensPerSecond = uiState.tokensPerSecond
                )
            }

            // Token Usage
            if (uiState.totalTokens > 0) {
                TokenUsageBar(
                    inputTokens = uiState.totalInputTokens,
                    outputTokens = uiState.totalOutputTokens,
                    totalTokens = uiState.totalTokens
                )
            }

            // Comparison in progress
            if (uiState.isComparing) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Running comparison (default vs optimized)...",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    item { EmptyStateMessage() }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    OllamaMessageBubble(message = message)
                }

                if (uiState.isLoading) {
                    item { LoadingIndicator() }
                }
            }

            // Error
            uiState.error?.let { error ->
                ErrorMessage(error = error, onDismiss = { viewModel.clearError() })
            }

            // Input Bar with Compare button
            InputBarWithCompare(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendMessage,
                onCompareClick = viewModel::runComparison,
                enabled = !uiState.isLoading && uiState.isConnected && !uiState.isComparing
            )
        }
    }
}

@Composable
fun SettingsPanel(
    config: OllamaGenerationConfig,
    selectedTemplate: OllamaPromptTemplate?,
    onTemperatureChange: (Double) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onContextWindowChange: (Int) -> Unit,
    onTopPChange: (Double) -> Unit,
    onTopKChange: (Int) -> Unit,
    onRepeatPenaltyChange: (Double) -> Unit,
    onTemplateSelected: (OllamaPromptTemplate?) -> Unit,
    onResetDefaults: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Generation Parameters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onResetDefaults) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prompt Templates
            Text(
                "Prompt Template",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTemplate == null,
                    onClick = { onTemplateSelected(null) },
                    label = { Text("Custom") }
                )
                OllamaPromptTemplate.TEMPLATES.forEach { template ->
                    FilterChip(
                        selected = selectedTemplate == template,
                        onClick = { onTemplateSelected(template) },
                        label = { Text(template.taskType.displayName) }
                    )
                }
            }

            if (selectedTemplate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    selectedTemplate.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Temperature
            ParamSlider(
                label = "Temperature",
                value = config.temperature.toFloat(),
                valueRange = 0f..2f,
                valueLabel = "%.2f".format(config.temperature),
                onValueChange = { onTemperatureChange(it.toDouble()) }
            )

            // Max Tokens
            ParamSlider(
                label = "Max Tokens",
                value = config.maxTokens.toFloat(),
                valueRange = 64f..4096f,
                valueLabel = "${config.maxTokens}",
                onValueChange = { onMaxTokensChange(it.toInt()) }
            )

            // Context Window
            ParamSlider(
                label = "Context Window",
                value = config.contextWindow.toFloat(),
                valueRange = 512f..8192f,
                valueLabel = "${config.contextWindow}",
                onValueChange = { onContextWindowChange(it.toInt()) }
            )

            // Top P
            ParamSlider(
                label = "Top P",
                value = config.topP.toFloat(),
                valueRange = 0f..1f,
                valueLabel = "%.2f".format(config.topP),
                onValueChange = { onTopPChange(it.toDouble()) }
            )

            // Top K
            ParamSlider(
                label = "Top K",
                value = config.topK.toFloat(),
                valueRange = 1f..100f,
                valueLabel = "${config.topK}",
                onValueChange = { onTopKChange(it.toInt()) }
            )

            // Repeat Penalty
            ParamSlider(
                label = "Repeat Penalty",
                value = config.repeatPenalty.toFloat(),
                valueRange = 0.5f..2f,
                valueLabel = "%.2f".format(config.repeatPenalty),
                onValueChange = { onRepeatPenaltyChange(it.toDouble()) }
            )
        }
    }
}

@Composable
fun ParamSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
fun ModelInfoBar(model: OllamaModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            model.quantizationLevel?.let {
                AssistChip(
                    onClick = {},
                    label = { Text("Q: $it", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
            model.parameterSize?.let {
                AssistChip(
                    onClick = {},
                    label = { Text("Params: $it", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
            model.family?.let {
                Text(
                    "Family: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComparisonResultDialog(
    result: OllamaComparisonResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Compare, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Comparison Results")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Config used
                if (result.templateUsed != null) {
                    Text(
                        "Template: ${result.templateUsed.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    "Config: temp=${result.configUsed.temperature}, ctx=${result.configUsed.contextWindow}, " +
                            "max_tokens=${result.configUsed.maxTokens}, top_p=${result.configUsed.topP}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                // Stats comparison
                Text("Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Default", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("${result.defaultDurationMs}ms", style = MaterialTheme.typography.bodySmall)
                        Text("${result.defaultEvalCount} tokens", style = MaterialTheme.typography.labelSmall)
                        Text("%.1f t/s".format(result.defaultTokensPerSec), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Optimized", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("${result.optimizedDurationMs}ms", style = MaterialTheme.typography.bodySmall)
                        Text("${result.optimizedEvalCount} tokens", style = MaterialTheme.typography.labelSmall)
                        Text("%.1f t/s".format(result.optimizedTokensPerSec), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Divider()

                // Default response
                Text("Default Response", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        result.defaultResponse,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Optimized response
                Text("Optimized Response", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        result.optimizedResponse,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun InputBarWithCompare(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCompareClick: () -> Unit,
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
                placeholder = {
                    Text(if (enabled) "Type a message..." else "Connect to Ollama first...")
                },
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(4.dp))
            // Compare button
            IconButton(
                onClick = onCompareClick,
                enabled = enabled && text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Compare default vs optimized",
                    tint = if (enabled && text.isNotBlank()) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilledIconButton(
                onClick = onSendClick,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
    }
}

// --- Existing components preserved ---

@Composable
fun ConnectionStatusBar(
    isConnected: Boolean,
    isChecking: Boolean,
    version: String?,
    error: String?,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when {
            isChecking -> MaterialTheme.colorScheme.surfaceVariant
            isConnected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.errorContainer
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                    )
                }

                Column {
                    Text(
                        text = when {
                            isChecking -> "Connecting to Ollama..."
                            isConnected -> "Connected to Ollama"
                            else -> "Not Connected"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (isConnected && version != null) {
                        Text(
                            "Version: $version",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (!isConnected && error != null) {
                        Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (!isConnected && !isChecking) {
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun ModelSelectorBar(
    selectedModel: String,
    availableModels: List<OllamaModel>,
    isLoading: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onModelSelected: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Memory, "Model", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Model:", style = MaterialTheme.typography.labelMedium)
            }

            Box {
                OutlinedButton(onClick = { onExpandChange(true) }, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(selectedModel)
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { onExpandChange(false) }) {
                    if (availableModels.isEmpty()) {
                        DropdownMenuItem(text = { Text("No models available") }, onClick = {}, enabled = false)
                    } else {
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(model.displayName, fontWeight = if (model.name == selectedModel) FontWeight.Bold else FontWeight.Normal)
                                            if (model.name == selectedModel) {
                                                Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text(
                                            "${model.formattedSize} ${model.parameterSize ?: ""} ${model.quantizationLevel ?: ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                onClick = { onModelSelected(model.name) }
                            )
                        }
                    }

                    Divider()
                    DropdownMenuItem(
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(16.dp))
                                Text("Refresh Models")
                            }
                        },
                        onClick = { onRefresh(); onExpandChange(false) }
                    )
                }
            }
        }
    }
}

@Composable
fun InferenceStatsBar(inferenceTimeMs: Long, tokensPerSecond: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, "Speed", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Text("%.1f tokens/sec".format(tokensPerSecond), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Divider(modifier = Modifier.height(16.dp).width(1.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
            Text("Inference: ${inferenceTimeMs}ms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun TokenUsageBar(inputTokens: Int, outputTokens: Int, totalTokens: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TokenMetric("Prompt", inputTokens)
            Divider(modifier = Modifier.height(16.dp).width(1.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f))
            TokenMetric("Generated", outputTokens)
            Divider(modifier = Modifier.height(16.dp).width(1.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f))
            TokenMetric("Total", totalTokens)
        }
    }
}

@Composable
private fun TokenMetric(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
        Text(value.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
fun EmptyStateMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Memory, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Start chatting with your local LLM", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Use the settings panel to configure parameters and select prompt templates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Press the compare button to see default vs optimized results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun OllamaMessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                    message.tokenUsage?.let { usage ->
                        Text(
                            "${usage.outputTokens} tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            modifier = Modifier.widthIn(max = 120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Thinking...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
fun ErrorMessage(error: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onDismiss() },
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
