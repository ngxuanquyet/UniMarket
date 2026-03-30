package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.ChatMessage
import com.example.unimarket.domain.model.Conversation
import com.example.unimarket.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(currentUserId: String): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun createOrGetConversation(product: Product): Result<String>
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        imageUrl: String = "",
        clientMessageId: String = ""
    ): Result<Unit>
    suspend fun markConversationAsSeen(conversationId: String): Result<Unit>
}
