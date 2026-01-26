package com.example.chatagent.presentation.analyst

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatagent.domain.model.AnalystFile
import com.example.chatagent.domain.model.AnalystFileMetadata
import com.example.chatagent.domain.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalystScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AnalystViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showModelSelector by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFilePicked(it, context.contentResolver) }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Local Analyst")
                        Text(
                            text = uiState.loadedFile?.let { "File: ${it.fileName}" } ?: "No file loaded",
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
                    IconButton(onClick = { viewModel.checkConnection() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearConversation() }) {
                            Icon(Icons.Default.Delete, "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
            AnalystConnectionBar(
                isConnected = uiState.isConnected,
                isChecking = uiState.isCheckingConnection,
                version = uiState.ollamaVersion,
                error = uiState.connectionError,
                onRetry = { viewModel.checkConnection() }
            )

            // Model Selector
            AnalystModelSelector(
                selectedModel = selectedModel,
                availableModels = uiState.availableModels,
                expanded = showModelSelector,
                isLoading = uiState.isLoadingModels,
                onExpandChange = { showModelSelector = it },
                onModelSelected = { model ->
                    viewModel.selectModel(model)
                    showModelSelector = false
                },
                onRefresh = { viewModel.loadModels() }
            )

            // File Picker Card
            FilePickerCard(
                loadedFile = uiState.loadedFile,
                isLoading = uiState.isParsingFile,
                fileError = uiState.fileError,
                onPickFile = {
                    filePickerLauncher.launch(arrayOf(
                        "text/csv",
                        "text/comma-separated-values",
                        "application/json",
                        "text/plain"
                    ))
                },
                onClearFile = { viewModel.clearFile() },
                onLoadSample = { fileName ->
                    viewModel.loadSampleFile(context.assets, fileName)
                }
            )

            // Data Preview
            if (uiState.loadedFile != null) {
                DataPreviewCard(file = uiState.loadedFile!!)
            }

            // Context Window Slider
            if (uiState.loadedFile != null) {
                ContextWindowSlider(
                    value = uiState.generationConfig.contextWindow,
                    onValueChange = { viewModel.updateContextWindow(it) }
                )
            }

            // Inference Stats
            if (uiState.lastInferenceTimeMs > 0) {
                AnalystStatsBar(
                    inferenceTimeMs = uiState.lastInferenceTimeMs,
                    tokensPerSecond = uiState.tokensPerSecond
                )
            }

            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isEmpty() && uiState.loadedFile != null && !uiState.isLoading) {
                    item { AnalystEmptyState() }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    AnalystMessageBubble(message = message)
                }

                if (uiState.isLoading) {
                    item { AnalystLoadingIndicator() }
                }
            }

            // Error
            uiState.error?.let { error ->
                AnalystErrorBar(error = error, onDismiss = { viewModel.clearError() })
            }

            // Input Bar
            AnalystInputBar(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendMessage,
                enabled = !uiState.isLoading && uiState.isConnected && uiState.loadedFile != null
            )
        }
    }
}

@Composable
fun AnalystConnectionBar(
    isConnected: Boolean,
    isChecking: Boolean,
    version: String?,
    error: String?,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isConnected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                CircleShape
                            )
                    )
                }
                Column {
                    Text(
                        if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (isConnected && version != null) {
                        Text("v$version", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    if (!isConnected && error != null) {
                        Text(error, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
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
fun AnalystModelSelector(
    selectedModel: String,
    availableModels: List<com.example.chatagent.domain.model.OllamaModel>,
    expanded: Boolean,
    isLoading: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onModelSelected: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        DropdownMenuItem(text = { Text("No models") }, onClick = {}, enabled = false)
                    } else {
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(model.displayName, fontWeight = if (model.name == selectedModel) FontWeight.Bold else FontWeight.Normal)
                                        if (model.name == selectedModel) {
                                            Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
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
fun FilePickerCard(
    loadedFile: AnalystFile?,
    isLoading: Boolean,
    fileError: String?,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onLoadSample: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (loadedFile != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Parsing file...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (loadedFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Column {
                            Text(loadedFile.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${loadedFile.fileType.displayName} | ${formatFileSize(loadedFile.rawSize)} | ~${loadedFile.tokenEstimate} tokens",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (loadedFile.wasTruncated) {
                                Text("Truncated to fit context", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Row {
                        IconButton(onClick = onPickFile) {
                            Icon(Icons.Default.FolderOpen, "Change file", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onClearFile) {
                            Icon(Icons.Default.Close, "Remove file", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                // Metadata summary
                when (val meta = loadedFile.metadata) {
                    is AnalystFileMetadata.CsvMetadata -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Columns: ${meta.headers.joinToString(", ")} | Rows: ${meta.includedRows}/${meta.rowCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 2
                        )
                    }
                    is AnalystFileMetadata.JsonMetadata -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        val info = buildString {
                            append("Type: ${meta.topLevelType}")
                            meta.elementCount?.let { append(" | Elements: $it") }
                            meta.topLevelKeys?.let { append(" | Keys: ${it.take(5).joinToString(", ")}") }
                        }
                        Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 2)
                    }
                    is AnalystFileMetadata.LogMetadata -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Lines: ${meta.includedLines}/${meta.lineCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                // No file loaded
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Load a file to analyze", style = MaterialTheme.typography.bodyMedium)
                    Text("CSV, JSON, or Log/Text files", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onPickFile) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose File")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Or try a sample:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onLoadSample("sample_errors.csv") }) {
                            Text("Errors.csv", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onLoadSample("sample_users.json") }) {
                            Text("Users.json", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onLoadSample("sample_app.log") }) {
                            Text("App.log", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (fileError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(fileError, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun DataPreviewCard(file: AnalystFile) {
    var expanded by remember { mutableStateOf(false) }
    val previewLines = file.parsedContent.lines().take(20)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Data Preview", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Toggle preview",
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = previewLines.joinToString("\n"),
                            modifier = Modifier.padding(8.dp).horizontalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 20
                        )
                    }
                    if (file.parsedContent.lines().size > 20) {
                        Text(
                            "... ${file.parsedContent.lines().size - 20} more lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContextWindowSlider(value: Int, onValueChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Context Window", style = MaterialTheme.typography.labelSmall)
                Text("$value tokens", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 1024f..8192f,
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
fun AnalystStatsBar(inferenceTimeMs: Long, tokensPerSecond: Double) {
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
                Text("%.1f t/s".format(tokensPerSecond), style = MaterialTheme.typography.labelMedium)
            }
            Divider(modifier = Modifier.height(16.dp).width(1.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
            Text("${inferenceTimeMs}ms", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AnalystEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Description, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("File loaded! Ask a question", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Examples: \"What errors are most common?\", \"Summarize the data\", \"Find patterns\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun AnalystMessageBubble(message: Message) {
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
                message.tokenUsage?.let { usage ->
                    Spacer(modifier = Modifier.height(4.dp))
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

@Composable
fun AnalystLoadingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            modifier = Modifier.widthIn(max = 140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Analyzing...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
fun AnalystErrorBar(error: String, onDismiss: () -> Unit) {
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

@Composable
fun AnalystInputBar(
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
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (!enabled && text.isEmpty()) "Load a file and connect to start..."
                        else "Ask about your data..."
                    )
                },
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
