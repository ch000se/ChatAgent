package com.example.chatagent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HFChatRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 300,
    val temperature: Double = 0.7
)

data class HFChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String? = null
)

data class Message(
    val role: String,
    val content: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)


