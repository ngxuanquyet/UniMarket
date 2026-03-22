package com.example.unimarket.domain.model

data class Conversation(
    val id: String,
    val productId: String,
    val productName: String,
    val productImageUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val otherUser: ChatUser,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Int = 0
)
