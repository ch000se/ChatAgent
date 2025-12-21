# 🧪 Швидке тестування Daily Summary

## ⚡ Спосіб 1: Одна лінія коду (30 секунд)

**В `MainActivity.kt:18` додайте:**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 🧪 TEST: Запустити Daily Summary зараз
    com.example.chatagent.presentation.debug.TestDailySummary.triggerNow(this)

    enableEdgeToEdge()
    // ... решта коду
}
```

**Що станеться:**
1. При запуску додатка Worker запуститься негайно
2. Через 5-10 секунд побачите нотифікацію
3. Перевірте logcat: `DailySummaryWorker`

---

## ⚡ Спосіб 2: ADB команда (без збірки!)

```bash
# Запустити worker вручну
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS \
  -p com.example.chatagent

# Або форсувати виконання через WorkManager
adb shell cmd jobscheduler run -f com.example.chatagent 1
```

---

## ⚡ Спосіб 3: Змінити час на 1 хвилину

**В `MyApp.kt:26-27` змініть на:**

```kotlin
companion object {
    // Поточний час + 1 хвилина
    const val DAILY_SUMMARY_HOUR = 15  // ← Ваш поточний час + 0
    const val DAILY_SUMMARY_MINUTE = 45  // ← Ваша поточна хвилина + 1
}
```

Перезапустіть додаток і зачекайте 1 хвилину.

---

## 📋 Передумови для тесту

### 1. MCP сервер запущений
```bash
python mcp_reminder_server.py
```

Побачите:
```
🚀 Starting MCP Reminder Server...
📍 Server will run on http://localhost:3000
```

### 2. MCP підключений в додатку
1. MCP Tools → "Localhost Reminder Server" → Connect
2. Побачите 5 інструментів

### 3. Є тестові дані
```
В чаті:
Додай нагадування:
- Купити молоко (high priority, дедлайн сьогодні)
- Зателефонувати Олегу (середній пріоритет, дедлайн завтра)
- Завершити звіт (високий пріоритет, дедлайн 2025-12-25)
```

---

## 🔍 Що перевіряти

### 1. Logcat

```bash
adb logcat -s DailySummaryWorker MyApp
```

**Очікувані логи:**

```
D/MyApp: Daily summary scheduled for ... (in X minutes)
D/DailySummaryWorker: Starting daily summary generation...
D/DailySummaryWorker: Sending request to Claude with 5 MCP tools...
D/DailySummaryWorker: Summary generated: 📊 Daily Reminder Summary...
D/DailySummaryWorker: ✅ Successfully sent to Android Notification
D/DailySummaryWorker: Daily summary sent to 1/1 channels
```

### 2. Нотифікація

Повинна з'явитися нотифікація:
- **Заголовок:** 📊 Daily Reminder Summary
- **Текст:** Beautiful formatted summary від Claude
- **Розгорнути:** Побачите повний summary з insights

### 3. МCP Server Logs

В терміналі де запущено `mcp_reminder_server.py`:

```
INFO: 127.0.0.1:XXXXX - "POST /mcp HTTP/1.1" 200 OK
{"method": "tools/list", ...}
{"method": "tools/call", "params": {"name": "get_summary", ...}}
```

---

## ❌ Troubleshooting

### Worker не запускається

**Перевірка:**
```kotlin
// Додайте в MainActivity
val workInfo = WorkManager.getInstance(this)
    .getWorkInfosForUniqueWork("daily_summary_work")
    .get()
Log.d("TEST", "Work state: ${workInfo.firstOrNull()?.state}")
```

**Якщо BLOCKED/CANCELLED:**
```kotlin
// Видаліть старий work і створіть новий
WorkManager.getInstance(this).cancelUniqueWork("daily_summary_work")
TestDailySummary.triggerNow(this)
```

### MCP не підключений

**Симптом в логах:**
```
D/DailySummaryWorker: MCP not connected, skipping daily summary
```

**Рішення:**
1. Переконайтесь що `mcp_reminder_server.py` запущений
2. MCP Tools → Connect
3. Побачите "Connected, tools: 5"

### Claude не відповідає

**Симптом:**
```
E/DailySummaryWorker: Daily summary error: timeout
```

**Рішення:**
1. Перевірте API ключ в BuildConfig
2. Перевірте інтернет з'єднання
3. Спробуйте в чаті - якщо чат працює, worker теж має працювати

### Нотифікація не показується

**Рішення:**
1. Настройки → Додатки → ChatAgent → Нотифікації → Дозволено?
2. Перевірте logcat - чи worker відпрацював успішно?

---

## ✅ Приклад успішного тесту

```bash
# Terminal 1: MCP Server
$ python mcp_reminder_server.py
🚀 Starting MCP Reminder Server...
📍 Server will run on http://localhost:3000

# Terminal 2: Logcat
$ adb logcat -s DailySummaryWorker

D/DailySummaryWorker: Starting daily summary generation...
D/DailySummaryWorker: Sending request to Claude with 5 MCP tools...
D/DailySummaryWorker: Summary generated: 📊 Daily Reminder Summary

✅ Completed Tasks (2)
━━━━━━━━━━━━━━━━━━━━━━
🎯 Bought milk
📧 Called Oleg

📝 Pending Tasks (1 total)
━━━━━━━━━━━━━━━━━━━━━━
🔴 Finish report (high priority, due in 8 days)

💡 Insights:
━━━━━━━━━━━━━━━━━━━━━━
- Great job! 2 tasks completed today 🎉
- 1 high priority task remaining

🎯 Top Priority: Finish report...

D/DailySummaryWorker: ✅ Successfully sent to Android Notification
D/DailySummaryWorker: Daily summary sent to 1/1 channels

# Додаток: Нотифікація з'явилась!
```

---

## 🎯 Швидкий чеклист

- [ ] MCP сервер запущений (`python mcp_reminder_server.py`)
- [ ] MCP підключений в додатку (MCP Tools → Connect)
- [ ] Є тестові нагадування (додати через чат)
- [ ] Додано `TestDailySummary.triggerNow(this)` в MainActivity
- [ ] Запущено додаток
- [ ] Очікується 5-10 секунд
- [ ] Нотифікація з'явилась! ✅

---

**Готово! Тепер можете швидко тестувати Daily Summary!** 🚀
