# 🚀 Daily Summary with LLM - Швидкий старт (2 хвилини)

## 🎯 Що це робить?

**Раз в день (9:00 ранку):**
1. 📡 Worker "пінгує" Claude з промптом: "Дай summary моїх нагадувань"
2. 🤖 Claude викликає MCP tools (`list_reminders`, `get_summary`)
3. ✨ Claude формує **beautiful formatted** summary
4. 📤 Summary пушиться вам через нотифікацію (Email/Telegram опціонально)

---

# 🚀 Швидкий старт

## Крок 1: Запустити сервер

```bash
# Встановити залежності (один раз)
pip install fastapi uvicorn

# Запустити MCP Reminder Server
python mcp_reminder_server.py
```

✅ Побачите:
```
🚀 Starting MCP Reminder Server...
📍 Server will run on http://localhost:3000
📱 Android emulator: http://10.0.2.2:3000/mcp
```

## Крок 2: Підключитися з додатка

1. Відкрити **MCP Tools**
2. Вибрати **"Localhost (Recommended for Testing)"**
3. Натиснути **Connect**
4. ✅ Побачите 5 інструментів: `add_reminder`, `list_reminders`, `complete_reminder`, `delete_reminder`, `get_summary`

## Крок 3: Використовувати в чаті

### Додати нагадування:
```
Додай нагадування купити молоко

Нагадай завтра зателефонувати Олегу

Add high priority task: finish report by 2025-12-25
```

### Переглянути:
```
Покажи мої нагадування

Дай summary
```

### Виконати/Видалити:
```
Виконано завдання 3

Видали нагадування 5
```

## 🔔 Автоматичні daily summary

- ✅ Запускаються **автоматично** раз в день (9:00 ранку)
- ✅ **LLM формує** beautiful summary ваших нагадувань
- ✅ Показують **нотифікацію** з insights та recommendations
- ✅ Працюють **24/7** в фоні
- ✅ Можна налаштувати Email/Telegram/Webhook

## 📊 Приклад summary від Claude:

```
📊 Daily Reminder Summary

✅ Completed Tasks (3)
━━━━━━━━━━━━━━━━━━━━━━
🎯 Finished project report
📧 Sent emails to team
✍️ Wrote documentation

📝 Pending Tasks (5 total)
━━━━━━━━━━━━━━━━━━━━━━
⚠️ OVERDUE (2):
  🔴 Call client (2 days overdue)

⏰ Due TODAY (1):
  🟡 Team standup meeting

💡 Insights:
━━━━━━━━━━━━━━━━━━━━━━
- You completed 3 tasks today! 🎉
- 2 tasks overdue - prioritize! ⚡

🎯 Top Priority: Call client
```

## ⏰ Налаштування часу

За замовчуванням: **9:00 ранку**

Змінити в `MyApp.kt:26-27`:
```kotlin
const val DAILY_SUMMARY_HOUR = 20  // 20:00 вечора
const val DAILY_SUMMARY_MINUTE = 0
```

## 📤 Додаткові канали (опціонально)

В `DailySummaryWorker.kt:149-170` можна увімкнути:
- 📧 Email
- 💬 Telegram
- 🔗 Webhook (Discord, Slack, etc.)

## ✅ Готово!

Тепер у вас є **інтелектуальний агент 24/7** який:
- ✅ Раз в день "пінгує" Claude з промптом
- ✅ Claude **сам викликає** MCP tools для аналізу
- ✅ Claude формує **beautiful summary** з insights
- ✅ Summary **автоматично пушиться** вам
- ✅ Працює **в фоні** навіть коли додаток закритий

**Це справжній AI-асистент!** 🤖✨

**Детальна документація:** `DAILY_SUMMARY_GUIDE.md`
