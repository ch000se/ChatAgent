# Інструкція по налаштуванню MCP Server та копіюванню документації

## Частина 1: Копіювання документації в assets/docs/

### Крок 1: Створіть папку assets/docs/
```
D:\AndroidStudioProjects\ChatAgent\app\src\main\assets\docs\
```

**Детально:**
1. Відкрийте провідник Windows
2. Перейдіть в `D:\AndroidStudioProjects\ChatAgent\app\src\main\`
3. Якщо папки `assets` немає - створіть її (Правий клік → Створити → Папка)
4. Зайдіть в папку `assets`
5. Створіть папку `docs`

### Крок 2: Скопіюйте всі .md файли з кореня проекту

**З цієї папки:**
```
D:\AndroidStudioProjects\ChatAgent\
```

**Скопіюйте ці файли:**
- ✅ `BUILD_INSTRUCTIONS.md`
- ✅ `RAG_QUICKSTART.md`
- ✅ `RAG_DEMO_EXPLAINED.md`
- ✅ `RAG_TASK_COMPLETED.md`
- ✅ `RAG_COMPARISON_RESULTS.md`
- ✅ `RAG_CODE_EXAMPLES.md`
- ✅ `RAG_TEST_INSTRUCTIONS.md`
- ✅ `RAG_RERANKING_FILTERING.md`
- ✅ `RAG_RERANKING_TEST_GUIDE.md`
- ✅ `RAG_CHAT_TEST_GUIDE.md`

**В цю папку:**
```
D:\AndroidStudioProjects\ChatAgent\app\src\main\assets\docs\
```

### Крок 3: Перевірка

Після копіювання структура має виглядати так:
```
D:\AndroidStudioProjects\ChatAgent\
├── app\
│   └── src\
│       └── main\
│           ├── assets\
│           │   └── docs\
│           │       ├── BUILD_INSTRUCTIONS.md
│           │       ├── RAG_QUICKSTART.md
│           │       ├── RAG_DEMO_EXPLAINED.md
│           │       ├── RAG_TASK_COMPLETED.md
│           │       ├── RAG_COMPARISON_RESULTS.md
│           │       ├── RAG_CODE_EXAMPLES.md
│           │       ├── RAG_TEST_INSTRUCTIONS.md
│           │       ├── RAG_RERANKING_FILTERING.md
│           │       ├── RAG_RERANKING_TEST_GUIDE.md
│           │       └── RAG_CHAT_TEST_GUIDE.md
│           └── java\
│               └── ...
```

---

## Частина 2: Налаштування MCP Server для `/git` команди

### Що таке MCP?

MCP (Model Context Protocol) - це протокол для з'єднання додатків з зовнішніми інструментами. У нашому випадку, він дозволяє виконувати git команди через stdio JSON-RPC.

### Архітектура MCP в додатку

```
ChatAgent App → McpClient (Kotlin) → stdio/JSON-RPC → MCP Server (Node.js) → git команди
```

### Варіанти налаштування MCP Server

#### **Варіант 1: Використати готовий MCP Server (РЕКОМЕНДОВАНО)**

1. **Встановіть Node.js** (якщо ще не встановлено):
   - Завантажте з https://nodejs.org/ (версія LTS)
   - Перевірте встановлення: `node --version` та `npm --version`

2. **Встановіть MCP Server з підтримкою git:**

```bash
npm install -g @modelcontextprotocol/server-filesystem
```

3. **Створіть startup script для MCP Server:**

Створіть файл `D:\AndroidStudioProjects\ChatAgent\mcp-server\start-mcp.bat`:

```batch
@echo off
echo Starting MCP Server with filesystem and git support...
npx @modelcontextprotocol/server-filesystem "D:\AndroidStudioProjects\ChatAgent" --allow-commands git
pause
```

4. **Запустіть MCP Server:**
```
D:\AndroidStudioProjects\ChatAgent\mcp-server\start-mcp.bat
```

#### **Варіант 2: Створити власний MCP Server**

1. **Створіть папку для MCP Server:**
```
D:\AndroidStudioProjects\ChatAgent\mcp-server\
```

2. **Ініціалізуйте Node.js проект:**
```bash
cd D:\AndroidStudioProjects\ChatAgent\mcp-server
npm init -y
npm install @modelcontextprotocol/sdk commander
```

3. **Створіть server.js:**

```javascript
#!/usr/bin/env node

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

const PROJECT_ROOT = 'D:\\AndroidStudioProjects\\ChatAgent';

const server = new McpServer({
  name: 'git-mcp-server',
  version: '1.0.0'
});

// Register git commands tool
server.tool('execute_command', 'Execute git command', {
  command: { type: 'string', description: 'Command to execute' },
  args: { type: 'array', description: 'Command arguments', items: { type: 'string' } }
}, async ({ command, args }) => {
  if (command !== 'git') {
    throw new Error('Only git commands are allowed');
  }

  const fullCommand = `git ${args.join(' ')}`;

  try {
    const { stdout, stderr } = await execAsync(fullCommand, {
      cwd: PROJECT_ROOT,
      maxBuffer: 1024 * 1024 * 10
    });

    return {
      content: [
        {
          type: 'text',
          text: stdout || stderr || 'Command executed successfully'
        }
      ]
    };
  } catch (error) {
    return {
      content: [
        {
          type: 'text',
          text: `Error: ${error.message}`
        }
      ],
      isError: true
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('MCP Git Server running on stdio');
}

main().catch(console.error);
```

4. **Оновіть package.json:**

```json
{
  "name": "git-mcp-server",
  "version": "1.0.0",
  "type": "module",
  "main": "server.js",
  "bin": {
    "git-mcp-server": "./server.js"
  },
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "@modelcontextprotocol/sdk": "^0.5.0",
    "commander": "^11.1.0"
  }
}
```

5. **Зробіть скрипт виконуваним:**
```bash
chmod +x server.js
```

---

## Частина 3: Налаштування Android додатку для MCP

### Перевірка існуючого коду McpClient

McpClient вже реалізований в проекті:
```
app/src/main/java/com/example/chatagent/data/remote/client/McpClient.kt
```

### Налаштування підключення до MCP Server

**Варіант A: Використання HTTP Bridge (для тестування)**

Якщо MCP Server працює локально, потрібен HTTP bridge:

1. Створіть `mcp-http-bridge.js`:

```javascript
const express = require('express');
const { spawn } = require('child_process');
const app = express();

app.use(express.json());

app.post('/mcp', (req, res) => {
  const mcpProcess = spawn('node', ['server.js'], {
    cwd: 'D:\\AndroidStudioProjects\\ChatAgent\\mcp-server'
  });

  mcpProcess.stdin.write(JSON.stringify(req.body) + '\n');

  let response = '';
  mcpProcess.stdout.on('data', (data) => {
    response += data.toString();
  });

  mcpProcess.stdout.on('end', () => {
    res.json(JSON.parse(response));
  });
});

app.listen(3000, () => {
  console.log('MCP HTTP Bridge running on http://localhost:3000');
});
```

2. Запустіть bridge:
```bash
node mcp-http-bridge.js
```

**Варіант B: Локальний stdio MCP (потребує додаткових налаштувань Android)**

Для Android додатку stdio підключення потребує:
- Запуск MCP Server як окремого процесу на пристрої
- Використання Process API для stdio комунікації

---

## Частина 4: Тестування

### Тестування індексування документації

1. Перезберіть додаток після копіювання файлів в `assets/docs/`:
```bash
cd D:\AndroidStudioProjects\ChatAgent
gradlew.bat clean assembleDebug
```

2. Встановіть додаток:
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

3. Запустіть додаток та перегляньте logcat:
```bash
adb logcat | findstr "MyApp"
```

Очікуваний вивід:
```
📚 PROJECT DOCS AUTO-INDEXING
🔍 Scanning for project documents...
📄 Found 10 documents
⚙️ Indexing [1/10]: BUILD_INSTRUCTIONS.md
⚙️ Indexing [2/10]: RAG_QUICKSTART.md
...
✅ Indexing completed: 10 indexed, 0 skipped
```

### Тестування `/help` команди

1. Відкрийте чат в додатку
2. Натисніть на чіп `/help` або введіть `/help RAG`
3. Очікуваний результат:

```
Command • 234ms

Based on project documentation:

RAG (Retrieval-Augmented Generation) is implemented using:
- TF-IDF vectorization for document embeddings
- Room database for persistence
...

Sources (5):
• RAG_QUICKSTART.md (89%)
• RAG_DEMO_EXPLAINED.md (76%)
• RAG_CODE_EXAMPLES.md (65%)
```

### Тестування `/git` команди (коли MCP налаштовано)

1. Переконайтесь що MCP Server запущений
2. В додатку введіть `/git` або `/git log`
3. Очікуваний результат:

```
Command • 567ms

Git Status:
On branch feature
Your branch is up to date with 'origin/feature'.

Changes staged for commit:
  modified:   app/src/main/java/...

Sources: MCP Server
```

---

## Частина 5: Відлагодження помилок

### Помилка: "MCP not connected"

**Симптом:** При виконанні `/git` команди з'являється повідомлення "❌ MCP Server is not connected"

**Рішення:**
1. Перевірте чи запущений MCP Server
2. Перевірте чи правильно налаштований McpClient в коді
3. Подивіться логи: `adb logcat | findstr "McpClient"`

### Помилка: "Found 0 documents"

**Симптом:** При запуску додатку в логах: "Found 0 project documents"

**Рішення:**
1. Перевірте чи файли скопійовані в правильну папку: `app/src/main/assets/docs/`
2. Перезберіть додаток: `gradlew.bat clean assembleDebug`
3. Перевстановіть APK

### Помилка: "Query words not in vocabulary"

**Симптом:** Similarity scores = 0.0000 для всіх результатів

**Рішення:**
1. Це нормально якщо документи ще не проіндексовані
2. Дочекайтесь завершення індексування (перегляньте logcat)
3. Очистіть дані додатку та перезапустіть

---

## Швидкий старт (TL;DR)

### Для індексування документації:
```bash
# 1. Створіть папку
mkdir D:\AndroidStudioProjects\ChatAgent\app\src\main\assets\docs

# 2. Скопіюйте файли
copy D:\AndroidStudioProjects\ChatAgent\*.md D:\AndroidStudioProjects\ChatAgent\app\src\main\assets\docs\

# 3. Зберіть додаток
cd D:\AndroidStudioProjects\ChatAgent
gradlew.bat assembleDebug

# 4. Встановіть
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Для MCP Server:
```bash
# 1. Встановіть Node.js (якщо потрібно)

# 2. Встановіть MCP Server
npm install -g @modelcontextprotocol/server-filesystem

# 3. Створіть startup script
echo npx @modelcontextprotocol/server-filesystem "D:\AndroidStudioProjects\ChatAgent" --allow-commands git > start-mcp.bat

# 4. Запустіть
start-mcp.bat
```

---

## Контрольний список

- [ ] Створено папку `app/src/main/assets/docs/`
- [ ] Скопійовано всі .md файли в assets/docs/
- [ ] Додаток перезібрано після копіювання файлів
- [ ] APK встановлено на пристрій/емулятор
- [ ] Logcat показує "10 indexed" при запуску
- [ ] `/help` команда повертає результати з similarity > 0
- [ ] Node.js встановлено (для MCP)
- [ ] MCP Server запущений
- [ ] `/git` команда працює

---

**Автор:** Claude Sonnet 4.5
**Дата:** 2026-01-13
**Версія:** 1.0
