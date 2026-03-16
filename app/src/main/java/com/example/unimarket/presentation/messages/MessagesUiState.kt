package com.example.unimarket.presentation.messages

import com.example.unimarket.domain.model.Conversation

data class MessagesUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
) {
    val filteredConversations: List<Conversation>
        get() = if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conversation ->
                conversation.otherUser.name.contains(searchQuery, ignoreCase = true) ||
                    conversation.productName.contains(searchQuery, ignoreCase = true) ||
                    conversation.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
}
