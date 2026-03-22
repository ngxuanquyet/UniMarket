package com.example.unimarket.domain.model

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String = "",
    val text: String,
    val createdAt: Long,
    val clientMessageId: String = ""
)
