# 🎯 Як працює Pipeline Agent - Покроково

## 📋 Загальна схема

```
┌──────────────────────────────────────────────────────────────┐
│                    Android App (UI)                           │
│  1. Користувач вводить запит                                 │
│  2. Натискає "Run Custom Search"                             │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│              PipelineViewModel (Логіка)                       │
│  3. Створює PipelineConfig з 3 кроками                       │
│  4. Викликає ExecutePipelineUseCase                          │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│         ExecutePipelineUseCase (Оркестратор)                 │
│  5. Підключається до MCP серверів                            │
│  6. Виконує кроки послідовно                                 │
│  7. Передає дані між кроками                                 │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│              MultiMcpClient (Комунікація)                     │
│  8. HTTP запити до MCP серверів                              │
│  9. Отримує відповіді                                        │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│               MCP Servers (Python)                            │
│  10. search_real.py - пошук в DuckDuckGo                     │
│  11. filesystem_demo.py - збереження файлів                  │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│                  Інтернет / Файлова система                   │
│  12. Реальні дані з DuckDuckGo/Wikipedia                     │
│  13. Файл збережено в output/                                │
└──────────────────────────────────────────────────────────────┘
```

---

## 📱 Крок 1: Користувач вводить запит

**Файл:** `PipelineScreen.kt`

**Що відбувається:**
```kotlin
// Користувач бачить поле вводу
OutlinedTextField(
    value = uiState.customSearchQuery,  // "latest SpaceX news"
    onValueChange = { viewModel.updateSearchQuery(it) },
    label = { Text("Enter search query") }
)
```

**Дії:**
1. Користувач набирає текст: "latest SpaceX news"
2. Текст зберігається в `customSearchQuery` через `updateSearchQuery()`

---

## 🔘 Крок 2: Натискання кнопки "Run"

**Файл:** `PipelineScreen.kt`

**Що відбувається:**
```kotlin
Button(
    onClick = { viewModel.executeWithCustomQuery() },
    enabled = !uiState.isExecuting
) {
    Text("Run Custom Search")
}
```

**Дії:**
1. Натиснуто кнопку
2. Викликається `viewModel.executeWithCustomQuery()`
3. UI показує індикатор завантаження

---

## 🏗️ Крок 3: Створення PipelineConfig

**Файл:** `PipelineViewModel.kt`

**Що відбувається:**
```kotlin
fun executeWithCustomQuery() {
    val query = _uiState.value.customSearchQuery  // "latest SpaceX news"

    // Створюємо конфігурацію пайплайна
    val customPipeline = createSearchAndSavePipeline(query)
    executePipeline(customPipeline)
}

private fun createSearchAndSavePipeline(customQuery: String): PipelineConfig {
    return PipelineConfig(
        name = "Custom Search: latest SpaceX news",
        steps = listOf(
            // Крок 1: Пошук
            PipelineStep(
                name = "Search Web",
                serverUrl = "http://10.0.2.2:3000",
                toolName = "brave_web_search",
                arguments = mapOf("query" to customQuery, "count" to 3),
                order = 1
            ),
            // Крок 2: Суммаризація
            PipelineStep(
                name = "Create Summary",
                serverUrl = "http://10.0.2.2:3000",
                toolName = "summarize",
                arguments = mapOf("text" to "\${PREVIOUS_OUTPUT}"),
                order = 2
            ),
            // Крок 3: Збереження
            PipelineStep(
                name = "Save to File",
                serverUrl = "http://10.0.2.2:3001",
                toolName = "write_file",
                arguments = mapOf(
                    "path" to "/sdcard/Download/spacex_summary.txt",
                    "content" to "\${PREVIOUS_OUTPUT}"
                ),
                order = 3
            )
        )
    )
}
```

**Дані:**
- Назва: "Custom Search: latest SpaceX news"
- 3 кроки в пайплайні
- Запит: "latest SpaceX news"

---

## 🎭 Крок 4: Запуск ExecutePipelineUseCase

**Файл:** `ExecutePipelineUseCase.kt`

**Що відбувається:**
```kotlin
fun execute(config: PipelineConfig): Flow<PipelineProgress> = flow {
    emit(PipelineProgress.Started(config))

    // Підключення до серверів
    emit(PipelineProgress.ConnectingToServers(...))

    // Виконання кроків
    for (step in config.steps.sortedBy { it.order }) {
        emit(PipelineProgress.StepStarted(step))
        // ... виконання кроку
        emit(PipelineProgress.StepCompleted(step, result))
    }

    emit(PipelineProgress.Completed(config, result))
}
```

**Дії:**
1. Емітить прогрес "Started"
2. UI отримує оновлення і показує "Pipeline started"
3. Починається підключення до серверів

---

## 🔌 Крок 5: Підключення до MCP серверів

**Файл:** `MultiMcpClient.kt`

**Що відбувається:**
```kotlin
// Підключення до search_real.py (порт 3000)
val result1 = connectToServer("http://10.0.2.2:3000")

// Підключення до filesystem_demo.py (порт 3001)
val result2 = connectToServer("http://10.0.2.2:3001")

suspend fun connectToServer(serverUrl: String): Result<ServerInfo> {
    val initRequest = JsonRpcRequest(
        method = "initialize",
        params = InitializeParams(clientInfo = ClientInfo("ChatAgent", "1.0"))
    )

    val response = mcpApiService.sendRequest(serverUrl, initRequest)
    // Отримуємо ServerInfo
}
```

**HTTP Запит до серверу:**
```json
POST http://10.0.2.2:3000
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "params": {
    "clientInfo": {"name": "ChatAgent", "version": "1.0"}
  }
}
```

**Відповідь сервера:**
```json
{
  "jsonrpc": "2.0",
  "result": {
    "serverInfo": {
      "name": "Real Web Search (DuckDuckGo)",
      "version": "1.0.0"
    },
    "capabilities": {"tools": {}}
  }
}
```

**Дії:**
1. Надсилаємо `initialize` запит
2. Сервер підтверджує підключення
3. Зберігаємо з'єднання в `connections` Map

---

## 🔍 Крок 6: Виконання Кроку 1 - Пошук

**Файл:** `ExecutePipelineUseCase.kt` → `MultiMcpClient.kt`

**Що відбувається:**
```kotlin
// Виклик інструменту
val result = multiMcpClient.callTool(
    serverUrl = "http://10.0.2.2:3000",
    toolName = "brave_web_search",
    arguments = mapOf("query" to "latest SpaceX news", "count" to 3)
)
```

**HTTP Запит:**
```json
POST http://10.0.2.2:3000
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "brave_web_search",
    "arguments": {
      "query": "latest SpaceX news",
      "count": 3
    }
  }
}
```

**Python сервер (search_real.py):**
```python
# Отримує запит
def search_duckduckgo(query, num_results=3):
    # 1. HTTP запит до DuckDuckGo
    response = requests.get(
        'https://html.duckduckgo.com/html/',
        params={'q': query}
    )

    # 2. Парсинг HTML
    soup = BeautifulSoup(response.text, 'html.parser')

    # 3. Витягування результатів
    for result_div in soup.find_all('div', class_='result')[:3]:
        title = result_div.find('a', class_='result__a').text
        url = result_div.find('a', class_='result__a')['href']
        description = ...

        results.append({
            'title': title,
            'url': url,
            'description': description
        })

    return results  # 3 реальні статті!
```

**Відповідь сервера:**
```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [{
      "type": "text",
      "text": "🔍 Search results for: latest SpaceX news\n\n📄 SpaceX Launches Starship...\n🔗 https://spacenews.com/...\n📝 SpaceX successfully launched...\n\n📄 Elon Musk Announces...\n🔗 https://techcrunch.com/..."
    }]
  }
}
```

**Дії:**
1. Android app → HTTP POST → Python server
2. Python → DuckDuckGo → парсинг результатів
3. Python → повертає 3 статті
4. Android app отримує текст з результатами
5. Зберігає в `previousOutput`

---

## 📝 Крок 7: Виконання Кроку 2 - Суммаризація

**Файл:** `ExecutePipelineUseCase.kt`

**Що відбувається:**
```kotlin
// Підготовка аргументів з попереднім результатом
val arguments = prepareArguments(step2, previousOutput)
// arguments = {"text": "📄 SpaceX Launches...\n📄 Elon Musk...", "max_length": 500}

val result = multiMcpClient.callTool(
    serverUrl = "http://10.0.2.2:3000",
    toolName = "summarize",
    arguments = arguments
)
```

**Python сервер:**
```python
def summarize(text, max_length=500):
    # Проста суммаризація - перші речення до max_length
    sentences = text.split('.')
    summary = ""
    for sentence in sentences:
        if len(summary) + len(sentence) < max_length:
            summary += sentence + "."
        else:
            break

    return f"Summary ({len(summary)} chars):\n\n{summary}"
```

**Результат:**
```
Summary (450 chars):

📄 SpaceX Launches Starship Successfully
🔗 https://spacenews.com/starship-launch/
📝 SpaceX successfully launched its Starship rocket...

📄 Elon Musk Announces Mars Mission Timeline
🔗 https://techcrunch.com/mars-mission/
📝 CEO Elon Musk revealed ambitious plans...
```

**Дії:**
1. Бере `previousOutput` (результати пошуку)
2. Надсилає на сервер для суммаризації
3. Отримує скорочений текст (500 символів)
4. Зберігає в новий `previousOutput`

---

## 💾 Крок 8: Виконання Кроку 3 - Збереження

**Файл:** `MultiMcpClient.kt`

**Що відбувається:**
```kotlin
val result = multiMcpClient.callTool(
    serverUrl = "http://10.0.2.2:3001",  // File System server
    toolName = "write_file",
    arguments = mapOf(
        "path" to "/sdcard/Download/spacex_summary.txt",
        "content" to previousOutput  // Текст суммаризації
    )
)
```

**Python сервер (filesystem_demo.py):**
```python
def write_file(path, content):
    # Конвертація Android шляху
    if path.startswith('/sdcard/Download'):
        path = 'output/' + path.split('/')[-1]
        # path = "output/spacex_summary.txt"

    # Запис файлу
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

    return f"✓ File written to {path}"
```

**Результат:**
```
Файл створено: mcp_servers/output/spacex_summary.txt

Вміст:
Summary (450 chars):

📄 SpaceX Launches Starship Successfully
🔗 https://spacenews.com/starship-launch/
📝 SpaceX successfully launched its Starship rocket...

📄 Elon Musk Announces Mars Mission Timeline
🔗 https://techcrunch.com/mars-mission/
📝 CEO Elon Musk revealed ambitious plans...
```

**Дії:**
1. Надсилає текст суммаризації на File System сервер
2. Сервер зберігає у `output/spacex_summary.txt`
3. Повертає підтвердження

---

## ✅ Крок 9: Завершення пайплайна

**Файл:** `ExecutePipelineUseCase.kt`

**Що відбувається:**
```kotlin
// Всі кроки виконані успішно
val executionResult = PipelineExecutionResult(
    pipelineId = config.id,
    status = PipelineStatus.COMPLETED,
    stepResults = listOf(stepResult1, stepResult2, stepResult3),
    finalOutput = previousOutput,  // Фінальний текст
    startTime = startTime,
    endTime = System.currentTimeMillis()
)

emit(PipelineProgress.Completed(config, executionResult))
```

**Дії:**
1. Створює підсумковий результат
2. Емітить `Completed` прогрес
3. UI отримує оновлення

---

## 📱 Крок 10: Відображення результату в UI

**Файл:** `PipelineScreen.kt`

**Що відбувається:**
```kotlin
// ViewModel отримує Completed прогрес
viewModel.uiState.collect { state ->
    when {
        state.executionResult != null -> {
            // Показуємо результат
            ExecutionResultCard(state.executionResult.finalOutput)
        }
    }
}
```

**UI показує:**
```
[14:30:15] Pipeline started
[14:30:16] Executing: Search Web
[14:30:18] ✓ Search Web completed
[14:30:18]   Output: Found 3 articles...
[14:30:18] Executing: Create Summary
[14:30:19] ✓ Create Summary completed
[14:30:19]   Output: Summary (450 chars)...
[14:30:19] Executing: Save to File
[14:30:20] ✓ Save to File completed
[14:30:20]   Output: File written...
[14:30:20] Pipeline completed successfully!

┌────────────────────────────────────┐
│ ✓ Final Output                     │
│                                    │
│ Summary (450 chars):               │
│ 📄 SpaceX Launches...              │
│ ...                                │
└────────────────────────────────────┘
```

---

## 🔄 Потік даних через всі компоненти

```
Користувач вводить "latest SpaceX news"
    ↓
PipelineViewModel.updateSearchQuery()
    ↓
Зберігається в customSearchQuery
    ↓
Користувач натискає "Run"
    ↓
PipelineViewModel.executeWithCustomQuery()
    ↓
createSearchAndSavePipeline("latest SpaceX news")
    ↓
PipelineConfig {
    steps: [Пошук, Суммаризація, Збереження]
}
    ↓
ExecutePipelineUseCase.execute(config)
    ↓
MultiMcpClient.connectToServer(3000)
MultiMcpClient.connectToServer(3001)
    ↓
MultiMcpClient.callTool("brave_web_search", {query: "latest SpaceX news"})
    ↓
HTTP POST → search_real.py:3000
    ↓
DuckDuckGo пошук → парсинг HTML → результати
    ↓
Повертає 3 статті
    ↓
previousOutput = "📄 SpaceX Launches...\n📄 Elon Musk..."
    ↓
MultiMcpClient.callTool("summarize", {text: previousOutput})
    ↓
HTTP POST → search_real.py:3000
    ↓
Суммаризація → перші 500 символів
    ↓
previousOutput = "Summary (450 chars):\n\n📄 SpaceX..."
    ↓
MultiMcpClient.callTool("write_file", {path: "...", content: previousOutput})
    ↓
HTTP POST → filesystem_demo.py:3001
    ↓
Запис файлу output/spacex_summary.txt
    ↓
Повертає "✓ File written"
    ↓
ExecutePipelineUseCase.emit(Completed)
    ↓
PipelineViewModel оновлює UI state
    ↓
PipelineScreen показує результат
```

---

## 📊 Технічні деталі

### Передача даних між кроками

**Механізм `${PREVIOUS_OUTPUT}`:**
```kotlin
// В ExecutePipelineUseCase.kt
private fun prepareArguments(step: PipelineStep, previousOutput: String?): Map<String, Any> {
    val arguments = step.arguments.toMutableMap()

    if (previousOutput != null) {
        arguments.forEach { (key, value) ->
            if (value is String && value == "\${PREVIOUS_OUTPUT}") {
                arguments[key] = previousOutput  // ← Заміна!
            }
        }
    }

    return arguments
}
```

**Приклад:**
```
Крок 2:
  arguments = {"text": "${PREVIOUS_OUTPUT}", "max_length": 500}

Після prepareArguments():
  arguments = {"text": "📄 SpaceX Launches...", "max_length": 500}
```

### MCP протокол (JSON-RPC 2.0)

**Структура запиту:**
```json
{
  "jsonrpc": "2.0",
  "id": "unique-id",
  "method": "tools/call",
  "params": {
    "name": "tool_name",
    "arguments": {...}
  }
}
```

**Структура відповіді:**
```json
{
  "jsonrpc": "2.0",
  "id": "unique-id",
  "result": {
    "content": [{
      "type": "text",
      "text": "результат"
    }]
  }
}
```

---

## 🎯 Підсумок

### Весь процес займає ~3-5 секунд:

1. **0.0s** - Ввід запиту "latest SpaceX news"
2. **0.1s** - Створення PipelineConfig
3. **0.2s** - Підключення до серверів
4. **0.3s-2.5s** - Пошук в DuckDuckGo (найдовше!)
5. **2.5s-2.6s** - Суммаризація
6. **2.6s-2.7s** - Збереження файлу
7. **2.7s** - Відображення результату

### Компоненти:
- ✅ Android App (Kotlin, Jetpack Compose)
- ✅ MCP Servers (Python, Flask)
- ✅ DuckDuckGo (реальний пошук)
- ✅ Wikipedia (запасний варіант)

### Результат:
- 📄 Реальні статті з інтернету
- 📝 Суммаризований текст
- 💾 Збережено у файл

---

🎉 **Ось так працює Pipeline Agent від початку до кінця!**
