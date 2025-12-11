package com.example.chatagent.domain.model

data class SummarizationConfig(
    val enabled: Boolean = true,
    val triggerThreshold: Int = 10,
    val retentionCount: Int = 5
)
