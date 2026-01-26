package com.example.chatagent.domain.usecase

import com.example.chatagent.domain.model.AnalystFile
import com.example.chatagent.domain.model.AnalystFileMetadata
import com.example.chatagent.domain.model.OllamaGenerationConfig
import javax.inject.Inject

class BuildAnalystPromptUseCase @Inject constructor() {

    operator fun invoke(file: AnalystFile, contextWindow: Int): AnalystPromptResult {
        val systemPrompt = buildString {
            appendLine("You are a data analyst assistant. The user has loaded a local file for analysis.")
            appendLine("Answer questions about the data accurately and concisely.")
            appendLine("When possible, provide specific numbers, counts, and patterns from the data.")
            appendLine("If the data appears truncated, mention that your analysis covers a subset.")
            appendLine()
            appendLine("=== FILE INFO ===")
            appendLine("File: ${file.fileName}")
            appendLine("Type: ${file.fileType.displayName}")
            appendLine("Size: ${formatSize(file.rawSize)}")
            if (file.wasTruncated) {
                appendLine("NOTE: File was truncated to fit context window. Analysis covers a subset of the data.")
            }
            appendLine()

            when (val meta = file.metadata) {
                is AnalystFileMetadata.CsvMetadata -> {
                    appendLine("=== CSV STRUCTURE ===")
                    appendLine("Headers: ${meta.headers.joinToString(", ")}")
                    appendLine("Total rows: ${meta.rowCount}")
                    if (meta.includedRows < meta.rowCount) {
                        appendLine("Included rows: ${meta.includedRows}")
                    }
                }
                is AnalystFileMetadata.JsonMetadata -> {
                    appendLine("=== JSON STRUCTURE ===")
                    appendLine("Type: ${meta.topLevelType}")
                    meta.elementCount?.let { appendLine("Elements: $it") }
                    meta.topLevelKeys?.let { appendLine("Keys: ${it.joinToString(", ")}") }
                }
                is AnalystFileMetadata.LogMetadata -> {
                    appendLine("=== LOG INFO ===")
                    appendLine("Total lines: ${meta.lineCount}")
                    if (meta.includedLines < meta.lineCount) {
                        appendLine("Included lines: ${meta.includedLines} (most recent)")
                    }
                }
            }

            appendLine()
            appendLine("=== FILE DATA ===")
            append(file.parsedContent)
        }

        val recommendedConfig = OllamaGenerationConfig(
            temperature = 0.1,
            maxTokens = 1024,
            contextWindow = contextWindow,
            topP = 0.85,
            topK = 20,
            repeatPenalty = 1.15
        )

        return AnalystPromptResult(
            systemPrompt = systemPrompt,
            recommendedConfig = recommendedConfig
        )
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }
}

data class AnalystPromptResult(
    val systemPrompt: String,
    val recommendedConfig: OllamaGenerationConfig
)
