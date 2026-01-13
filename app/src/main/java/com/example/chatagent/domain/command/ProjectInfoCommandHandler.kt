package com.example.chatagent.domain.command

import android.util.Log
import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandMetadata
import com.example.chatagent.domain.model.CommandResult
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import com.example.chatagent.domain.util.LocalGitExecutor
import javax.inject.Inject

class ProjectInfoCommandHandler @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val mcpClient: McpClient,
    private val localGitExecutor: LocalGitExecutor
) : CommandHandler<Command.ProjectInfo> {

    companion object {
        private const val TAG = "ProjectInfoHandler"
    }

    override suspend fun handle(command: Command.ProjectInfo): CommandResult {
        val startTime = System.currentTimeMillis()

        return try {
            Log.d(TAG, "Processing /project command")

            val projectInfo = buildProjectInfo()

            CommandResult(
                command = command,
                content = projectInfo,
                success = true,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "project",
                    matchCount = null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in project info command", e)
            CommandResult(
                command = command,
                content = "❌ Помилка отримання інформації про проект: ${e.message}",
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "project",
                    matchCount = 0
                )
            )
        }
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.ProjectInfo
    }

    private suspend fun buildProjectInfo(): String {
        val builder = StringBuilder()

        builder.append("# 📱 ChatAgent - AI Developer Assistant\n\n")

        // Get git info - try MCP first, then local git
        val connectionState = mcpClient.connectionState.value
        val useMcp = connectionState is McpClient.ConnectionState.Connected

        if (useMcp) {
            // Try MCP
            try {
                val branchResult = mcpClient.callTool(
                    toolName = "execute_command",
                    arguments = mapOf(
                        "command" to "git",
                        "args" to listOf("rev-parse", "--abbrev-ref", "HEAD")
                    )
                )

                val branch = branchResult.getOrNull()?.content?.firstOrNull()?.text?.trim() ?: "unknown"

                builder.append("## 🔀 Git Repository\n")
                builder.append("- **Поточна гілка:** `$branch`\n")
                builder.append("- **Git Mode:** MCP (підключено)\n\n")
            } catch (e: Exception) {
                builder.append("## 🔀 Git Repository\n")
                builder.append("- **MCP Status:** ⚠️ Помилка підключення\n\n")
            }
        } else {
            // Try local git as fallback
            try {
                val branchResult = localGitExecutor.getCurrentBranch()
                val branch = branchResult.getOrNull() ?: "unknown"

                builder.append("## 🔀 Git Repository\n")
                builder.append("- **Поточна гілка:** `$branch`\n")
                builder.append("- **Git Mode:** 💻 Local (без MCP)\n\n")
            } catch (e: Exception) {
                builder.append("## 🔀 Git Repository\n")
                builder.append("- **Git Status:** ❌ Недоступний\n\n")
            }
        }

        // Search for project documentation
        val architectureSearch = searchDocumentsUseCase(
            query = "архітектура проект структура clean architecture",
            topK = 3
        )

        val results = architectureSearch.getOrNull() ?: emptyList()

        if (results.isNotEmpty()) {
            builder.append("## 🏗️ Архітектура\n")
            builder.append("- **Паттерн:** Clean Architecture + MVVM\n")
            builder.append("- **UI Framework:** Jetpack Compose\n")
            builder.append("- **DI:** Hilt/Dagger\n")
            builder.append("- **Database:** Room + SQLite\n")
            builder.append("- **API:** Anthropic Claude API\n\n")
        }

        builder.append("## ✨ Основний функціонал\n")
        builder.append("- 🤖 AI чат-асистент на базі Claude\n")
        builder.append("- 📚 RAG система з TF-IDF векторизацією\n")
        builder.append("- 🔌 MCP інтеграція для git операцій\n")
        builder.append("- 💬 Командна система (/help, /code, /docs, /git)\n")
        builder.append("- 📊 Автоматична сумаризація історії\n")
        builder.append("- 📝 Tracking токенів та метрик\n\n")

        builder.append("## 📋 Доступні команди\n")
        builder.append("- `/help [запит]` - AI асистент з RAG пошуком\n")
        builder.append("- `/code [запит]` - Пошук в .kt файлах\n")
        builder.append("- `/docs [запит]` - Пошук в документації\n")
        builder.append("- `/git [status|log|diff|branch]` - Git операції\n")
        builder.append("- `/project` - Інформація про проект\n\n")

        builder.append("## 🎯 RAG System\n")
        builder.append("- **Векторизація:** TF-IDF (384 виміри)\n")
        builder.append("- **Chunk Size:** 500 символів\n")
        builder.append("- **Similarity:** Cosine similarity\n")
        builder.append("- **Reranking:** Enabled\n")
        builder.append("- **Auto-indexing:** При запуску додатку\n\n")

        // Get document count from search
        val allDocsSearch = searchDocumentsUseCase(
            query = "проект документація",
            topK = 20
        )
        val docCount = allDocsSearch.getOrNull()?.size ?: 0

        builder.append("## 📄 Документація\n")
        builder.append("- **Проіндексовано документів:** $docCount chunks\n")
        builder.append("- **Типи файлів:** .md (README, API, MCP guides)\n\n")

        builder.append("💡 **Підказка:** Використовуйте `/help [ваше питання]` для отримання детальної допомоги з RAG пошуком!\n")

        return builder.toString()
    }
}
