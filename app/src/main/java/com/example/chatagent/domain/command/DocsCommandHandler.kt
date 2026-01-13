package com.example.chatagent.domain.command

import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandMetadata
import com.example.chatagent.domain.model.CommandResult
import com.example.chatagent.domain.model.DocumentSearchResult
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import javax.inject.Inject

class DocsCommandHandler @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : CommandHandler<Command.Docs> {

    override suspend fun handle(command: Command.Docs): CommandResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Search only .md files
            val searchResult = searchDocumentsUseCase(
                query = command.query,
                topK = 3
            )

            val allResults = searchResult.getOrNull() ?: emptyList()

            // Filter to only show PROJECT_DOC_ prefixed documents
            val docResults = allResults.filter {
                it.document.fileName.startsWith("PROJECT_DOC_")
            }

            if (docResults.isEmpty()) {
                CommandResult(
                    command = command,
                    content = buildNoDocsResponse(command.query),
                    success = true,
                    metadata = CommandMetadata(
                        sources = null,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        commandType = "docs",
                        matchCount = 0
                    )
                )
            } else {
                CommandResult(
                    command = command,
                    content = buildDocsResponse(command.query, docResults),
                    success = true,
                    metadata = CommandMetadata(
                        sources = docResults,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        commandType = "docs",
                        matchCount = docResults.size
                    )
                )
            }
        } catch (e: Exception) {
            CommandResult(
                command = command,
                content = "Error searching docs: ${e.message}",
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "docs",
                    matchCount = 0
                )
            )
        }
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.Docs
    }

    private fun buildDocsResponse(query: String, results: List<DocumentSearchResult>): String {
        val builder = StringBuilder()
        builder.append("## Documentation: $query\n\n")

        results.forEach { result ->
            val fileName = result.document.fileName.removePrefix("PROJECT_DOC_")
            val similarity = (result.similarity * 100).toInt()

            builder.append("### $fileName ($similarity% match)\n")
            builder.append("${result.chunk.text}\n\n")
        }

        return builder.toString()
    }

    private fun buildNoDocsResponse(query: String): String {
        return """
            ## No documentation found for: "$query"

            Try:
            - /docs build instructions
            - /docs RAG quickstart
            - /help (searches all project docs)
        """.trimIndent()
    }
}
