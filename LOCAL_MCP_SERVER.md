# Запуск локального MCP сервера (Обхід DNS проблеми)

## Чому локальний сервер?

Android емулятор має проблеми з DNS для деяких доменів. Локальний сервер - найпростіше рішення!

## Швидкий старт (5 хвилин)

### Варіант 1: Використати готовий MCP Server Everything (Рекомендовано)

```bash
# 1. Встановити Node.js (якщо ще немає): https://nodejs.org/

# 2. Запустити MCP Everything Server одною командою:
npx @modelcontextprotocol/server-everything --port 3000
```

Сервер запуститься на `http://localhost:3000`

### Варіант 2: Використати MCP Inspector для debugging

```bash
npx @modelcontextprotocol/inspector
```

Запуститься на `http://localhost:5173`

### Варіант 3: Простий Echo Server на Python

Створіть файл `mcp_server.py`:

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/mcp")
@app.post("/")
async def mcp_endpoint(request: dict):
    """Echo server - returns request back"""
    if request.get("method") == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": request.get("id"),
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {"listChanged": True}
                },
                "serverInfo": {
                    "name": "Local Echo Server",
                    "version": "1.0.0"
                }
            }
        }
    elif request.get("method") == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": request.get("id"),
            "result": {
                "tools": [
                    {
                        "name": "echo",
                        "description": "Echoes back the provided message",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "message": {
                                    "type": "string",
                                    "description": "Message to echo"
                                }
                            },
                            "required": ["message"]
                        }
                    },
                    {
                        "name": "get_time",
                        "description": "Returns current server time",
                        "inputSchema": {
                            "type": "object",
                            "properties": {}
                        }
                    }
                ]
            }
        }
    elif request.get("method") == "tools/call":
        tool_name = request.get("params", {}).get("name")
        arguments = request.get("params", {}).get("arguments", {})

        if tool_name == "echo":
            message = arguments.get("message", "No message")
            result_text = f"Echo: {message}"
        elif tool_name == "get_time":
            import datetime
            result_text = f"Current time: {datetime.datetime.now()}"
        else:
            result_text = "Unknown tool"

        return {
            "jsonrpc": "2.0",
            "id": request.get("id"),
            "result": {
                "content": [
                    {
                        "type": "text",
                        "text": result_text
                    }
                ]
            }
        }

    return {"error": "Unknown method"}

if __name__ == "__main__":
    print("🚀 MCP Server starting on http://localhost:3000")
    print("📱 Android Emulator URL: http://10.0.2.2:3000/mcp")
    uvicorn.run(app, host="0.0.0.0", port=3000)
```

Запустити:
```bash
pip install fastapi uvicorn
python mcp_server.py
```

## Підключення з додатка

1. Запустіть будь-який з серверів вище
2. В додатку відкрийте "MCP Tools"
3. Виберіть "Localhost (Recommended for Testing)"
4. Натисніть "Connect"

URL для Android Emulator: `http://10.0.2.2:3000/mcp`
- `10.0.2.2` - це спеціальна адреса для localhost з емулятора

## Перевірка що сервер працює

```bash
# В терміналі:
curl -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "test",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "clientInfo": {"name": "test", "version": "1.0"}
    }
  }'
```

Повинна бути відповідь з `serverInfo`.

## Готові MCP сервери для локального запуску

### 1. MCP Server Everything
- Найбільш функціональний
- Включає: filesystem, git, database, web scraping
```bash
npx @modelcontextprotocol/server-everything
```

### 2. MCP Server Memory
- Зберігає контекст між запитами
```bash
npx @modelcontextprotocol/server-memory
```

### 3. MCP Server Brave Search
- Потребує API key від Brave
```bash
npx @modelcontextprotocol/server-brave-search
```

### 4. Custom сервери з GitHub

```bash
git clone https://github.com/modelcontextprotocol/servers.git
cd servers/src/filesystem
npm install
npm run build
npm start
```

## Переваги локального сервера

✅ Немає DNS проблем
✅ Працює офлайн
✅ Швидше (no network latency)
✅ Повний контроль
✅ Легко debugging

## Troubleshooting

**Помилка: порт зайнятий**
```bash
# Змініть порт
npx @modelcontextprotocol/server-everything --port 8080

# В додатку використайте: http://10.0.2.2:8080/mcp
```

**Сервер не доступний з емулятора**
- Перевірте що сервер слухає на `0.0.0.0`, а не тільки `localhost`
- Перевірте firewall
- Спробуйте інший порт

**Connection refused**
- Переконайтеся що сервер запущений
- Перевірте що використовуєте `10.0.2.2`, а не `localhost` в додатку
