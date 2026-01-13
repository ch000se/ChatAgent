package com.example.chatagent.domain.command

import android.content.Context
import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandMetadata
import com.example.chatagent.domain.model.CommandResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CodeSearchCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : CommandHandler<Command.Code> {

    override suspend fun handle(command: Command.Code): CommandResult {
        val startTime = System.currentTimeMillis()

        return try {
            // MVP: Simple search through hardcoded important files
            val matches = searchCodeFiles(command.query)

            if (matches.isEmpty()) {
                CommandResult(
                    command = command,
                    content = buildNoCodeFoundResponse(command.query),
                    success = true,
                    metadata = CommandMetadata(
                        sources = null,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        commandType = "code",
                        matchCount = 0
                    )
                )
            } else {
                CommandResult(
                    command = command,
                    content = buildCodeResultsResponse(command.query, matches),
                    success = true,
                    metadata = CommandMetadata(
                        sources = null,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        commandType = "code",
                        matchCount = matches.size
                    )
                )
            }
        } catch (e: Exception) {
            CommandResult(
                command = command,
                content = "Error searching code: ${e.message}",
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "code",
                    matchCount = 0
                )
            )
        }
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.Code
    }

    private fun searchCodeFiles(query: String): List<CodeMatch> {
        // MVP: Return hardcoded important files that match query
        return getImportantFiles().filter { codeFile ->
            codeFile.fileName.contains(query, ignoreCase = true) ||
            codeFile.description.contains(query, ignoreCase = true) ||
            codeFile.keyMethods.any { it.contains(query, ignoreCase = true) }
        }
    }

    private fun getImportantFiles(): List<CodeMatch> {
        return listOf(
            CodeMatch(
                fileName = "ChatRepository.kt",
                path = "data/repository/ChatRepositoryImpl.kt",
                description = "Main chat repository with MCP integration and message handling",
                keyMethods = listOf("sendMessage", "clearConversationHistory", "compressConversationHistory"),
                category = "Repository"
            ),
            CodeMatch(
                fileName = "ChatViewModel.kt",
                path = "presentation/chat/ChatViewModel.kt",
                description = "Chat screen view model, manages UI state and user interactions",
                keyMethods = listOf("sendMessage", "setSystemPrompt", "clearConversation", "toggleSummarization"),
                category = "ViewModel"
            ),
            CodeMatch(
                fileName = "McpClient.kt",
                path = "data/remote/client/McpClient.kt",
                description = "MCP client for tool execution (stdio-based JSON-RPC)",
                keyMethods = listOf("connect", "listTools", "callTool", "disconnect"),
                category = "MCP"
            ),
            CodeMatch(
                fileName = "DocumentRepository.kt",
                path = "data/repository/DocumentRepositoryImpl.kt",
                description = "RAG document repository with TF-IDF vectorization and indexing",
                keyMethods = listOf("addDocument", "indexDocument", "searchDocuments", "deleteDocument"),
                category = "Repository"
            ),
            CodeMatch(
                fileName = "RagChatViewModel.kt",
                path = "presentation/ragchat/RagChatViewModel.kt",
                description = "RAG chat with automatic document search and source citation",
                keyMethods = listOf("sendMessage", "searchDocuments", "buildRagContext"),
                category = "ViewModel"
            ),
            CodeMatch(
                fileName = "TfidfVectorizer.kt",
                path = "data/util/TfidfVectorizer.kt",
                description = "TF-IDF vectorization for document embeddings (384-dimensional)",
                keyMethods = listOf("fit", "transform", "cosineSimilarity"),
                category = "Util"
            ),
            CodeMatch(
                fileName = "ChatScreen.kt",
                path = "presentation/chat/ChatScreen.kt",
                description = "Main chat UI with Jetpack Compose",
                keyMethods = listOf("ChatScreen", "MessageBubble", "InputBar"),
                category = "UI"
            ),
            CodeMatch(
                fileName = "SendMessageUseCase.kt",
                path = "domain/usecase/SendMessageUseCase.kt",
                description = "Use case for sending messages through chat repository",
                keyMethods = listOf("invoke"),
                category = "UseCase"
            ),
            CodeMatch(
                fileName = "SearchDocumentsUseCase.kt",
                path = "domain/usecase/SearchDocumentsUseCase.kt",
                description = "Use case for searching documents with RAG",
                keyMethods = listOf("invoke"),
                category = "UseCase"
            ),
            CodeMatch(
                fileName = "IndexDocumentUseCase.kt",
                path = "domain/usecase/IndexDocumentUseCase.kt",
                description = "Use case for indexing documents for RAG search",
                keyMethods = listOf("invoke"),
                category = "UseCase"
            )
        )
    }

    private fun buildCodeResultsResponse(query: String, matches: List<CodeMatch>): String {
        val builder = StringBuilder()
        builder.append("## Code Search: $query\n\n")
        builder.append("Found ${matches.size} relevant files:\n\n")

        matches.forEach { match ->
            builder.append("### ${match.fileName}\n")
            builder.append("**Path**: `${match.path}`\n")
            builder.append("**Category**: ${match.category}\n")
            builder.append("**Description**: ${match.description}\n")
            builder.append("**Key Methods**: ${match.keyMethods.joinToString(", ")}\n\n")
        }

        return builder.toString()
    }

    private fun buildNoCodeFoundResponse(query: String): String {
        return """
            ## No code files found for: "$query"

            Try searching for:
            - ChatRepository, ChatViewModel
            - McpClient, DocumentRepository
            - RagChatViewModel, SendMessageUseCase
            - TfidfVectorizer

            Example: /code chat repository
        """.trimIndent()
    }

    data class CodeMatch(
        val fileName: String,
        val path: String,
        val description: String,
        val keyMethods: List<String>,
        val category: String
    )
}
