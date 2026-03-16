package com.example.unimarket.domain.usecase.chat

import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ChatRepository
import javax.inject.Inject

class CreateOrGetConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(product: Product): Result<String> {
        return chatRepository.createOrGetConversation(product)
    }
}
