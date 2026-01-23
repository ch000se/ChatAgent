package com.example.chatagent.data.remote.api

import com.example.chatagent.data.remote.dto.OllamaChatRequest
import com.example.chatagent.data.remote.dto.OllamaChatResponse
import com.example.chatagent.data.remote.dto.OllamaEmbeddingRequest
import com.example.chatagent.data.remote.dto.OllamaEmbeddingResponse
import com.example.chatagent.data.remote.dto.OllamaGenerateRequest
import com.example.chatagent.data.remote.dto.OllamaGenerateResponse
import com.example.chatagent.data.remote.dto.OllamaModelsResponse
import com.example.chatagent.data.remote.dto.OllamaPullRequest
import com.example.chatagent.data.remote.dto.OllamaPullResponse
import com.example.chatagent.data.remote.dto.OllamaShowRequest
import com.example.chatagent.data.remote.dto.OllamaShowResponse
import com.example.chatagent.data.remote.dto.OllamaVersionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OllamaApiService {

    @POST("api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequest
    ): Response<OllamaChatResponse>

    @POST("api/generate")
    suspend fun generate(
        @Body request: OllamaGenerateRequest
    ): Response<OllamaGenerateResponse>

    @GET("api/tags")
    suspend fun listModels(): Response<OllamaModelsResponse>

    @POST("api/show")
    suspend fun showModelInfo(
        @Body request: OllamaShowRequest
    ): Response<OllamaShowResponse>

    @POST("api/pull")
    suspend fun pullModel(
        @Body request: OllamaPullRequest
    ): Response<OllamaPullResponse>

    @POST("api/embeddings")
    suspend fun embeddings(
        @Body request: OllamaEmbeddingRequest
    ): Response<OllamaEmbeddingResponse>

    @GET("api/version")
    suspend fun getVersion(): Response<OllamaVersionResponse>
}
