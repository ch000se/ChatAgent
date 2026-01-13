# 🔀 Git MCP Server

Python-based MCP server для Git операцій в ChatAgent.

## ✅ Швидкий старт

### 1. Запустіть сервер

**Windows:**
```bash
start_git.bat
```

**Linux/Mac:**
```bash
python git_server.py
```

### 2. Перевірте роботу

```bash
# Windows
test_git_server.bat

# Linux/Mac
python test_git_server.py
```

Очікуваний результат:
```
✓ Git MCP Server is working correctly
```

### 3. Підключіться в ChatAgent

1. Відкрийте додаток
2. Меню → MCP Tools
3. Підключіться до:
   - **Android Emulator:** `http://10.0.2.2:3002`
   - **Physical Device:** `http://YOUR_PC_IP:3002`

### 4. Використовуйте команди

```bash
/git status
/git log
/git diff
/git branch
/project
```

---

## 🛠️ Доступні інструменти

| Інструмент | Опис | Параметри |
|-----------|------|-----------|
| `git_status` | Статус репозиторію | - |
| `git_log` | Історія коммітів | `count` (за замовчуванням 10) |
| `git_diff` | Статистика змін | - |
| `git_branch` | Список гілок | - |
| `git_current_branch` | Поточна гілка | - |
| `git_remote` | Remote інформація | - |
| `execute_command` | Виконання git команди | `command`, `args` |

---

## 📡 API Endpoints

### Health Check
```bash
GET /health
```

Відповідь:
```json
{
  "status": "healthy",
  "git": true,
  "repository": "/path/to/ChatAgent",
  "version": "1.0.0"
}
```

### List Tools
```bash
POST /mcp/v1/tools/list
```

### Call Tool
```bash
POST /mcp/v1/tools/call
Content-Type: application/json

{
  "name": "git_status",
  "arguments": {}
}
```

---

## 🔧 Налаштування

### Порт
За замовчуванням: `3002`

Змінити в `git_server.py`:
```python
app.run(host='0.0.0.0', port=3002, debug=False)
```

### Репозиторій
За замовчуванням: батьківська директорія (весь проект)

Змінити в `git_server.py`:
```python
REPO_PATH = "/custom/path/to/repo"
```

---

## 🧪 Тестування

### Автоматичний тест
```bash
python test_git_server.py
```

### Ручний тест
```bash
# Health check
curl http://localhost:3002/health

# Get status
curl -X POST http://localhost:3002/mcp/v1/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name":"git_status","arguments":{}}'

# Get current branch
curl -X POST http://localhost:3002/mcp/v1/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name":"git_current_branch","arguments":{}}'
```

---

## 📱 Використання з Android

### Android Emulator
```kotlin
// В додатку підключіться до:
"http://10.0.2.2:3002"
```

Emulator перенаправляє `10.0.2.2` на `localhost` хост-машини.

### Physical Device (WiFi)

1. Знайдіть IP вашого ПК:
```bash
# Windows
ipconfig

# Linux/Mac
ifconfig
```

2. Підключіться до:
```kotlin
"http://192.168.1.100:3002"  // Ваш IP
```

### USB Connection (adb reverse)
```bash
# Налаштуйте port forwarding
adb reverse tcp:3002 tcp:3002

# Тепер підключайтесь до:
"http://localhost:3002"
```

---

## ❓ Troubleshooting

### "Connection refused"

**Проблема:** Не можу підключитись до сервера

**Рішення:**
1. Перевірте що сервер запущено: `http://localhost:3002/health`
2. Для емулятора використовуйте `10.0.2.2`, не `localhost`
3. Перевірте firewall (може блокувати порт 3002)

### "Git command failed"

**Проблема:** Git команди не працюють

**Рішення:**
1. Перевірте що Git встановлено: `git --version`
2. Перевірте що ви в git репозиторії
3. Перегляньте логи сервера (консоль де запущено)

### "Python module not found"

**Проблема:** `ImportError: No module named flask`

**Рішення:**
```bash
pip install -r requirements.txt
```

---

## 🔐 Безпека

**ВАЖЛИВО:**
- Сервер дозволяє виконувати ТІЛЬКИ git команди
- Інші команди (`execute_command` з command != 'git') блокуються
- Використовуйте тільки в trusted мережах

**Приклад блокування:**
```bash
# Це НЕ спрацює:
execute_command(command="rm", args=["-rf", "/"])
# Повернення: "Error: Only git commands are allowed"
```

---

## 🚀 Запуск всіх серверів разом

Для повного функціоналу запустіть всі MCP сервери:

```bash
start_all_WITH_GIT.bat
```

Це запустить:
- 🌐 Web Search (port 3000)
- 💾 File System (port 3001)
- 🔀 Git Operations (port 3002)

В додатку підключіться до кожного окремо або налаштуйте multi-MCP.

---

## 📊 Архітектура

```
Android App
    ↓ HTTP Request
Git MCP Server (Python/Flask)
    ↓ subprocess.run()
Git CLI
    ↓
Repository (D:\AndroidStudioProjects\ChatAgent)
```

---

## 🎯 Інтеграція з ChatAgent

### GitCommandHandler
```kotlin
// В AndroidManifest.xml вже налаштовано:
android:usesCleartextTraffic="true"

// McpClient автоматично підключається до:
val serverUrl = "http://10.0.2.2:3002"

// Виклик інструменту:
mcpClient.callTool(
    toolName = "execute_command",
    arguments = mapOf(
        "command" to "git",
        "args" to listOf("status", "--short", "--branch")
    )
)
```

### Fallback
Якщо MCP не підключено, GitCommandHandler автоматично використовує локальний git (ProcessBuilder).

---

## 📝 Логи

Сервер виводить детальні логи:

```
[MCP] Tool call: git_status
[MCP] Arguments: {}
[MCP] Result: ## feature
M  app/src/main/java/MyFile.kt...
```

---

## ✅ Готово!

Тепер Git команди працюють на Android через MCP! 🎉

**Наступні кроки:**
1. Запустіть `start_git.bat`
2. Запустіть `test_git_server.bat`
3. Підключіться в додатку
4. Спробуйте `/git status`
