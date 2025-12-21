package com.example.chatagent.presentation.mcp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.data.remote.dto.McpTool
import com.example.chatagent.domain.model.McpServers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpScreen(
    onNavigateBack: () -> Unit,
    viewModel: McpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showServerListDialog by remember { mutableStateOf(false) }
    var showToolExecutionDialog by remember { mutableStateOf(false) }

    if (showServerListDialog) {
        ServerListDialog(
            onDismiss = { showServerListDialog = false },
            onServerSelected = { server ->
                viewModel.updateServerUrl(server.url)
                showServerListDialog = false
            }
        )
    }

    uiState.selectedTool?.let { tool ->
        if (showToolExecutionDialog) {
            ToolExecutionDialog(
                tool = tool,
                isExecuting = uiState.isExecutingTool,
                onDismiss = {
                    showToolExecutionDialog = false
                    viewModel.selectTool(null)
                },
                onExecute = { arguments ->
                    viewModel.callTool(tool.name, arguments)
                    showToolExecutionDialog = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Tools") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.connectionState is McpClient.ConnectionState.Connected) {
                        IconButton(onClick = { viewModel.listTools() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh tools")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionStatusCard(
                connectionState = uiState.connectionState,
                isLoading = uiState.isLoading
            )

            if (uiState.connectionState is McpClient.ConnectionState.Disconnected) {
                OutlinedButton(
                    onClick = { showServerListDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select Public Server")
                }
            }

            OutlinedTextField(
                value = uiState.serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = { Text("MCP Server URL") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.connectionState is McpClient.ConnectionState.Disconnected,
                singleLine = true
            )

            Button(
                onClick = {
                    when (uiState.connectionState) {
                        is McpClient.ConnectionState.Connected -> viewModel.disconnect()
                        else -> viewModel.connect()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(
                    when (uiState.connectionState) {
                        is McpClient.ConnectionState.Connected -> "Disconnect"
                        is McpClient.ConnectionState.Connecting -> "Connecting..."
                        else -> "Connect"
                    }
                )
            }

            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            uiState.lastToolResult?.let { result ->
                ToolResultCard(
                    result = result,
                    onDismiss = { viewModel.clearToolResult() }
                )
            }

            if (uiState.tools.isNotEmpty()) {
                Text(
                    text = "Available Tools (${uiState.tools.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tools) { tool ->
                        ExecutableToolCard(
                            tool = tool,
                            onExecuteClick = { selectedTool ->
                                viewModel.selectTool(selectedTool)
                                showToolExecutionDialog = true
                            }
                        )
                    }
                }
            } else if (uiState.connectionState is McpClient.ConnectionState.Connected && !uiState.isLoading) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tools available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: McpClient.ConnectionState,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is McpClient.ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is McpClient.ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (connectionState) {
                is McpClient.ConnectionState.Connected -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${connectionState.serverInfo.name} v${connectionState.serverInfo.version}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                is McpClient.ConnectionState.Connecting -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Connecting...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                is McpClient.ConnectionState.Error -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Connection Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                is McpClient.ConnectionState.Disconnected -> {
                    Text(
                        text = "Not Connected",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ServerListDialog(
    onDismiss: () -> Unit,
    onServerSelected: (com.example.chatagent.domain.model.McpServerInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select MCP Server") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(McpServers.getPublicServers()) { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onServerSelected(server) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = server.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = server.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = server.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
