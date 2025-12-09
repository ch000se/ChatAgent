package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.BenchmarkComparison
import com.example.chatagent.domain.model.ModelInfo

interface BenchmarkRepository {
    suspend fun runBenchmark(
        models: List<ModelInfo>,
        prompt: String
    ): Result<BenchmarkComparison>
}