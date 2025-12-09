package com.example.chatagent.presentation.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatagent.domain.model.BenchmarkComparison
import com.example.chatagent.domain.model.BenchmarkResult
import com.example.chatagent.domain.model.ModelCategory
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    onNavigateBack: () -> Unit,
    viewModel: BenchmarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Benchmark") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PromptInput(
                prompt = uiState.prompt,
                onPromptChanged = viewModel::onPromptChanged,
                onRunClick = viewModel::runBenchmark,
                enabled = !uiState.isRunning
            )

            if (uiState.isRunning) {
                LoadingIndicator()
            }

            uiState.error?.let { error ->
                ErrorCard(error = error)
            }

            uiState.comparison?.let { comparison ->
                BenchmarkResults(comparison = comparison)
            }
        }
    }
}

@Composable
fun PromptInput(
    prompt: String,
    onPromptChanged: (String) -> Unit,
    onRunClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Test Prompt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your prompt here...") },
                enabled = enabled,
                minLines = 3,
                maxLines = 5
            )

            Button(
                onClick = onRunClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Run Benchmark")
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Running benchmark...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun BenchmarkResults(comparison: BenchmarkComparison) {
    val decimalFormat = DecimalFormat("#.##")

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryCard(comparison, decimalFormat)
        }

        item {
            Text(
                text = "Model Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(comparison.results) { result ->
            ModelResultCard(result, decimalFormat)
        }
    }
}

@Composable
fun SummaryCard(comparison: BenchmarkComparison, decimalFormat: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Divider()

            InfoRow(
                label = "Total Models:",
                value = "${comparison.results.size}"
            )
            InfoRow(
                label = "Successful:",
                value = "${comparison.successCount}"
            )
            InfoRow(
                label = "Total Duration:",
                value = "${decimalFormat.format(comparison.totalDurationMs / 1000.0)}s"
            )
            InfoRow(
                label = "Total Cost:",
                value = "$${decimalFormat.format(comparison.totalCost)}"
            )

            comparison.fastestResult?.let { fastest ->
                InfoRow(
                    label = "Fastest:",
                    value = "${fastest.modelInfo.name} (${decimalFormat.format(fastest.responseTimeSec)}s)"
                )
            }

            comparison.cheapestResult?.let { cheapest ->
                InfoRow(
                    label = "Cheapest:",
                    value = "${cheapest.modelInfo.name} ($${decimalFormat.format(cheapest.estimatedCost)})"
                )
            }
        }
    }
}

@Composable
fun ModelResultCard(result: BenchmarkResult, decimalFormat: DecimalFormat) {
    val categoryColor = when (result.modelInfo.category) {
        ModelCategory.SMALL -> MaterialTheme.colorScheme.tertiaryContainer
        ModelCategory.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        ModelCategory.LARGE -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = categoryColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = result.modelInfo.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = result.modelInfo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            if (result.isSuccess) {
                InfoRow(
                    label = "Response Time:",
                    value = "${decimalFormat.format(result.responseTimeSec)}s"
                )
                InfoRow(
                    label = "Tokens:",
                    value = "${result.totalTokens} (in: ${result.inputTokens}, out: ${result.outputTokens})"
                )
                InfoRow(
                    label = "Est. Cost:",
                    value = "$${decimalFormat.format(result.estimatedCost)}"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Response Preview:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = result.response.take(200) + if (result.response.length > 200) "..." else "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    text = "Error: ${result.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}