package com.example.chatagent.data.remote.api

import com.example.chatagent.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface yfMcpApiService {
    @POST
    @Headers(
        "Accept: application/json, text/event-stream",
        "Content-Type: application/json"
    )
    suspend fun sendRequest(
        @Url url: String,
        @Body request: JsonRpcRequest
    ): Response<JsonRpcResponse<Any>>

    @POST
    @Headers(
        "Accept: application/json, text/event-stream",
        "Content-Type: application/json"
    )
    suspend fun sendRequestWithSession(
        @Url url: String,
        @retrofit2.http.Header("mcp-session-id") sessionId: String,
        @Body request: JsonRpcRequest
    ): Response<JsonRpcResponse<Any>>
}
