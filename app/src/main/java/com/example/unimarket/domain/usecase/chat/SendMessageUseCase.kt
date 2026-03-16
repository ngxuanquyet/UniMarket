package com.example.unimarket.domain.usecase.chat

import com.example.unimarket.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String, text: String): Result<Unit> {
        return chatRepository.sendMessage(conversationId, text)
    }
}
