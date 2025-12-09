package com.example.chatagent.data.remote.api

import com.example.chatagent.data.remote.dto.HFChatRequest
import com.example.chatagent.data.remote.dto.HFChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HuggingFaceApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: HFChatRequest
    ): Response<HFChatResponse>
}

