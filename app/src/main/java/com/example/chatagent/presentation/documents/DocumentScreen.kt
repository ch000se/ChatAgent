package com.example.chatagent.presentation.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Indexing") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDocumentDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            StatsCard(
                totalDocuments = uiState.totalDocuments,
                indexedDocuments = uiState.indexedDocuments
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchBar(
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSearch = { viewModel.searchDocuments() },
                isSearching = uiState.isSearching
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.searchResults.isNotEmpty()) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                SearchResultsList(
                    results = uiState.searchResults,
                    onClearSearch = { viewModel.clearSearch() }
                )
            } else {
                Text(
                    text = "Documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                DocumentsList(
                    documents = uiState.documents,
                    onIndexDocument = { viewModel.indexDocument(it) },
                    onDeleteDocument = { viewModel.deleteDocument(it) },
                    isIndexing = uiState.isIndexing,
                    indexingProgress = uiState.indexingProgress
                )
            }

            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }

        if (uiState.showAddDocumentDialog) {
            AddDocumentDialog(
                documentName = uiState.documentName,
                documentContent = uiState.documentContent,
                documentType = uiState.documentType,
                onNameChange = { viewModel.onDocumentNameChanged(it) },
                onContentChange = { viewModel.onDocumentContentChanged(it) },
                onTypeChange = { viewModel.onDocumentTypeChanged(it) },
                onDismiss = { viewModel.hideAddDocumentDialog() },
                onConfirm = { viewModel.addDocument() }
            )
        }
    }
}

@Composable
fun StatsCard(
    totalDocuments: Int,
    indexedDocuments: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalDocuments",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Documents",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$indexedDocuments",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Indexed",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search documents...") },
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSearch,
            enabled = !isSearching && searchQuery.isNotBlank()
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }
    }
}

@Composable
fun SearchResultsList(
    results: List<com.example.chatagent.domain.model.DocumentSearchResult>,
    onClearSearch: () -> Unit
) {
    Column {
        TextButton(onClick = onClearSearch) {
            Text("Clear Search")
        }

        LazyColumn {
            items(results) { result ->
                SearchResultItem(result = result)
            }
        }
    }
}

@Composable
fun SearchResultItem(result: com.example.chatagent.domain.model.DocumentSearchResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Rank: ${result.rank} | Similarity: ${"%.3f".format(result.similarity)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.document.fileName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.chunk.text.take(200) + if (result.chunk.text.length > 200) "..." else "",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DocumentsList(
    documents: List<com.example.chatagent.domain.model.Document>,
    onIndexDocument: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    isIndexing: Boolean,
    indexingProgress: com.example.chatagent.domain.model.IndexingProgress?
) {
    LazyColumn {
        items(documents) { document ->
            DocumentItem(
                document = document,
                onIndexClick = { onIndexDocument(document.id) },
                onDeleteClick = { onDeleteDocument(document.id) },
                isIndexing = isIndexing && indexingProgress?.documentId == document.id,
                indexingProgress = if (indexingProgress?.documentId == document.id) indexingProgress else null
            )
        }
    }
}

@Composable
fun DocumentItem(
    document: com.example.chatagent.domain.model.Document,
    onIndexClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isIndexing: Boolean,
    indexingProgress: com.example.chatagent.domain.model.IndexingProgress?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = document.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Type: ${document.contentType} | Size: ${document.fileSize} bytes",
                style = MaterialTheme.typography.bodySmall
            )

            if (document.indexed) {
                Text(
                    text = "Indexed (${document.chunkCount} chunks)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isIndexing && indexingProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { indexingProgress.processedChunks.toFloat() / indexingProgress.totalChunks.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = indexingProgress.currentStatus,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = onIndexClick,
                    enabled = !isIndexing && !document.indexed
                ) {
                    Text(if (document.indexed) "Re-index" else "Index")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun AddDocumentDialog(
    documentName: String,
    documentContent: String,
    documentType: String,
    onNameChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = documentName,
                    onValueChange = onNameChange,
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = documentContent,
                    onValueChange = onContentChange,
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = documentType,
                    onValueChange = onTypeChange,
                    label = { Text("Type (text/markdown/code)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
