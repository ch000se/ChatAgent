package com.example.chatagent.presentation.benchmark

import com.example.chatagent.domain.model.BenchmarkComparison

data class BenchmarkUiState(
    val prompt: String = "Explain what is machine learning in simple terms.",
    val isRunning: Boolean = false,
    val comparison: BenchmarkComparison? = null,
    val error: String? = null
)