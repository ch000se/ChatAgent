package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: List<ContentDto>,

    @SerializedName("model")
    val model: String,

    @SerializedName("stop_reason")
    val stopReason: String? = null
)

data class ContentDto(
    @SerializedName("type")
    val type: String,

    @SerializedName("text")
    val text: String
)