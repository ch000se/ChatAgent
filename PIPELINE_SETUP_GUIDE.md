# MCP Pipeline Setup Guide

## Обзор

Этот гайд поможет настроить MCP Pipeline Agent - систему для автоматической связки нескольких MCP инструментов в цепочку выполнения.

## Архитектура

Pipeline Agent состоит из:
- **MultiMcpClient** - управляет несколькими MCP серверами одновременно
- **ExecutePipelineUseCase** - оркестрирует выполнение цепочки инструментов
- **PipelineViewModel** - управляет состоянием UI
- **PipelineScreen** - отображает доступные пайплайны и их выполнение

## Пример пайплайна

**Web Search & Save Pipeline:**
1. 🔍 **Поиск в интернете** (Brave Search MCP) → находит 3 релевантные статьи
2. 📝 **Суммаризация** (LLM/Claude) → создает краткую выжимку
3. 💾 **Сохранение в файл** (File System MCP) → сохраняет результат

## Необходимые MCP серверы

Для демо-пайплайнов нужны 2 MCP сервера:

### 1. Brave Search MCP Server

Для поиска в интернете.

#### Установка

```bash
npm install -g @modelcontextprotocol/server-brave-search
```

#### Настройка

1. Получите API ключ на [Brave Search API](https://brave.com/search/api/)
2. Создайте файл `.env`:
```
BRAVE_API_KEY=your_api_key_here
```

#### Запуск сервера

```bash
# Установите зависимости
npm install @anthropic-ai/mcp-server-brave-search

# Создайте server.js
cat > server.js << 'EOF'
const { BraveSearchServer } = require('@anthropic-ai/mcp-server-brave-search');

const server = new BraveSearchServer({
  apiKey: process.env.BRAVE_API_KEY
});

server.start(3000);
console.log('Brave Search MCP Server running on http://localhost:3000');
EOF

# Запустите сервер
BRAVE_API_KEY=your_key node server.js
```

**Доступные инструменты:**
- `brave_web_search` - поиск в интернете
  - Параметры: `query` (string), `count` (number, optional)
  - Возвращает: список результатов с заголовками, URL и описаниями

### 2. File System MCP Server

Для работы с файлами.

#### Установка

```bash
npm install -g @modelcontextprotocol/server-filesystem
```

#### Запуск сервера

```bash
# Создайте server.js для filesystem
cat > filesystem-server.js << 'EOF'
const { FileSystemServer } = require('@anthropic-ai/mcp-server-filesystem');

const server = new FileSystemServer({
  allowedPaths: ['/sdcard/Download', '/tmp']
});

server.start(3001);
console.log('File System MCP Server running on http://localhost:3001');
EOF

# Запустите сервер
node filesystem-server.js
```

**Доступные инструменты:**
- `write_file` - записать файл
  - Параметры: `path` (string), `content` (string)
- `read_file` - прочитать файл
  - Параметры: `path` (string)
- `list_directory` - список файлов в директории
  - Параметры: `path` (string)

## Простой способ: Docker Compose

Используйте Docker для быстрого запуска обоих серверов:

```yaml
# docker-compose.yml
version: '3.8'

services:
  brave-search-mcp:
    image: node:18
    working_dir: /app
    volumes:
      - ./brave-search-server:/app
    environment:
      - BRAVE_API_KEY=${BRAVE_API_KEY}
    command: node server.js
    ports:
      - "3000:3000"

  filesystem-mcp:
    image: node:18
    working_dir: /app
    volumes:
      - ./filesystem-server:/app
      - /sdcard/Download:/sdcard/Download
    command: node server.js
    ports:
      - "3001:3001"
```

Запуск:
```bash
docker-compose up
```

## Альтернативный вариант: Python MCP серверы

Если не хотите использовать Node.js, можно создать простые Python MCP серверы:

### Brave Search Python Server

```python
# brave_search_mcp.py
from flask import Flask, request, jsonify
import requests

app = Flask(__name__)
BRAVE_API_KEY = "your_api_key"

@app.route('/', methods=['POST'])
def handle_request():
    data = request.json
    method = data.get('method')

    if method == 'initialize':
        return jsonify({
            "jsonrpc": "2.0",
            "id": data['id'],
            "result": {
                "protocolVersion": "2024-11-05",
                "serverInfo": {"name": "Brave Search", "version": "1.0.0"},
                "capabilities": {"tools": {}}
            }
        })

    elif method == 'tools/list':
        return jsonify({
            "jsonrpc": "2.0",
            "id": data['id'],
            "result": {
                "tools": [{
                    "name": "brave_web_search",
                    "description": "Search the web using Brave Search",
                    "inputSchema": {
                        "type": "object",
                        "properties": {
                            "query": {"type": "string"},
                            "count": {"type": "number", "default": 3}
                        },
                        "required": ["query"]
                    }
                }]
            }
        })

    elif method == 'tools/call':
        params = data.get('params', {})
        tool_name = params.get('name')
        args = params.get('arguments', {})

        if tool_name == 'brave_web_search':
            query = args.get('query')
            count = args.get('count', 3)

            # Call Brave Search API
            response = requests.get(
                'https://api.search.brave.com/res/v1/web/search',
                headers={'X-Subscription-Token': BRAVE_API_KEY},
                params={'q': query, 'count': count}
            )

            results = response.json()

            return jsonify({
                "jsonrpc": "2.0",
                "id": data['id'],
                "result": {
                    "content": [{
                        "type": "text",
                        "text": str(results)
                    }]
                }
            })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3000)
```

Запуск:
```bash
pip install flask requests
python brave_search_mcp.py
```

### File System Python Server

```python
# filesystem_mcp.py
from flask import Flask, request, jsonify
import os

app = Flask(__name__)
ALLOWED_PATHS = ['/sdcard/Download', '/tmp']

@app.route('/', methods=['POST'])
def handle_request():
    data = request.json
    method = data.get('method')

    if method == 'initialize':
        return jsonify({
            "jsonrpc": "2.0",
            "id": data['id'],
            "result": {
                "protocolVersion": "2024-11-05",
                "serverInfo": {"name": "File System", "version": "1.0.0"},
                "capabilities": {"tools": {}}
            }
        })

    elif method == 'tools/list':
        return jsonify({
            "jsonrpc": "2.0",
            "id": data['id'],
            "result": {
                "tools": [
                    {
                        "name": "write_file",
                        "description": "Write content to a file",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "path": {"type": "string"},
                                "content": {"type": "string"}
                            },
                            "required": ["path", "content"]
                        }
                    },
                    {
                        "name": "read_file",
                        "description": "Read content from a file",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "path": {"type": "string"}
                            },
                            "required": ["path"]
                        }
                    }
                ]
            }
        })

    elif method == 'tools/call':
        params = data.get('params', {})
        tool_name = params.get('name')
        args = params.get('arguments', {})

        if tool_name == 'write_file':
            path = args.get('path')
            content = args.get('content')

            # Check if path is allowed
            if not any(path.startswith(allowed) for allowed in ALLOWED_PATHS):
                return jsonify({
                    "jsonrpc": "2.0",
                    "id": data['id'],
                    "error": {"code": -32000, "message": "Path not allowed"}
                })

            # Write file
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, 'w') as f:
                f.write(content)

            return jsonify({
                "jsonrpc": "2.0",
                "id": data['id'],
                "result": {
                    "content": [{
                        "type": "text",
                        "text": f"File written successfully to {path}"
                    }]
                }
            })

        elif tool_name == 'read_file':
            path = args.get('path')

            if not any(path.startswith(allowed) for allowed in ALLOWED_PATHS):
                return jsonify({
                    "jsonrpc": "2.0",
                    "id": data['id'],
                    "error": {"code": -32000, "message": "Path not allowed"}
                })

            with open(path, 'r') as f:
                content = f.read()

            return jsonify({
                "jsonrpc": "2.0",
                "id": data['id'],
                "result": {
                    "content": [{
                        "type": "text",
                        "text": content
                    }]
                }
            })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3001)
```

Запуск:
```bash
pip install flask
python filesystem_mcp.py
```

## Настройка Android приложения

### 1. Обновите URL серверов

В `PipelineViewModel.kt` обновите URL серверов на ваши локальные адреса:

```kotlin
// Для эмулятора Android
serverUrl = "http://10.0.2.2:3000"  // Brave Search
serverUrl = "http://10.0.2.2:3001"  // File System

// Для физического устройства (используйте IP компьютера)
serverUrl = "http://192.168.1.100:3000"
serverUrl = "http://192.168.1.100:3001"
```

### 2. Разрешите HTTP трафик

В `AndroidManifest.xml` убедитесь, что указано:
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

### 3. Добавьте разрешения

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Использование

1. **Запустите MCP серверы** (оба должны быть активны)
2. **Откройте приложение** и перейдите на экран Pipeline (иконка дерева в топ-баре)
3. **Выберите пайплайн** из списка доступных
4. **Нажмите "Run"** для запуска
5. **Наблюдайте за прогрессом** в реальном времени
6. **Проверьте результаты** в execution log и final output

## Создание своих пайплайнов

В `PipelineViewModel.kt` вы можете создавать свои пайплайны:

```kotlin
private fun createCustomPipeline(): PipelineConfig {
    return PipelineConfig(
        name = "My Custom Pipeline",
        description = "Description of what it does",
        steps = listOf(
            PipelineStep(
                name = "Step 1",
                description = "First step description",
                serverUrl = "http://10.0.2.2:3000",
                toolName = "tool_name",
                arguments = mapOf(
                    "param1" to "value1"
                ),
                order = 1
            ),
            PipelineStep(
                name = "Step 2",
                description = "Second step uses previous output",
                serverUrl = "http://10.0.2.2:3001",
                toolName = "another_tool",
                arguments = mapOf(
                    "input" to "\${PREVIOUS_OUTPUT}"  // Использует результат предыдущего шага
                ),
                order = 2
            )
        )
    )
}
```

## Troubleshooting

### Проблема: "Connection failed"
- Проверьте, что MCP серверы запущены
- Проверьте правильность URL (10.0.2.2 для эмулятора)
- Убедитесь, что `usesCleartextTraffic="true"` в манифесте

### Проблема: "Tool call failed"
- Проверьте формат аргументов инструмента
- Убедитесь, что у вас есть API ключ для Brave Search
- Проверьте логи MCP сервера

### Проблема: "File write failed"
- Проверьте разрешения на запись файлов
- Убедитесь, что путь находится в разрешенных директориях
- Для Android 11+ нужен особый доступ к файловой системе

## Примеры использования

### 1. Исследовательский пайплайн
```
Поиск по теме → Суммаризация → Сохранение отчета
```

### 2. Мониторинг новостей
```
Поиск новостей → Фильтрация → Отправка уведомления
```

### 3. Автоматизация контента
```
Получение данных → Обработка → Экспорт в различных форматах
```

## Дополнительные ресурсы

- [MCP Protocol Specification](https://spec.modelcontextprotocol.io/)
- [Brave Search API Docs](https://brave.com/search/api/)
- [Android Network Security](https://developer.android.com/training/articles/security-config)
