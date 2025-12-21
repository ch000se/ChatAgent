# 🚀 Швидкий гайд: Кастомний пошук

## Що тепер можна робити?

Тепер можете **вводити будь-який запит** і отримувати реальні результати з інтернету!

---

## Покрокова інструкція

### Крок 1: Запустіть MCP сервери

```bash
cd mcp_servers
start_all_REAL.bat
```

Відкриються 2 вікна - **НЕ ЗАКРИВАЙТЕ ЇХ!**

---

### Крок 2: Відкрийте додаток

1. Запустіть **ChatAgent** в Android Studio
2. Натисніть іконку **дерева 🌳** в топ-барі
3. Відкриється екран **MCP Pipeline Agent**

---

### Крок 3: Введіть свій запит

Зверху екрану побачите **"🔍 Custom Search"** карточку з полем вводу.

**Спробуйте будь-що:**
```
latest SpaceX news
kotlin android tips 2025
quantum computing breakthroughs
Ukraine news today
machine learning tutorials
```

---

### Крок 4: Натисніть "Run Custom Search"

Почнеться виконання пайплайна:

```
[14:30:15] Pipeline started: Custom Search: your query
[14:30:16] Executing: Search Web
[14:30:18] ✓ Search Web completed
[14:30:18]   Output: Found 3 articles...
[14:30:18] Executing: Create Summary
[14:30:19] ✓ Create Summary completed
[14:30:19] Executing: Save to File
[14:30:20] ✓ Save to File completed
[14:30:20] Pipeline completed successfully!
```

---

### Крок 5: Перевірте результат

**В додатку:**
- Прокрутіть вниз до **"Final Output"**
- Побачите суммаризацію 3 статей

**На комп'ютері:**
```bash
cd mcp_servers/output
type your_query_summary.txt    # Windows
cat your_query_summary.txt     # Linux/Mac
```

---

## Що відбувається під капотом?

```
Ваш запит: "latest SpaceX news"
    ↓
    1. Пошук в DuckDuckGo
       → Знаходить 3 РЕАЛЬНІ статті з інтернету
    ↓
    2. Суммаризація
       → Створює короткий текст (500 символів)
    ↓
    3. Збереження
       → Файл: mcp_servers/output/spacex_summary.txt
```

---

## Приклади використання

### 1. Новини про технології
```
Запит: "latest iPhone 15 news"

Результат:
📄 iPhone 15 Pro Max Review - The Verge
📄 Apple Announces New Features - TechCrunch
📄 iPhone 15 Sales Numbers - Bloomberg
```

### 2. Навчальні матеріали
```
Запит: "kotlin coroutines tutorial"

Результат:
📄 Kotlin Coroutines Guide - kotlinlang.org
📄 Async Programming in Kotlin - Medium
📄 Coroutines Best Practices - Android Developers
```

### 3. Наукові дослідження
```
Запит: "quantum computing 2024"

Результат:
📄 Quantum Computing Breakthroughs - Nature
📄 IBM Quantum Roadmap - IBM Research
📄 Quantum Algorithms Explained - ArXiv
```

---

## Налаштування запиту

### Де міняти кількість результатів?

**Файл:** `PipelineViewModel.kt` (рядок 191)

```kotlin
arguments = mapOf(
    "query" to searchQuery,
    "count" to 3  // ← Змініть на 5, 10 тощо
)
```

### Де міняти довжину суммаризації?

**Файл:** `PipelineViewModel.kt` (рядок 201)

```kotlin
arguments = mapOf(
    "text" to "\${PREVIOUS_OUTPUT}",
    "max_length" to 500  // ← Змініть на 1000, 2000 тощо
)
```

### Де міняти шлях збереження?

**Файл:** `PipelineViewModel.kt` (рядок 210)

```kotlin
arguments = mapOf(
    "path" to "/sdcard/Download/my_summary.txt",  // ← Ваш шлях
    "content" to "\${PREVIOUS_OUTPUT}"
)
```

---

## Поради

### ✅ Добрі запити
- "latest AI developments"
- "SpaceX Starship launch"
- "kotlin best practices"
- "climate change news"

### ❌ Погані запити
- "" (порожній)
- "asdfghjkl" (безглуздий текст)
- Занадто довгі запити (>100 символів)

### 💡 Час виконання

- **Швидкий запит** (популярна тема): 1-2 секунди
- **Середній запит**: 2-4 секунди
- **Складний запит**: 4-6 секунд

Якщо довше - DuckDuckGo може блокувати. Почекайте 10-20 сек і спробуйте знову.

---

## Troubleshooting

### Проблема: "No results found"

**Причина:** DuckDuckGo не повернув результати

**Рішення:**
1. Почекайте 10-20 секунд
2. Спробуйте інший запит
3. Сервер автоматично спробує Wikipedia

### Проблема: Повільно працює

**Причина:** Реальний пошук в інтернеті займає час

**Це нормально:**
- Demo режим: ~10ms
- Real режим: ~2000ms (2 секунди)

### Проблема: Помилка підключення

**Причина:** MCP сервери не запущені

**Рішення:**
```bash
cd mcp_servers
start_all_REAL.bat
```

---

## Структура UI

```
┌─────────────────────────────────────────────┐
│ MCP Pipeline Agent            [🌳 icon]     │
├─────────────────────────────────────────────┤
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ 🔍 Custom Search                        │ │
│ │                                         │ │
│ │ [Enter search query____________]  🔍   │ │
│ │                                         │ │
│ │ [  ▶  Run Custom Search  ]             │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ─────────────────────────────────────────── │
│                                             │
│ Pre-configured Pipelines                    │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Web Search & Save                       │ │
│ │ Search the web, create summary, save    │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ─────────────────────────────────────────── │
│                                             │
│ Execution Log                               │
│                                             │
│ [14:30:15] Pipeline started                 │
│ [14:30:16] Executing: Search Web            │
│ [14:30:18] ✓ Search Web completed           │
│ ...                                         │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ ✓ Final Output                          │ │
│ │ Summary (450 chars): ...                │ │
│ └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## Що далі?

1. **Спробуйте різні запити** - тепер працює все!
2. **Змініть параметри** - кількість результатів, довжину суммаризації
3. **Створіть свої пайплайни** - додайте нові кроки
4. **Інтегруйте інші сервіси** - Google Drive, Claude API тощо

---

🎉 **Готово! Тепер можете шукати що завгодно!**

Детальне пояснення: **HOW_IT_WORKS_STEP_BY_STEP.md**
