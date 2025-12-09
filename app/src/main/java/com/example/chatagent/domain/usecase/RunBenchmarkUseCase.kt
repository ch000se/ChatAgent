package com.example.chatagent.domain.usecase

import com.example.chatagent.domain.model.BenchmarkComparison
import com.example.chatagent.domain.model.ModelInfo
import com.example.chatagent.domain.repository.BenchmarkRepository
import javax.inject.Inject

class RunBenchmarkUseCase @Inject constructor(
    private val repository: BenchmarkRepository
) {
    suspend operator fun invoke(
        models: List<ModelInfo>,
        prompt: String
    ): Result<BenchmarkComparison> {
        require(models.isNotEmpty()) { "Models list cannot be empty" }
        require(prompt.isNotBlank()) { "Prompt cannot be blank" }

        return repository.runBenchmark(models, prompt)
    }
}