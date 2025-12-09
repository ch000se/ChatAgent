package com.example.chatagent.domain.model

data class BenchmarkResult(
    val modelInfo: ModelInfo,
    val prompt: String,
    val response: String,
    val responseTimeMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val estimatedCost: Double,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean get() = error == null

    val responseTimeSec: Double get() = responseTimeMs / 1000.0
}

data class BenchmarkComparison(
    val prompt: String,
    val results: List<BenchmarkResult>,
    val startTime: Long,
    val endTime: Long
) {
    val totalDurationMs: Long get() = endTime - startTime

    val fastestResult: BenchmarkResult?
        get() = results.filter { it.isSuccess }.minByOrNull { it.responseTimeMs }

    val cheapestResult: BenchmarkResult?
        get() = results.filter { it.isSuccess }.minByOrNull { it.estimatedCost }

    val successCount: Int
        get() = results.count { it.isSuccess }

    val totalCost: Double
        get() = results.filter { it.isSuccess }.sumOf { it.estimatedCost }
}