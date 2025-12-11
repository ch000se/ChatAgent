package com.example.chatagent.domain.model

data class SummarizationStats(
    val totalSummarizations: Int = 0,
    val tokensSaved: Int = 0,
    val compressionRatio: Double = 0.0
)
