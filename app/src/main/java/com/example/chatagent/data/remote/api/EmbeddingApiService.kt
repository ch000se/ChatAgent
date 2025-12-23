package com.example.chatagent.data.remote.api

import com.example.chatagent.data.remote.dto.EmbeddingRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface EmbeddingApiService {

    @POST(".")
    suspend fun generateEmbeddings(
        @Body request: EmbeddingRequest
    ): Response<List<List<Float>>>
}
