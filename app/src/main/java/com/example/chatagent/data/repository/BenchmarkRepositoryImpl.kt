package com.example.chatagent.data.repository

import com.example.chatagent.data.remote.api.HuggingFaceApiService
import com.example.chatagent.data.remote.dto.HFChatRequest
import com.example.chatagent.data.remote.dto.Message
import com.example.chatagent.domain.model.BenchmarkComparison
import com.example.chatagent.domain.model.BenchmarkResult
import com.example.chatagent.domain.model.ModelInfo
import com.example.chatagent.domain.repository.BenchmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BenchmarkRepositoryImpl @Inject constructor(
    private val huggingFaceApi: HuggingFaceApiService
) : BenchmarkRepository {

    override suspend fun runBenchmark(
        models: List<ModelInfo>,
        prompt: String
    ): Result<BenchmarkComparison> = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()

            val results = models.map { model ->
                async { queryModelWithMetrics(model, prompt) }
            }.awaitAll()

            val end = System.currentTimeMillis()

            Result.success(
                BenchmarkComparison(
                    prompt = prompt,
                    results = results,
                    startTime = start,
                    endTime = end
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun queryModelWithMetrics(
        model: ModelInfo,
        prompt: String
    ): BenchmarkResult {
        return try {
            val start = System.currentTimeMillis()

            val request = HFChatRequest(
                model = model.endpoint,
                messages = listOf(
                    Message(role = "user", content = prompt)
                ),
                max_tokens = 500,
                temperature = 0.7
            )

            val response = huggingFaceApi.chatCompletion(request)
            val responseTime = System.currentTimeMillis() - start

            if (!response.isSuccessful || response.body() == null) {
                return BenchmarkResult(
                    modelInfo = model,
                    prompt = prompt,
                    response = "",
                    responseTimeMs = responseTime,
                    inputTokens = 0,
                    outputTokens = 0,
                    totalTokens = 0,
                    estimatedCost = 0.0,
                    error = "API Error: ${response.code()} ${response.message()}"
                )
            }

            val hfResponse = response.body()!!

            val generatedText = hfResponse.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: ""

            val inputTokens = estimateTokens(prompt)
            val outputTokens = estimateTokens(generatedText)
            val totalTokens = inputTokens + outputTokens

            val cost = calculateCost(model, inputTokens, outputTokens)

            BenchmarkResult(
                modelInfo = model,
                prompt = prompt,
                response = generatedText,
                responseTimeMs = responseTime,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                totalTokens = totalTokens,
                estimatedCost = cost
            )

        } catch (e: Exception) {
            BenchmarkResult(
                modelInfo = model,
                prompt = prompt,
                response = "",
                responseTimeMs = 0,
                inputTokens = 0,
                outputTokens = 0,
                totalTokens = 0,
                estimatedCost = 0.0,
                error = e.message ?: "Unknown error"
            )
        }
    }

    private fun estimateTokens(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }

    private fun calculateCost(model: ModelInfo, input: Int, output: Int): Double {
        val pricing = model.pricing ?: return 0.0
        val inCost = (input / 1_000_000.0) * pricing.inputCostPer1MTokens
        val outCost = (output / 1_000_000.0) * pricing.outputCostPer1MTokens
        return inCost + outCost
    }
}
