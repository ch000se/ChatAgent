package com.example.chatagent.domain.usecase

import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(message: String): Result<Message> {
        return repository.sendMessage(message)
    }
}