package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmbeddingRequest(
    val inputs: List<String>,
    val options: EmbeddingOptions? = EmbeddingOptions()
)

data class EmbeddingOptions(
    @SerializedName("wait_for_model")
    val waitForModel: Boolean = true
)

data class EmbeddingResponse(
    val embeddings: List<List<Float>>? = null,
    val error: String? = null
)
