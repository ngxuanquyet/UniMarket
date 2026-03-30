package com.example.unimarket.presentation.messages

import android.net.Uri
import com.example.unimarket.domain.model.ChatMessage
import com.example.unimarket.domain.model.Conversation

data class ChatUiState(
    val isLoading: Boolean = true,
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val selectedImageUri: Uri? = null,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
