# 🚀 Quick Start - MCP за 2 хвилини

## ✅ ОНОВЛЕННЯ: Додано робочі публічні MCP сервери!

Тепер доступні публічні MCP сервери від Cloudflare, PayPal, Microsoft та GitHub!

### Спосіб 1: Cloudflare Demo Day MCP (РЕКОМЕНДОВАНО!)

**Додаток:**
1. Відкрити **MCP Tools**
2. Вибрати **"Cloudflare Demo Day MCP"** (вже вибраний за замовчуванням!)
3. Натиснути **Connect**
4. ✅ Готово!

URL: `https://demo-day.mcp.cloudflare.com/sse`

**Альтернатива:** PayPal MCP Server - `https://mcp.paypal.com/sse`

### Спосіб 2: Локальний сервер (якщо публічний не підходить)

**Термінал** - Запустити сервер:
```bash
npx @modelcontextprotocol/server-everything
```

Побачите:
```
✓ Server running on port 3000
```

**Додаток** - Підключитися:
1. Відкрити **MCP Tools**
2. Вибрати **"Localhost (Recommended for Testing)"**
3. Натиснути **Connect**
4. ✅ Побачите список інструментів

**Чат** - Протестувати:
```
Які у тебе є інструменти?
```

### Спосіб 2: Простий Python сервер

**Термінал** - Створити файл `simple_mcp.py`:
```python
from fastapi import FastAPI
import uvicorn

app = FastAPI()

@app.post("/mcp")
async def mcp(request: dict):
    method = request.get("method")
    req_id = request.get("id")

    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {"tools": {}},
                "serverInfo": {"name": "Test Server", "version": "1.0"}
            }
        }

    if method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "tools": [{
                    "name": "echo",
                    "description": "Echo message",
                    "inputSchema": {
                        "type": "object",
                        "properties": {"msg": {"type": "string"}},
                        "required": ["msg"]
                    }
                }]
            }
        }

    if method == "tools/call":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "content": [{"type": "text", "text": "Echo: " + str(request.get("params", {}).get("arguments", {}))}]
            }
        }

    return {"error": "Unknown"}

uvicorn.run(app, host="0.0.0.0", port=3000)
```

**Запустити:**
```bash
pip install fastapi uvicorn
python simple_mcp.py
```

**Додаток:**
- URL: `http://10.0.2.2:3000/mcp`

## Якщо хочете виправити DNS замість локального сервера

```bash
adb root
adb shell "setprop net.dns1 8.8.8.8"
adb shell "setprop net.dns2 8.8.4.4"
# Перезапустити додаток
```

Після цього публічні сервери працюватимуть.

## Як це використовувати в чаті

1. **Підключитися** до MCP сервера (один раз)
2. **Чатити** з Claude як звичайно
3. **Claude автоматично** використає інструменти коли потрібно

**Приклади:**
```
Які інструменти доступні?
Використай echo щоб сказати "Hello"
```

## Troubleshooting

**Сервер не запускається:**
- Змініть порт: `--port 8080`
- В додатку: `http://10.0.2.2:8080/mcp`

**Connection refused в додатку:**
- Переконайтеся що сервер запущений
- Використовуйте `10.0.2.2`, НЕ `localhost`

**Tools не з'являються:**
- Перевірте Logcat: "Available MCP tools: X"
- Якщо 0 - перепідключіться

## Детальна документація

- `MCP_INTEGRATION_SUMMARY.md` - Повний огляд
- `LOCAL_MCP_SERVER.md` - Докладно про локальні сервери
- `DNS_FIX.md` - Виправлення DNS
- `MCP_GUIDE.md` - Повний гайд по MCP

**Готово! Тепер ваш чат може використовувати MCP tools!** 🎉
