# 📝 RAG Code Examples - Практичні приклади використання

**ВАЖЛИВО**: Цей файл містить псевдокод для навчання. Код не компілюється і призначений для розуміння концепцій RAG.

---

## ПРИКЛАД 1: Базовий RAG-запит

```kotlin
suspend fun example1_BasicRagQuery(
    documentRepository: DocumentRepository,
    indexDocumentUseCase: IndexDocumentUseCase,
    searchDocumentsUseCase: SearchDocumentsUseCase,
    chatRepository: ChatRepository
) {
    // Крок 1: Індексуємо документ
    val documentContent = """
        Політика відпусток CompanyX:
        - 20 днів на рік для всіх співробітників
        - 25 днів після 3 років роботи
    """.trimIndent()

    val addResult = documentRepository.addDocument(
        fileName = "vacation_policy.txt",
        content = documentContent,
        contentType = "text/plain"
    )

    val documentId = addResult.getOrNull()?.id ?: return

    // Крок 2: Індексуємо (розбивка на чанки + векторизація)
    indexDocumentUseCase(documentId).collect { progress ->
        println("Indexing: ${progress.currentStatus}")
    }

    // Крок 3: Пошук релевантних чанків
    val query = "Скільки днів відпустки?"
    val searchResult = searchDocumentsUseCase(query, topK = 3)

    searchResult.onSuccess { chunks ->
        // Крок 4: Формуємо контекст
        val context = chunks.joinToString("\n\n") { chunk ->
            "[${chunk.document.fileName}] ${chunk.chunk.text}"
        }

        // Крок 5: Запит до LLM з контекстом
        val prompt = """
            Контекст: $context
            Питання: $query
            Відповідь:
        """.trimIndent()

        chatRepository.sendMessage(prompt).onSuccess { message ->
            println("Відповідь з RAG: ${message.content}")
            // Очікуваний результат: "20 днів (або 25 після 3 років)"
        }
    }
}
```

---

## ПРИКЛАД 2: Порівняння з RAG та без RAG

```kotlin
suspend fun example2_CompareWithAndWithoutRag(
    searchDocumentsUseCase: SearchDocumentsUseCase,
    chatRepository: ChatRepository
) {
    val query = "Яка політика remote work у CompanyX?"

    // ===== БЕЗ RAG =====
    val promptWithoutRag = """
        Ти — AI-асистент. Відповідай на питання.

        Питання: $query
        Відповідь:
    """.trimIndent()

    chatRepository.sendMessage(promptWithoutRag).onSuccess { message ->
        println("БЕЗ RAG: ${message.content}")
        // Очікуваний результат: загальна інформація про remote work
        // "Зазвичай компанії дозволяють працювати віддалено 1-2 дні на тиждень..."
    }

    // ===== З RAG =====
    val searchResult = searchDocumentsUseCase(query, topK = 3)
    searchResult.onSuccess { chunks ->
        val context = chunks.joinToString("\n") { it.chunk.text }

        val promptWithRag = """
            Відповідай ТІЛЬКИ на основі контексту.

            Контекст: $context
            Питання: $query
            Відповідь:
        """.trimIndent()

        chatRepository.sendMessage(promptWithRag).onSuccess { message ->
            println("З RAG: ${message.content}")
            // Очікуваний результат: конкретна інформація з документа
            // "Згідно з політикою CompanyX, розробники можуть працювати віддалено до 3 днів на тиждень"
        }
    }
}
```

---

## ПРИКЛАД 3: Аналіз релевантності чанків

```kotlin
suspend fun example3_AnalyzeChunkRelevance(
    searchDocumentsUseCase: SearchDocumentsUseCase
) {
    val query = "Як оформити відпустку?"

    searchDocumentsUseCase(query, topK = 5).onSuccess { results ->
        println("=== Аналіз релевантності ===")

        results.forEachIndexed { index, result ->
            val similarityPercent = result.similarity * 100

            val relevanceLevel = when {
                result.similarity > 0.7 -> "🟢 ВИСОКА"
                result.similarity > 0.4 -> "🟡 СЕРЕДНЯ"
                else -> "🔴 НИЗЬКА"
            }

            println("""
                Ранг ${index + 1}:
                  Релевантність: $relevanceLevel (${String.format("%.1f", similarityPercent)}%)
                  Документ: ${result.document.fileName}
                  Чанк #${result.chunk.chunkIndex}: ${result.chunk.text.take(60)}...
            """.trimIndent())
        }
    }
}
```

**Приклад виводу:**
```
Ранг 1:
  Релевантність: 🟢 ВИСОКА (87.3%)
  Документ: vacation_policy.txt
  Чанк #2: Для оформлення відпустки потрібно: 1. Подати заявку в...

Ранг 2:
  Релевантність: 🟡 СЕРЕДНЯ (54.2%)
  Документ: hr_handbook.txt
  Чанк #5: Документи для HR: заявка на відпустку, лікарняний...

Ранг 3:
  Релевантність: 🔴 НИЗЬКА (23.1%)
  Документ: it_policy.txt
  Чанк #1: Оформлення доступу до систем...
```

---

## ПРИКЛАД 4: Гібридний режим (RAG тільки коли потрібно)

```kotlin
suspend fun example4_HybridMode(
    userQuery: String,
    searchDocumentsUseCase: SearchDocumentsUseCase,
    chatRepository: ChatRepository
) {
    // Визначаємо, чи потрібен RAG для цього запиту
    val requiresRag = detectIfRagNeeded(userQuery)

    if (requiresRag) {
        println("Запит вимагає знань з бази → використовуємо RAG")
        performRagQuery(userQuery, searchDocumentsUseCase, chatRepository)
    } else {
        println("Загальний запит → звичайний LLM")
        performSimpleQuery(userQuery, chatRepository)
    }
}

fun detectIfRagNeeded(query: String): Boolean {
    // Ключові слова, що вказують на потребу в контексті
    val ragKeywords = listOf(
        "скільки", "як", "коли", "де", "хто", "політика", "правило",
        "документація", "інструкція", "процес", "процедура"
    )

    // Ключові слова для креативних/загальних запитів
    val noRagKeywords = listOf(
        "напиши", "створи", "згенеруй", "розкажи історію",
        "що таке", "поясни"
    )

    val queryLower = query.lowercase()

    return when {
        noRagKeywords.any { queryLower.contains(it) } -> false
        ragKeywords.any { queryLower.contains(it) } -> true
        else -> true // За замовчуванням використовуємо RAG (безпечніше)
    }
}
```

---

## ПРИКЛАД 5: Метрики якості RAG

```kotlin
suspend fun example5_RagQualityMetrics(
    searchDocumentsUseCase: SearchDocumentsUseCase
) {
    val query = "Скільки днів відпустки?"

    searchDocumentsUseCase(query, topK = 5).onSuccess { results ->
        // Метрика 1: Середня релевантність
        val avgSimilarity = results.map { it.similarity }.average()
        println("Середня релевантність: ${String.format("%.2f", avgSimilarity)}")

        // Метрика 2: Топ-1 релевантність (найважливіша)
        val top1Similarity = results.firstOrNull()?.similarity ?: 0f
        println("Top-1 релевантність: ${String.format("%.2f", top1Similarity)}")

        // Метрика 3: Coverage (скільки документів покривають запит)
        val uniqueDocuments = results.map { it.document.fileName }.distinct().size
        println("Покриття: $uniqueDocuments документ(и)")

        // Метрика 4: Поріг релевантності
        val relevantChunks = results.filter { it.similarity > 0.4 }
        println("Релевантні чанки (>40%): ${relevantChunks.size}/${results.size}")

        // Оцінка якості RAG
        val ragQuality = when {
            top1Similarity > 0.7 && avgSimilarity > 0.5 -> "🟢 ВІДМІННО - RAG дуже ефективний"
            top1Similarity > 0.4 && avgSimilarity > 0.3 -> "🟡 ДОБРЕ - RAG корисний"
            top1Similarity > 0.2 -> "🟠 ПОМІРНО - RAG може допомогти, але є шум"
            else -> "🔴 ПОГАНО - RAG неефективний, використай звичайний LLM"
        }

        println("Якість RAG: $ragQuality")
    }
}
```

---

## ПРИКЛАД 6: Fallback стратегія

```kotlin
suspend fun example6_RagWithFallback(
    query: String,
    searchDocumentsUseCase: SearchDocumentsUseCase,
    chatRepository: ChatRepository
) {
    val searchResult = searchDocumentsUseCase(query, topK = 3)

    searchResult.onSuccess { chunks ->
        // Перевіряємо якість знайдених чанків
        val top1Similarity = chunks.firstOrNull()?.similarity ?: 0f

        when {
            // Випадок 1: Висока релевантність → використовуємо RAG
            top1Similarity > 0.6 -> {
                println("Знайдено релевантний контекст (${String.format("%.1f", top1Similarity * 100)}%) → RAG")

                val context = chunks.joinToString("\n") { it.chunk.text }
                val prompt = """
                    Відповідай на основі контексту.
                    Контекст: $context
                    Питання: $query
                """.trimIndent()

                chatRepository.sendMessage(prompt)
            }

            // Випадок 2: Низька релевантність → fallback до LLM
            top1Similarity < 0.3 -> {
                println("Контекст не релевантний (${String.format("%.1f", top1Similarity * 100)}%) → звичайний LLM")

                val prompt = "Питання: $query\nВідповідь:"
                chatRepository.sendMessage(prompt)
            }

            // Випадок 3: Середня релевантність → гібридний підхід
            else -> {
                println("Часткова релевантність (${String.format("%.1f", top1Similarity * 100)}%) → гібридний режим")

                val context = chunks.joinToString("\n") { it.chunk.text }
                val prompt = """
                    Ти можеш використати наданий контекст, але також додай власні знання.

                    Контекст (може бути частково релевантний): $context

                    Питання: $query

                    Відповідь (використай контекст якщо релевантний, інакше — загальні знання):
                """.trimIndent()

                chatRepository.sendMessage(prompt)
            }
        }
    }
}
```

---

## 📝 Використання в реальному коді

Для реальної реалізації дивіться:

1. **RagComparisonViewModel.kt** - повна робоча реалізація:
   - `queryWithoutRAG()` - режим без RAG
   - `queryWithRAG()` - режим з RAG
   - `compareResponses()` - порівняння обох

2. **DocumentRepositoryImpl.kt** - низькорівнева реалізація:
   - `indexDocument()` - індексація документів
   - `searchDocuments()` - пошук релевантних чанків
   - `generateEmbedding()` - TF-IDF векторизація
   - `cosineSimilarity()` - обчислення схожості

3. **SearchDocumentsUseCase.kt** - use case для пошуку

---

## 🎯 Ключові висновки

### Коли використовувати RAG:
✅ Domain-specific знання
✅ Корпоративні політики
✅ Приватні дані
✅ Технічна документація

### Коли НЕ використовувати RAG:
❌ Загальні знання (Google-подібні запити)
❌ Креативні завдання
❌ Математика/логіка
❌ Розмовні фрази

---

**Автор**: ChatAgent Team
**Дата**: 2025-12-23
**Призначення**: Освітній матеріал для вивчення RAG
