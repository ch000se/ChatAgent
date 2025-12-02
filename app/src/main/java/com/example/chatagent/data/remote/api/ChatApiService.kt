package com.example.chatagent.data.remote.api

import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ChatApiService {

    @Headers("anthropic-version: 2023-06-01")
    @POST("messages")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse
}