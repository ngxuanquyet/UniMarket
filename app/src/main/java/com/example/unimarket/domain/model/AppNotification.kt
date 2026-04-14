package com.example.unimarket.domain.model

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val isRead: Boolean
)
