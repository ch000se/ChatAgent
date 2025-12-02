package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.Message

interface ChatRepository {
    suspend fun sendMessage(message: String): Result<Message>
}