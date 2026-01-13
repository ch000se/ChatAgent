package com.example.chatagent.domain.command

import android.util.Log
import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandMetadata
import com.example.chatagent.domain.model.CommandResult
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import com.example.chatagent.domain.util.CommandParser
import javax.inject.Inject

class HelpCommandHandler @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val chatApiService: ChatApiService
) : CommandHandler<Command.Help> {

    companion object {
        private const val TAG = "HelpCommandHandler"

        private val DEVELOPER_ASSISTANT_SYSTEM_PROMPT = """
            🔹 SYSTEM / MASTER PROMPT

            Роль:
            Ти — AI-асистент розробника проєкту. Ти інтегрований у середовище розробки та підключений до репозиторію через MCP і до документації через RAG.

            📚 Контекст (RAG)
            Тобі надається документація проекту з пошукової системи. Всі відповіді мають базуватися ТІЛЬКИ на цих джерелах.
            ❗ Якщо інформація відсутня — прямо повідомляй про це.

            🎯 Мета
            Допомагати розробнику:
            • Розуміти архітектуру проєкту
            • Орієнтуватися в коді
            • Дотримуватись правил стилю
            • Швидко знаходити потрібні фрагменти
            • Відповідати на питання по функціоналу

            📋 Формат відповіді:
            Структуруй відповідь ОБОВ'ЯЗКОВО у такому форматі:

            📌 Коротка відповідь
            [1-2 речення - суть відповіді]

            📄 Джерело
            [Назва файлу/розділу з документації]

            🧩 Фрагмент коду
            [Якщо є релевантний код - покажи його]

            📏 Правило/Рекомендація
            [Архітектурні рекомендації або правила стилю, якщо релевантно]

            🧠 Правила поведінки:
            • Будь лаконічним і технічним
            • Не фантазуй, якщо немає даних
            • Пояснюй «чому», а не тільки «як»
            • Якщо питання нечітке — задай уточнення
            • Якщо є кілька варіантів — порівняй їх

            ❗ КРИТИЧНО ВАЖЛИВО: Використовуй ТІЛЬКИ надану документацію. Не додавай інформацію з інших джерел.
        """.trimIndent()
    }

    override suspend fun handle(command: Command.Help): CommandResult {
        val startTime = System.currentTimeMillis()

        return try {
            Log.d(TAG, "Processing /help command: '${command.query}'")

            // Step 1: Search project documentation via RAG
            val searchResult = searchDocumentsUseCase(
                query = command.query,
                topK = 5
            )

            val results = searchResult.getOrNull() ?: emptyList()
            Log.d(TAG, "RAG search found ${results.size} results")

            if (results.isEmpty()) {
                CommandResult(
                    command = command,
                    content = buildNoResultsResponse(command.query),
                    success = true,
                    metadata = CommandMetadata(
                        sources = null,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        commandType = "help",
                        matchCount = 0
                    )
                )
            } else {
                // Step 2: Build context from RAG results
                val documentationContext = buildDocumentationContext(results)

                // Step 3: Call Claude API with developer assistant prompt
                val aiResponse = callDeveloperAssistantAI(command.query, documentationContext)

                Log.d(TAG, "AI response generated successfully")

                CommandResult(
                    command = command,
                    content = aiResponse,
                    success = true,
                    metadata = CommandMetadata(
                        sources = results,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        commandType = "help",
                        matchCount = results.size
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in help command", e)
            CommandResult(
                command = command,
                content = "❌ Error: ${e.message}\n\nTry rephrasing your question or use /docs for direct documentation search.",
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "help",
                    matchCount = 0
                )
            )
        }
    }

    private fun buildDocumentationContext(
        results: List<com.example.chatagent.domain.model.DocumentSearchResult>
    ): String {
        val builder = StringBuilder()
        builder.append("=== DOCUMENTATION CONTEXT (from RAG search) ===\n\n")

        results.forEachIndexed { index, result ->
            val similarity = (result.similarity * 100).toInt()
            val fileName = result.document.fileName.removePrefix("PROJECT_DOC_")

            builder.append("--- Document ${index + 1}: $fileName (similarity: $similarity%) ---\n")
            builder.append("${result.chunk.text}\n\n")
        }

        builder.append("=== END OF DOCUMENTATION ===")
        return builder.toString()
    }

    private suspend fun callDeveloperAssistantAI(
        userQuery: String,
        documentationContext: String
    ): String {
        val userMessage = """
            Питання розробника: $userQuery

            $documentationContext

            Дай структуровану відповідь у форматі з емодзі:
            📌 Коротка відповідь
            📄 Джерело
            🧩 Фрагмент коду (якщо є)
            📏 Правило/Рекомендація (якщо релевантно)
        """.trimIndent()

        val request = ChatRequest(
            model = "claude-sonnet-4-5-20250929",
            system = DEVELOPER_ASSISTANT_SYSTEM_PROMPT,
            messages = listOf(
                MessageDto(role = "user", content = userMessage)
            ),
            maxTokens = 1024,
            temperature = 0.3 // Lower temperature for more factual responses
        )

        val response = chatApiService.sendMessage(request)
        return response.content.firstOrNull()?.text
            ?: "No response generated. Please try again."
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.Help
    }

    private fun buildNoResultsResponse(query: String): String {
        return """
            ❌ Документація не знайдена для: "$query"

            Можливі причини:
            • Документація ще не проіндексована
            • Запит занадто специфічний
            • Інформація відсутня в проекті

            💡 Спробуйте:
            • Перефразувати запит
            • Використати /docs для пошуку в .md файлах
            • Використати /code для пошуку в коді

            Доступні команди: /help, /code, /docs, /git
        """.trimIndent()
    }
}
