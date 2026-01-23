package com.example.chatagent.domain.model

data class OllamaGenerationConfig(
    val temperature: Double = 0.8,
    val maxTokens: Int = 512,
    val contextWindow: Int = 2048,
    val topP: Double = 0.9,
    val topK: Int = 40,
    val repeatPenalty: Double = 1.1
) {
    companion object {
        val DEFAULT = OllamaGenerationConfig()
    }
}

enum class OllamaTaskType(val displayName: String) {
    CODE_GENERATION("Code Generation"),
    TEXT_SUMMARIZATION("Summarization"),
    QA_FACTUAL("Q&A"),
    TRANSLATION("Translation")
}

data class OllamaPromptTemplate(
    val name: String,
    val systemPrompt: String,
    val description: String,
    val taskType: OllamaTaskType,
    val recommendedConfig: OllamaGenerationConfig
) {
    companion object {
        val TEMPLATES = listOf(
            OllamaPromptTemplate(
                name = "Code Generation",
                systemPrompt = "You are a senior software engineer. Write clean, efficient code. Always include brief comments for complex logic. Respond only with code unless clarification is needed.",
                description = "Low temperature, high context for accurate code",
                taskType = OllamaTaskType.CODE_GENERATION,
                recommendedConfig = OllamaGenerationConfig(
                    temperature = 0.2,
                    maxTokens = 2048,
                    contextWindow = 4096,
                    topP = 0.85,
                    topK = 30,
                    repeatPenalty = 1.05
                )
            ),
            OllamaPromptTemplate(
                name = "Text Summarization",
                systemPrompt = "You are a precise summarizer. Extract key points and present them concisely. Use bullet points. Keep the summary under 30% of the original length.",
                description = "Low temperature, high repeat penalty for concise output",
                taskType = OllamaTaskType.TEXT_SUMMARIZATION,
                recommendedConfig = OllamaGenerationConfig(
                    temperature = 0.3,
                    maxTokens = 512,
                    contextWindow = 4096,
                    topP = 0.9,
                    topK = 40,
                    repeatPenalty = 1.2
                )
            ),
            OllamaPromptTemplate(
                name = "Q&A (Factual)",
                systemPrompt = "You are a knowledgeable assistant. Answer questions accurately and concisely. If unsure, say so. Cite reasoning when applicable.",
                description = "Minimal temperature for factual accuracy",
                taskType = OllamaTaskType.QA_FACTUAL,
                recommendedConfig = OllamaGenerationConfig(
                    temperature = 0.1,
                    maxTokens = 1024,
                    contextWindow = 2048,
                    topP = 0.8,
                    topK = 20,
                    repeatPenalty = 1.15
                )
            ),
            OllamaPromptTemplate(
                name = "Translation",
                systemPrompt = "You are a professional translator. Translate accurately preserving tone and meaning. If the source language is ambiguous, ask for clarification.",
                description = "Balanced settings for natural translation",
                taskType = OllamaTaskType.TRANSLATION,
                recommendedConfig = OllamaGenerationConfig(
                    temperature = 0.3,
                    maxTokens = 1024,
                    contextWindow = 2048,
                    topP = 0.9,
                    topK = 50,
                    repeatPenalty = 1.0
                )
            )
        )
    }
}

data class OllamaComparisonResult(
    val defaultResponse: String,
    val optimizedResponse: String,
    val defaultDurationMs: Long,
    val optimizedDurationMs: Long,
    val defaultTokensPerSec: Double,
    val optimizedTokensPerSec: Double,
    val defaultEvalCount: Int,
    val optimizedEvalCount: Int,
    val configUsed: OllamaGenerationConfig,
    val templateUsed: OllamaPromptTemplate?
)
