# ChatAgent API Reference

## Claude API Integration

### ChatApiService

**Location:** `data/remote/api/ChatApiService.kt`

```kotlin
interface ChatApiService {
    @POST("v1/messages")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
}
```

### Request/Response Models

#### ChatRequest
```kotlin
data class ChatRequest(
    val model: String = "claude-3-5-sonnet-20241022",
    val messages: List<MessageDto>,
    val maxTokens: Int = 2048,
    val temperature: Double = 1.0,
    val system: String? = null,
    val tools: List<ClaudeToolDto>? = null
)
```

#### ChatResponse
```kotlin
data class ChatResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stopReason: String?,
    val usage: UsageDto?
)
```

#### ContentBlock
```kotlin
data class ContentBlock(
    val type: String,  // "text", "tool_use", "tool_result"
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: Map<String, Any>? = null,
    val toolUseId: String? = null,
    val content: Any? = null
)
```

## Repository Pattern

### ChatRepository

**Interface:** `domain/repository/ChatRepository.kt`

```kotlin
interface ChatRepository {
    // Message operations
    fun getAllMessages(): Flow<List<Message>>
    suspend fun sendMessage(message: String): Result<Message>
    fun clearConversationHistory()

    // Configuration
    fun setSystemPrompt(prompt: String)
    fun getSystemPrompt(): StateFlow<String>
    fun setTemperature(temperature: Double)
    fun getTemperature(): StateFlow<Double>

    // Summarization
    fun setSummarizationConfig(config: SummarizationConfig)
    fun getSummarizationConfig(): StateFlow<SummarizationConfig>
    fun getSummarizationStats(): StateFlow<SummarizationStats>
}
```

**Implementation:** `data/repository/ChatRepositoryImpl.kt`

**Key Methods:**

- `sendMessage()`: Відправляє повідомлення до Claude API з підтримкою tool use loop
- `compressConversationHistory()`: Автоматично стискає історію через summarization
- `getAvailableMcpTools()`: Отримує доступні MCP інструменти

### DocumentRepository

**Interface:** `domain/repository/DocumentRepository.kt`

```kotlin
interface DocumentRepository {
    // CRUD operations
    suspend fun addDocument(fileName: String, content: String, mimeType: String): Result<Document>
    suspend fun getDocument(id: String): Document?
    suspend fun getDocumentByFileName(fileName: String): Document?
    suspend fun getAllDocuments(): List<Document>
    suspend fun deleteDocument(id: String)

    // Indexing
    suspend fun indexDocument(documentId: String): Flow<IndexingProgress>

    // Search
    suspend fun searchDocuments(query: String, topK: Int): List<DocumentSearchResult>
}
```

**Implementation:** `data/repository/DocumentRepositoryImpl.kt`

**Key Methods:**

- `indexDocument()`: Векторизує документ через TF-IDF
- `searchDocuments()`: Пошук через cosine similarity
- `rerankResults()`: Reranking для підвищення точності

## Use Cases

### SendMessageUseCase

**Location:** `domain/usecase/SendMessageUseCase.kt`

```kotlin
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String): Result<Message> {
        return chatRepository.sendMessage(message)
    }
}
```

### SearchDocumentsUseCase

**Location:** `domain/usecase/SearchDocumentsUseCase.kt`

```kotlin
class SearchDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(query: String, topK: Int = 5): List<DocumentSearchResult> {
        return documentRepository.searchDocuments(query, topK)
    }
}
```

### IndexProjectDocumentsUseCase

**Location:** `domain/usecase/IndexProjectDocumentsUseCase.kt`

```kotlin
class IndexProjectDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val documentScanner: ProjectDocumentScanner
) {
    suspend operator fun invoke(): Flow<IndexingStatus>
}
```

**Indexing Status:**
```kotlin
sealed class IndexingStatus {
    object Scanning : IndexingStatus()
    data class Found(val count: Int) : IndexingStatus()
    data class Indexing(val current: Int, val total: Int, val fileName: String) : IndexingStatus()
    data class Completed(val indexed: Int, val skipped: Int) : IndexingStatus()
}
```

## Command System API

### CommandDispatcher

**Location:** `domain/command/CommandDispatcher.kt`

```kotlin
@Singleton
class CommandDispatcher @Inject constructor(
    private val helpHandler: HelpCommandHandler,
    private val codeHandler: CodeSearchCommandHandler,
    private val docsHandler: DocsCommandHandler,
    private val gitHandler: GitCommandHandler
) {
    suspend fun dispatch(command: Command): CommandResult
}
```

### Command Models

```kotlin
sealed class Command {
    abstract val rawInput: String

    data class Help(override val rawInput: String, val query: String) : Command()
    data class Code(override val rawInput: String, val query: String) : Command()
    data class Docs(override val rawInput: String, val query: String) : Command()
    data class Git(override val rawInput: String, val subcommand: GitSubcommand) : Command()
    data class Unknown(override val rawInput: String) : Command()
}
```

### CommandResult

```kotlin
data class CommandResult(
    val command: Command,
    val content: String,
    val success: Boolean,
    val metadata: CommandMetadata? = null,
    val error: String? = null
)

data class CommandMetadata(
    val sources: List<DocumentSearchResult>? = null,
    val executionTimeMs: Long? = null,
    val commandType: String,
    val matchCount: Int? = null
)
```

## MCP Client API

### McpClient

**Location:** `data/remote/client/McpClient.kt`

```kotlin
@Singleton
class McpClient @Inject constructor() {
    val connectionState: StateFlow<ConnectionState>
    val tools: StateFlow<List<McpTool>>

    suspend fun connect(config: McpConnectionConfig)
    suspend fun disconnect()
    suspend fun callTool(toolName: String, arguments: Map<String, Any>): Result<ToolCallResult>
}
```

### Connection States

```kotlin
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val serverInfo: ServerInfo) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
```

### Tool Call

```kotlin
data class ToolCallResult(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

data class ToolContent(
    val type: String,
    val text: String? = null,
    val data: Any? = null
)
```

## Database Schema (Room)

### MessageEntity

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val jsonResponse: String?,
    val tokenUsageJson: String?,
    val isSummary: Boolean,
    val summarizedMessageCount: Int?,
    val originalTokenCount: Int?,
    val isCommand: Boolean,
    val commandMetadataJson: String?
)
```

### DocumentEntity

```kotlin
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val content: String,
    val mimeType: String,
    val isIndexed: Boolean,
    val createdAt: Long
)
```

### EmbeddingEntity

```kotlin
@Entity(
    tableName = "embeddings",
    foreignKeys = [ForeignKey(
        entity = DocumentEntity::class,
        parentColumns = ["id"],
        childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EmbeddingEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val embedding: String,  // JSON array of floats
    val chunkIndex: Int,
    val chunkText: String
)
```

## ViewModel API

### ChatViewModel

**Location:** `presentation/chat/ChatViewModel.kt`

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val commandDispatcher: CommandDispatcher
) : ViewModel() {

    val uiState: StateFlow<ChatUiState>
    val currentSystemPrompt: StateFlow<String>
    val currentTemperature: StateFlow<Double>

    fun onInputTextChanged(text: String)
    fun sendMessage()
    fun clearConversation()
    fun setSystemPrompt(prompt: String)
    fun setTemperature(temperature: Double)
    fun toggleSummarization(enabled: Boolean)
}
```

### ChatUiState

```kotlin
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalTokens: Int = 0,
    val currentTemperature: Double = 1.0,
    val summarizationEnabled: Boolean = false,
    val totalSummarizations: Int = 0,
    val tokensSaved: Int = 0,
    val compressionRatio: Double = 0.0
)
```

## Error Handling

Всі асинхронні операції повертають `Result<T>`:

```kotlin
// Success
Result.success(data)

// Failure
Result.failure(exception)

// Usage
result.onSuccess { data ->
    // Handle success
}.onFailure { exception ->
    // Handle error
}
```

## Token Usage Tracking

```kotlin
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val cacheCreationInputTokens: Int? = null,
    val cacheReadInputTokens: Int? = null
)
```

## RAG Search Results

```kotlin
data class DocumentSearchResult(
    val document: Document,
    val similarity: Double,
    val matchedChunks: List<String>
)
```

## Приклади використання API

### Відправка повідомлення з обробкою результату

```kotlin
viewModelScope.launch {
    sendMessageUseCase(messageText)
        .onSuccess { response ->
            // Update UI with response
            _uiState.update {
                it.copy(messages = it.messages + response)
            }
        }
        .onFailure { error ->
            // Show error
            _uiState.update {
                it.copy(error = error.message)
            }
        }
}
```

### Пошук в документації

```kotlin
val results = searchDocumentsUseCase(
    query = "How to implement RAG?",
    topK = 5
)

results.forEach { result ->
    println("${result.document.fileName}: ${result.similarity}")
}
```

### Виконання git команди через MCP

```kotlin
val result = mcpClient.callTool(
    toolName = "execute_command",
    arguments = mapOf(
        "command" to "git",
        "args" to listOf("status", "--short")
    )
)

result.onSuccess { toolResult ->
    val output = toolResult.content.first().text
    println(output)
}
```

### Індексування документа

```kotlin
documentRepository.addDocument(
    fileName = "guide.md",
    content = fileContent,
    mimeType = "text/markdown"
).onSuccess { document ->
    documentRepository.indexDocument(document.id).collect { progress ->
        when (progress) {
            is IndexingProgress.Processing -> println("Processing...")
            is IndexingProgress.Completed -> println("Done!")
        }
    }
}
```
