# ChatAgent - AI Assistant with RAG and MCP

## Опис проекту

ChatAgent - це Android додаток-асистент з підтримкою:
- 🤖 Claude AI API для розумних відповідей
- 📚 RAG (Retrieval-Augmented Generation) для пошуку в документації
- 🔧 MCP (Model Context Protocol) для інтеграції з git
- 💬 Командна система для швидкого доступу до функцій

## Архітектура

Проект побудований за принципами **Clean Architecture** та **MVVM**:

```
├── presentation/     # UI Layer (Jetpack Compose)
│   ├── chat/        # Основний екран чату
│   └── ragchat/     # RAG-чат з пошуком документів
├── domain/          # Business Logic
│   ├── model/       # Domain моделі
│   ├── repository/  # Repository інтерфейси
│   ├── usecase/     # Use cases
│   └── command/     # Командна система
└── data/            # Data Layer
    ├── local/       # Room Database
    ├── remote/      # API та MCP клієнти
    └── repository/  # Repository імплементації
```

## Основні компоненти

### 1. RAG System (Retrieval-Augmented Generation)

**Файли:**
- `data/repository/DocumentRepositoryImpl.kt`
- `domain/usecase/SearchDocumentsUseCase.kt`
- `domain/usecase/IndexProjectDocumentsUseCase.kt`

**Принцип роботи:**
1. Документи індексуються при запуску додатку
2. Використовується TF-IDF векторизація (384 виміри)
3. Cosine similarity для пошуку релевантних документів
4. Reranking для підвищення точності

**Використання в коді:**
```kotlin
// Пошук документів
val results = searchDocumentsUseCase(query = "RAG", topK = 5)

// Індексування
indexProjectDocumentsUseCase().collect { status ->
    when (status) {
        is IndexingStatus.Completed -> // Done
    }
}
```

### 2. MCP Integration (Model Context Protocol)

**Файли:**
- `data/remote/client/McpClient.kt`
- `domain/command/GitCommandHandler.kt`

**Підтримувані git команди:**
- `git status` - поточний стан репозиторію
- `git log` - історія комітів
- `git diff` - зміни в коді
- `git branch` - список гілок

**Використання в коді:**
```kotlin
// Виклик git команди через MCP
val result = mcpClient.callTool(
    toolName = "execute_command",
    arguments = mapOf(
        "command" to "git",
        "args" to listOf("status", "--short")
    )
)
```

### 3. Command System

**Файли:**
- `domain/model/Command.kt`
- `domain/command/CommandDispatcher.kt`
- `domain/command/HelpCommandHandler.kt`
- `domain/command/GitCommandHandler.kt`

**Доступні команди:**
- `/help [query]` - пошук в документації проекту
- `/code [query]` - пошук в коді проекту
- `/docs [query]` - пошук тільки в .md файлах
- `/git [subcommand]` - git операції через MCP

**Використання в коді:**
```kotlin
// Парсинг команди
val command = CommandParser.parse("/help RAG")

// Виконання
val result = commandDispatcher.dispatch(command)
```

## API та Data Schemas

### Message Model
```kotlin
data class Message(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val tokenUsage: TokenUsage?,
    val sources: List<DocumentSearchResult>?,
    val isCommand: Boolean,
    val commandMetadata: CommandMetadata?
)
```

### Document Model
```kotlin
data class Document(
    val id: String,
    val fileName: String,
    val content: String,
    val mimeType: String,
    val isIndexed: Boolean,
    val createdAt: Long
)
```

### Room Database Schema

**Entities:**
1. **MessageEntity** - історія повідомлень
2. **DocumentEntity** - збережені документи
3. **EmbeddingEntity** - векторні представлення для RAG

**Міграції:** Використовується fallbackToDestructiveMigration

## Dependency Injection (Hilt)

Всі компоненти інжектяться через Hilt:

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val commandDispatcher: CommandDispatcher
)

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService,
    private val messageDao: MessageDao,
    private val mcpClient: McpClient
)
```

## Налаштування та конфігурація

### API Keys

Додайте в `local.properties`:
```properties
CLAUDE_API_KEY=your_api_key_here
```

### MCP Server

Для роботи `/git` команд потрібен запущений MCP Server. Див. `MCP_SETUP_GUIDE.md`

## Тестування

### Тестування RAG:
1. Запустіть додаток
2. Введіть `/help RAG` або `/docs vectorization`
3. Перевірте similarity scores та sources

### Тестування MCP:
1. Запустіть MCP Server
2. В додатку введіть `/git status`
3. Перевірте вивід git команди

## Правила стилю коду

### Naming Conventions
- **Classes:** PascalCase (ChatViewModel, MessageEntity)
- **Functions:** camelCase (sendMessage, getAllMessages)
- **Constants:** UPPER_SNAKE_CASE (MAX_HISTORY_MESSAGES)
- **Packages:** lowercase (com.example.chatagent.domain)

### Architecture Rules
- ✅ Domain layer НЕ залежить від data/presentation
- ✅ Repository pattern для всіх data sources
- ✅ UseCase для кожної бізнес-операції
- ✅ StateFlow для reactive UI updates
- ✅ Sealed classes для modeling outcomes

### Compose Guidelines
- Використовуйте `@Composable` functions для UI
- State hoisting - стан в ViewModel
- Material3 design system
- Prefer `remember` та `derivedStateOf` для performance

### Error Handling
```kotlin
suspend fun operation(): Result<Data> {
    return try {
        Result.success(data)
    } catch (e: Exception) {
        Log.e(TAG, "Error", e)
        Result.failure(e)
    }
}
```

## Корисні фрагменти коду

### Додавання нової команди:
```kotlin
// 1. Додати в Command.kt
data class MyCommand(
    override val rawInput: String,
    val param: String
) : Command()

// 2. Створити handler
class MyCommandHandler @Inject constructor() : CommandHandler<Command.MyCommand> {
    override suspend fun handle(command: Command.MyCommand): CommandResult {
        // Implementation
    }
}

// 3. Додати в CommandDispatcher
when (command) {
    is Command.MyCommand -> myCommandHandler.handle(command)
}
```

### RAG індексування нового джерела:
```kotlin
val result = documentRepository.addDocument(
    fileName = "my_doc.txt",
    content = "Document content",
    mimeType = "text/plain"
)

result.onSuccess { document ->
    documentRepository.indexDocument(document.id).collect { }
}
```

## Логування

Всі компоненти використовують Android Log:
```kotlin
private val TAG = "ComponentName"
Log.d(TAG, "Debug message")
Log.e(TAG, "Error message", exception)
```

Для перегляду логів:
```bash
adb logcat | grep "ChatAgent"
```

## Performance Tips

1. **RAG Search:** Використовуйте topK=5 для швидкості
2. **MCP Calls:** Встановіть timeout для git операцій
3. **Room Queries:** Індексуйте часто використовувані поля
4. **Compose:** Використовуйте keys в LazyColumn items

## Troubleshooting

**Проблема:** "MCP not connected"
**Рішення:** Перевірте чи запущений MCP Server

**Проблема:** "Similarity = 0.0000"
**Рішення:** Дочекайтесь завершення індексування документів

**Проблема:** Build errors
**Рішення:** Sync Gradle, Invalidate Caches, Clean Project

## Версії

- Kotlin: 1.9.0
- Compose: 2024.04.01
- Hilt: 2.48
- Room: 2.6.1
- Retrofit: 2.9.0

## Автор

ChatAgent розроблено з використанням Claude AI
