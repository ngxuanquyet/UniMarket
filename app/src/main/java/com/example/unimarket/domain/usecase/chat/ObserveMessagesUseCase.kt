package com.example.unimarket.domain.usecase.chat

import com.example.unimarket.domain.model.ChatMessage
import com.example.unimarket.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessage>> {
        return chatRepository.observeMessages(conversationId)
    }
}
