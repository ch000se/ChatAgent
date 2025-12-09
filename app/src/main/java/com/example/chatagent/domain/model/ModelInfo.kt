package com.example.chatagent.domain.model

data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String, // "HuggingFace", "Anthropic", etc.
    val endpoint: String,
    val description: String,
    val pricing: ModelPricing? = null,
    val category: ModelCategory
)

data class ModelPricing(
    val inputCostPer1MTokens: Double,  // Вартість за 1M вхідних токенів
    val outputCostPer1MTokens: Double   // Вартість за 1M вихідних токенів
)

enum class ModelCategory {
    SMALL,    // Легкі моделі (початок списку)
    MEDIUM,   // Середні моделі
    LARGE     // Великі моделі (кінець списку)
}
object PredefinedModels {

    val SMALL = ModelInfo(
        id = "Llama-3.1-8B",
        name = "Llama 3.1 8B Instruct",
        provider = "HuggingFace",
        endpoint = "meta-llama/Llama-3.1-8B-Instruct",
        description = "Small chat-compatible model",
        category = ModelCategory.SMALL
    )

    val MEDIUM = ModelInfo(
        id = "Qwen2.5-7B",
        name = "Qwen2.5 7B Instruct",
        provider = "HuggingFace",
        endpoint = "Qwen/Qwen2.5-7B-Instruct",
        description = "Good mid-range chat model",
        category = ModelCategory.MEDIUM
    )

    val LARGE = ModelInfo(
        id = "Qwen2.5-72B",
        name = "Qwen2.5 72B Instruct",
        provider = "HuggingFace",
        endpoint = "Qwen/Qwen2.5-72B-Instruct",
        description = "Large powerful model",
        category = ModelCategory.LARGE
    )

    val allModels = listOf(SMALL, MEDIUM, LARGE)
}




