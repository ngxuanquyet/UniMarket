package com.example.unimarket.domain.usecase.chat

import com.example.unimarket.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(currentUserId: String): Flow<List<com.example.unimarket.domain.model.Conversation>> {
        return chatRepository.observeConversations(currentUserId)
    }
}
