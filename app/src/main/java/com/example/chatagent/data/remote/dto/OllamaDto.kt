package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

// Request for chat completion
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerializedName("top_p")
    val topP: Double? = null,
    @SerializedName("top_k")
    val topK: Int? = null,
    @SerializedName("num_predict")
    val numPredict: Int? = null,
    val options: OllamaOptions? = null
)

data class OllamaMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

data class OllamaOptions(
    val temperature: Double? = null,
    @SerializedName("top_p")
    val topP: Double? = null,
    @SerializedName("top_k")
    val topK: Int? = null,
    @SerializedName("num_ctx")
    val numCtx: Int? = null,
    @SerializedName("num_predict")
    val numPredict: Int? = null
)

// Response from chat completion
data class OllamaChatResponse(
    val model: String,
    @SerializedName("created_at")
    val createdAt: String,
    val message: OllamaMessage,
    val done: Boolean,
    @SerializedName("total_duration")
    val totalDuration: Long? = null,
    @SerializedName("load_duration")
    val loadDuration: Long? = null,
    @SerializedName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerializedName("prompt_eval_duration")
    val promptEvalDuration: Long? = null,
    @SerializedName("eval_count")
    val evalCount: Int? = null,
    @SerializedName("eval_duration")
    val evalDuration: Long? = null
)

// Request for generate endpoint (alternative to chat)
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val system: String? = null,
    val template: String? = null,
    val context: List<Int>? = null,
    val options: OllamaOptions? = null
)

data class OllamaGenerateResponse(
    val model: String,
    @SerializedName("created_at")
    val createdAt: String,
    val response: String,
    val done: Boolean,
    val context: List<Int>? = null,
    @SerializedName("total_duration")
    val totalDuration: Long? = null,
    @SerializedName("load_duration")
    val loadDuration: Long? = null,
    @SerializedName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerializedName("eval_count")
    val evalCount: Int? = null,
    @SerializedName("eval_duration")
    val evalDuration: Long? = null
)

// Models list response
data class OllamaModelsResponse(
    val models: List<OllamaModelInfo>
)

data class OllamaModelInfo(
    val name: String,
    val model: String? = null,
    @SerializedName("modified_at")
    val modifiedAt: String,
    val size: Long,
    val digest: String? = null,
    val details: OllamaModelDetails? = null
)

data class OllamaModelDetails(
    @SerializedName("parent_model")
    val parentModel: String? = null,
    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,
    @SerializedName("parameter_size")
    val parameterSize: String? = null,
    @SerializedName("quantization_level")
    val quantizationLevel: String? = null
)

// Pull model request/response
data class OllamaPullRequest(
    val name: String,
    val insecure: Boolean = false,
    val stream: Boolean = false
)

data class OllamaPullResponse(
    val status: String,
    val digest: String? = null,
    val total: Long? = null,
    val completed: Long? = null
)

// Show model info
data class OllamaShowRequest(
    val name: String
)

data class OllamaShowResponse(
    val license: String? = null,
    val modelfile: String? = null,
    val parameters: String? = null,
    val template: String? = null,
    val details: OllamaModelDetails? = null
)

// Embeddings
data class OllamaEmbeddingRequest(
    val model: String,
    val prompt: String
)

data class OllamaEmbeddingResponse(
    val embedding: List<Double>
)

// Server status check
data class OllamaVersionResponse(
    val version: String
)
