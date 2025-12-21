# MCP (Model Context Protocol) Integration Guide

## Що реалізовано

✅ **1. MCP HTTP Client через Retrofit**
- Файл: `app/src/main/java/com/example/chatagent/data/remote/client/McpClient.kt`
- Підтримка JSON-RPC 2.0 протоколу
- Методи: `initialize`, `tools/list`, `tools/call`

✅ **2. DTO Models для MCP**
- Файл: `app/src/main/java/com/example/chatagent/data/remote/dto/McpDto.kt`
- JSON-RPC Request/Response
- MCP Tool моделі
- Initialize/ListTools/CallTool параметри

✅ **3. UI для роботи з MCP**
- Файли:
  - `McpScreen.kt` - головний екран
  - `McpViewModel.kt` - бізнес логіка
  - `McpUiState.kt` - стан UI
  - `McpToolComponents.kt` - компоненти для виконання інструментів

✅ **4. Публічні MCP сервери**
- Echo Server (`https://echo.mcp.inevitable.fyi/mcp`) - для тестування
- Time Server (`https://time.mcp.inevitable.fyi/mcp`) - час та дата
- Everything Server (`https://everything.mcp.inevitable.fyi/mcp`) - різні функції
- Text Extractor (`https://text-extractor.mcp.inevitable.fyi/mcp`) - витягування тексту
- Inspector (`https://kite-mcp-inspector.fly.dev/`) - debugging

## Як використовувати

### 1. Підключення до MCP сервера

```kotlin
// В додатку:
1. Відкрийте MCP Tools екран
2. Натисніть "Select Public Server"
3. Виберіть сервер (наприклад, Echo Server або Time Server)
4. Натисніть "Connect"
```

### 2. Список доступних інструментів

Після підключення автоматично завантажиться список доступних інструментів з сервера.

Приклад інструментів залежить від обраного сервера:
- Echo Server - тестування MCP клієнта
- Time Server - робота з часом та датою
- Everything Server - різноманітні інструменти

### 3. Виклик інструменту

```kotlin
// В додатку:
1. Знайдіть потрібний інструмент в списку
2. Натисніть кнопку "Execute"
3. Введіть необхідні параметри
4. Натисніть "Execute" в діалозі
5. Результат відобразиться на екрані
```

### 4. Програмний виклик через McpClient

```kotlin
@Inject lateinit var mcpClient: McpClient

// Підключення до Echo Server
val initResult = mcpClient.connect("https://echo.mcp.inevitable.fyi/mcp")

// Отримання списку інструментів
val toolsResult = mcpClient.listTools()

// Виклик інструменту (приклад залежить від сервера)
val result = mcpClient.callTool(
    toolName = "echo",
    arguments = mapOf(
        "message" to "Hello MCP!"
    )
)

// Обробка результату
result.fold(
    onSuccess = { callResult ->
        callResult.content.forEach { content ->
            when (content.type) {
                "text" -> println(content.text)
                // інші типи контенту
            }
        }
    },
    onFailure = { error ->
        println("Error: ${error.message}")
    }
)
```

## Приклади використання

### Приклад 1: Echo Server (тестування)

```kotlin
// Підключіться до Echo Server
viewModel.callTool(
    toolName = "echo",
    arguments = mapOf(
        "message" to "Test message"
    )
)
```

### Приклад 2: Time Server

```kotlin
// Підключіться до Time Server (https://time.mcp.inevitable.fyi/mcp)
viewModel.callTool(
    toolName = "get-current-time",
    arguments = emptyMap()
)
```

## Структура проекту

```
app/src/main/java/com/example/chatagent/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── McpApiService.kt          # Retrofit API
│   │   ├── client/
│   │   │   └── McpClient.kt              # MCP HTTP Client
│   │   └── dto/
│   │       └── McpDto.kt                 # Data Transfer Objects
│   └── domain/
│       └── model/
│           └── McpServers.kt             # Список публічних серверів
└── presentation/
    └── mcp/
        ├── McpScreen.kt                  # Головний екран
        ├── McpViewModel.kt               # ViewModel
        ├── McpUiState.kt                 # UI State
        └── McpToolComponents.kt          # UI компоненти
```

## JSON-RPC протокол

### Initialize Request
```json
{
  "jsonrpc": "2.0",
  "id": "uuid",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "clientInfo": {
      "name": "ChatAgent",
      "version": "1.0.0"
    }
  }
}
```

### List Tools Request
```json
{
  "jsonrpc": "2.0",
  "id": "uuid",
  "method": "tools/list",
  "params": {}
}
```

### Call Tool Request
```json
{
  "jsonrpc": "2.0",
  "id": "uuid",
  "method": "tools/call",
  "params": {
    "name": "get-forecast",
    "arguments": {
      "latitude": "50.4501",
      "longitude": "30.5234"
    }
  }
}
```

## Інтеграція з чатом (ГОТОВО ✅)

MCP інструменти тепер повністю інтегровані в чат! Claude автоматично:

1. **Бачить доступні MCP інструменти** - якщо ви підключені до MCP сервера
2. **Вирішує коли їх використовувати** - на основі вашого запиту
3. **Викликає інструменти автоматично** - без вашої участі
4. **Повертає результати в чат** - як частину відповіді

### Як користуватися:

1. Підключіться до MCP сервера через екран "MCP Tools"
2. Поверніться в чат
3. Просто спілкуйтеся з Claude - він сам визначить коли потрібно використати інструменти

### Приклади запитів:

**Для Echo Server:**
```
Привіт! Які у тебе є інструменти?
Використай echo інструмент щоб повторити "Hello MCP!"
```

**Для Time Server:**
```
Який зараз час?
Покажи мені поточну дату та час
```

**Для Everything Server:**
```
Які функції ти можеш виконати?
```

### Технічні деталі:

- Максимум 5 циклів tool use для одного повідомлення
- Результати інструментів логуються в Logcat (тег: ChatRepositoryImpl)
- Помилки виконання повертаються Claude для обробки

## Troubleshooting

### Помилка: "Unable to resolve host" (DNS проблема) ⚠️

Це найчастіша проблема в Android Emulator. Детальні інструкції: `DNS_FIX.md`

**Швидке рішення:**
```bash
adb root
adb shell "setprop net.dns1 8.8.8.8"
adb shell "setprop net.dns2 8.8.4.4"
```

Після цього перезапустіть додаток.

**Альтернатива:** Використайте реальний Android пристрій замість емулятора.

### Помилка: "Connection failed"
- Перевірте інтернет з'єднання
- Переконайтесь що URL сервера правильний
- Спробуйте інший публічний сервер
- Перегляньте `DNS_FIX.md` якщо бачите "UnknownHostException"

### Помилка: "Tool execution failed"
- Перевірте правильність параметрів
- Переконайтесь що всі required поля заповнені
- Перегляньте логи для деталей помилки

### Сервер не відповідає
- Деякі публічні сервери можуть бути недоступні
- Спробуйте підключитись пізніше
- Використайте альтернативний сервер

### MCP інструменти не з'являються в чаті
- Переконайтеся що ви підключені до MCP сервера (екран MCP Tools)
- Перевірте логи (Logcat, тег: ChatRepositoryImpl) для "Available MCP tools: X"
- Якщо бачите "Available MCP tools: 0" - перепідключіться до сервера

## Корисні посилання

- [MCP Specification](https://modelcontextprotocol.io/specification/2025-11-25)
- [MCP Documentation](https://modelcontextprotocol.io/docs)
- [Awesome MCP Servers](https://github.com/punkpeye/awesome-mcp-servers)
