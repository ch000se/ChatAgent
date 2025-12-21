# MCP Pipeline Agent - Реализация

## Обзор проекта

MCP Pipeline Agent - это система автоматической оркестрации нескольких MCP (Model Context Protocol) серверов в единую цепочку выполнения. Агент позволяет связывать различные инструменты (поиск, обработка, сохранение) в автоматизированный пайплайн.

## Архитектура

### 1. Модели данных (`domain/model/Pipeline.kt`)

**PipelineStep** - шаг пайплайна
```kotlin
data class PipelineStep(
    val name: String,              // Название шага
    val serverUrl: String,         // URL MCP сервера
    val toolName: String,          // Имя инструмента на сервере
    val arguments: Map<String, Any>, // Аргументы для инструмента
    val order: Int,                // Порядок выполнения
    val status: StepStatus         // Статус: PENDING, IN_PROGRESS, COMPLETED, FAILED
)
```

**PipelineConfig** - конфигурация пайплайна
```kotlin
data class PipelineConfig(
    val name: String,              // Название пайплайна
    val description: String,       // Описание
    val steps: List<PipelineStep>, // Список шагов
    val status: PipelineStatus     // READY, RUNNING, COMPLETED, FAILED
)
```

**PipelineExecutionResult** - результат выполнения
```kotlin
data class PipelineExecutionResult(
    val stepResults: List<StepExecutionResult>, // Результаты каждого шага
    val finalOutput: String?,                   // Финальный результат
    val totalDuration: Long                     // Общее время выполнения
)
```

### 2. Мульти-MCP клиент (`data/remote/client/MultiMcpClient.kt`)

**Функционал:**
- ✅ Управление несколькими MCP серверами одновременно
- ✅ Поддержка сессий для каждого сервера
- ✅ Автоматическое получение списка инструментов
- ✅ Отслеживание состояния подключения

**Ключевые методы:**
```kotlin
// Подключение к серверу
suspend fun connectToServer(serverUrl: String): Result<ServerInfo>

// Получение списка инструментов
suspend fun listToolsForServer(serverUrl: String): Result<List<McpTool>>

// Вызов инструмента
suspend fun callTool(
    serverUrl: String,
    toolName: String,
    arguments: Map<String, Any>
): Result<CallToolResult>
```

**StateFlow для мониторинга:**
```kotlin
val serverStates: StateFlow<Map<String, ServerState>>
```

### 3. Агент-оркестратор (`domain/usecase/ExecutePipelineUseCase.kt`)

**Основной функционал:**

1. **Подключение к серверам**
   - Автоматически подключается к всем необходимым MCP серверам
   - Проверяет доступность инструментов

2. **Последовательное выполнение шагов**
   - Выполняет шаги по порядку (по полю `order`)
   - Передает результат предыдущего шага следующему

3. **Обработка ошибок**
   - При ошибке на любом шаге останавливает пайплайн
   - Сохраняет промежуточные результаты

4. **Реал-тайм прогресс**
   - Отправляет обновления через Flow
   - Трекинг времени выполнения каждого шага

**Flow обновлений:**
```kotlin
sealed class PipelineProgress {
    data class Started(val config: PipelineConfig)
    data class ConnectingToServers(val serverUrls: List<String>)
    data class ServersConnected(val serverUrls: List<String>)
    data class StepStarted(val step: PipelineStep)
    data class StepCompleted(val step: PipelineStep, val result: StepExecutionResult)
    data class StepFailed(val step: PipelineStep, val error: String)
    data class Completed(val config: PipelineConfig, val result: PipelineExecutionResult)
    data class Failed(val config: PipelineConfig, val error: String)
}
```

**Передача данных между шагами:**
```kotlin
// Использование результата предыдущего шага
val arguments = mapOf(
    "input" to "\${PREVIOUS_OUTPUT}"  // Заменяется на реальный результат
)
```

### 4. ViewModel (`presentation/pipeline/PipelineViewModel.kt`)

**Управление состоянием:**
```kotlin
data class PipelineUiState(
    val availablePipelines: List<PipelineConfig>,    // Доступные пайплайны
    val selectedPipeline: PipelineConfig?,           // Выбранный пайплайн
    val isExecuting: Boolean,                        // Выполняется ли сейчас
    val executionProgress: List<ProgressMessage>,    // Лог выполнения
    val executionResult: PipelineExecutionResult?,   // Результат
    val serverStates: Map<String, ServerState>,      // Состояния серверов
    val error: String?                               // Ошибка
)
```

**Методы:**
- `executePipeline(config: PipelineConfig)` - запуск пайплайна
- `selectPipeline(pipeline: PipelineConfig?)` - выбор пайплайна
- `clearResults()` - очистка результатов
- `disconnectAll()` - отключение от всех серверов

### 5. UI (`presentation/pipeline/PipelineScreen.kt`)

**Компоненты экрана:**

1. **PipelineCard** - карточка пайплайна
   - Показывает название и описание
   - Раскрывает список шагов при выборе
   - Кнопка "Run" для запуска

2. **PipelineStepItem** - шаг пайплайна
   - Номер шага
   - Название и описание
   - Имя инструмента (toolName)

3. **ProgressMessageItem** - сообщение прогресса
   - Временная метка
   - Текст сообщения
   - Монопространный шрифт для логов

4. **ExecutionResultCard** - финальный результат
   - Зеленая карточка с галочкой
   - Отображение финального вывода

5. **ErrorCard** - отображение ошибки
   - Красная карточка с иконкой ошибки
   - Детали ошибки

## Примеры пайплайнов

### 1. Web Search & Save Pipeline

**Описание:** Ищет статьи в интернете, создает краткое резюме и сохраняет в файл

**Шаги:**
```kotlin
PipelineConfig(
    name = "Web Search & Save",
    description = "Search the web, create a summary, and save to file",
    steps = listOf(
        // Шаг 1: Поиск в интернете
        PipelineStep(
            name = "Search Web",
            serverUrl = "http://10.0.2.2:3000",  // Brave Search MCP
            toolName = "brave_web_search",
            arguments = mapOf(
                "query" to "latest AI developments 2024",
                "count" to 3
            ),
            order = 1
        ),

        // Шаг 2: Суммаризация (примечание: нужен LLM MCP сервер)
        PipelineStep(
            name = "Create Summary",
            serverUrl = "http://10.0.2.2:3000",
            toolName = "summarize",
            arguments = mapOf(
                "text" to "\${PREVIOUS_OUTPUT}",  // Использует результаты поиска
                "max_length" to 500
            ),
            order = 2
        ),

        // Шаг 3: Сохранение в файл
        PipelineStep(
            name = "Save to File",
            serverUrl = "http://10.0.2.2:3001",  // File System MCP
            toolName = "write_file",
            arguments = mapOf(
                "path" to "/sdcard/Download/ai_summary.txt",
                "content" to "\${PREVIOUS_OUTPUT}"  // Использует резюме
            ),
            order = 3
        )
    )
)
```

**Поток данных:**
```
Поисковый запрос
    ↓ (Brave Search MCP)
3 статьи с описаниями
    ↓ (Суммаризация)
Краткое резюме (500 символов)
    ↓ (File System MCP)
Файл: /sdcard/Download/ai_summary.txt
```

### 2. Web Scraper Pipeline

**Описание:** Загружает веб-страницу, извлекает данные и экспортирует в JSON

**Шаги:**
```kotlin
PipelineConfig(
    name = "Web Scraper Pipeline",
    description = "Fetch web content, extract data, and export to JSON",
    steps = listOf(
        // Шаг 1: Загрузка страницы
        PipelineStep(
            name = "Fetch Web Page",
            serverUrl = "http://10.0.2.2:3002",
            toolName = "fetch_url",
            arguments = mapOf("url" to "https://example.com"),
            order = 1
        ),

        // Шаг 2: Извлечение данных
        PipelineStep(
            name = "Extract Data",
            serverUrl = "http://10.0.2.2:3002",
            toolName = "extract_data",
            arguments = mapOf(
                "html" to "\${PREVIOUS_OUTPUT}",
                "selector" to "article"
            ),
            order = 2
        ),

        // Шаг 3: Экспорт в JSON
        PipelineStep(
            name = "Export to JSON",
            serverUrl = "http://10.0.2.2:3001",
            toolName = "write_file",
            arguments = mapOf(
                "path" to "/sdcard/Download/scraped_data.json",
                "content" to "\${PREVIOUS_OUTPUT}"
            ),
            order = 3
        )
    )
)
```

## Необходимые MCP серверы

### Минимум 2 сервера для демо:

1. **Brave Search MCP** (порт 3000)
   - Инструмент: `brave_web_search`
   - Для поиска информации в интернете

2. **File System MCP** (порт 3001)
   - Инструменты: `write_file`, `read_file`, `list_directory`
   - Для работы с файлами

### Опциональные серверы:

3. **LLM MCP** - для суммаризации и обработки текста
4. **Web Scraper MCP** - для парсинга веб-страниц
5. **Google Drive MCP** - для сохранения в облако
6. **Database MCP** - для работы с базами данных

## Как это работает

### 1. Пользователь выбирает пайплайн
```
User taps on "Web Search & Save" → selectPipeline() вызывается
```

### 2. Пользователь запускает пайплайн
```
User taps "Run" → executePipeline() вызывается
```

### 3. Агент начинает выполнение

**Шаг 3.1: Подключение к серверам**
```kotlin
emit(PipelineProgress.ConnectingToServers(["http://10.0.2.2:3000", "http://10.0.2.2:3001"]))

for (serverUrl in serverUrls) {
    multiMcpClient.connectToServer(serverUrl)
}

emit(PipelineProgress.ServersConnected(serverUrls))
```

**Шаг 3.2: Выполнение шага 1 (Поиск)**
```kotlin
emit(PipelineProgress.StepStarted(step1))

val result = multiMcpClient.callTool(
    serverUrl = "http://10.0.2.2:3000",
    toolName = "brave_web_search",
    arguments = mapOf("query" to "latest AI developments 2024", "count" to 3)
)

previousOutput = extractOutput(result)  // "Article 1: ...\nArticle 2: ..."

emit(PipelineProgress.StepCompleted(step1, result))
```

**Шаг 3.3: Выполнение шага 2 (Суммаризация)**
```kotlin
emit(PipelineProgress.StepStarted(step2))

// ${PREVIOUS_OUTPUT} заменяется на результат шага 1
val arguments = mapOf(
    "text" to previousOutput,  // "Article 1: ...\nArticle 2: ..."
    "max_length" to 500
)

val result = multiMcpClient.callTool(
    serverUrl = "http://10.0.2.2:3000",
    toolName = "summarize",
    arguments = arguments
)

previousOutput = extractOutput(result)  // "Summary: AI developments include..."

emit(PipelineProgress.StepCompleted(step2, result))
```

**Шаг 3.4: Выполнение шага 3 (Сохранение)**
```kotlin
emit(PipelineProgress.StepStarted(step3))

val arguments = mapOf(
    "path" to "/sdcard/Download/ai_summary.txt",
    "content" to previousOutput  // "Summary: AI developments include..."
)

val result = multiMcpClient.callTool(
    serverUrl = "http://10.0.2.2:3001",
    toolName = "write_file",
    arguments = arguments
)

emit(PipelineProgress.StepCompleted(step3, result))
```

**Шаг 3.5: Завершение**
```kotlin
emit(PipelineProgress.Completed(config, executionResult))
```

### 4. UI обновляется в реальном времени

**Прогресс отображается в логе:**
```
[14:30:15] Pipeline started: Web Search & Save
[14:30:15] Connecting to 2 MCP server(s)...
[14:30:16] Connected to all servers
[14:30:16] Executing: Search Web
[14:30:18] ✓ Search Web completed
[14:30:18]   Output: Found 3 articles about AI...
[14:30:18] Executing: Create Summary
[14:30:20] ✓ Create Summary completed
[14:30:20]   Output: Summary: Recent AI developments...
[14:30:20] Executing: Save to File
[14:30:21] ✓ Save to File completed
[14:30:21]   Output: File written to /sdcard/Download/ai_summary.txt
[14:30:21] Pipeline completed successfully!
```

## Преимущества реализации

1. ✅ **Модульность** - каждый компонент независим
2. ✅ **Расширяемость** - легко добавить новые шаги или пайплайны
3. ✅ **Прозрачность** - полный лог выполнения в реальном времени
4. ✅ **Обработка ошибок** - корректная остановка при ошибках
5. ✅ **Переиспользование** - один клиент для всех серверов
6. ✅ **Типобезопасность** - строгая типизация Kotlin
7. ✅ **Реактивность** - Flow для асинхронных обновлений

## Дальнейшее развитие

### Возможные улучшения:

1. **Условные переходы**
   ```kotlin
   if (step1Result.contains("error")) {
       // Переход к шагу обработки ошибок
   } else {
       // Обычный поток
   }
   ```

2. **Параллельное выполнение**
   ```kotlin
   // Выполнить шаги 2 и 3 параллельно
   launch { executeStep(step2) }
   launch { executeStep(step3) }
   ```

3. **Повторные попытки**
   ```kotlin
   retry(3) {
       multiMcpClient.callTool(...)
   }
   ```

4. **Сохранение/загрузка пайплайнов**
   ```kotlin
   // Сохранение в локальную БД
   pipelineDao.savePipeline(config)
   ```

5. **Визуальный редактор пайплайнов**
   - Drag & drop интерфейс
   - Графическое отображение связей

6. **Шаблоны пайплайнов**
   - Библиотека готовых решений
   - Импорт/экспорт конфигураций

## Заключение

MCP Pipeline Agent демонстрирует мощь композиции простых инструментов в сложные автоматизированные процессы. Архитектура позволяет легко создавать новые пайплайны и интегрировать различные MCP серверы, открывая широкие возможности для автоматизации.
