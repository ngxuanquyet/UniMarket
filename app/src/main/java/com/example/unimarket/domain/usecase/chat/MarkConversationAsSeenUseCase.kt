package com.example.unimarket.domain.usecase.chat

import com.example.unimarket.domain.repository.ChatRepository
import javax.inject.Inject

class MarkConversationAsSeenUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return chatRepository.markConversationAsSeen(conversationId)
    }
}
