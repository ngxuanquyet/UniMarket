package com.example.unimarket.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.chat.ObserveConversationsUseCase
import com.example.unimarket.domain.usecase.chat.ObserveMessagesUseCase
import com.example.unimarket.domain.usecase.chat.SendMessageUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    private val currentUser = getCurrentUserUseCase() as? FirebaseUser

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var conversationJob: Job? = null
    private var messagesJob: Job? = null

    init {
        if (conversationId.isBlank() || currentUser == null) {
            _uiState.value = ChatUiState(
                isLoading = false,
                errorMessage = "Conversation not found"
            )
        } else {
            observeConversation()
            observeMessages()
        }
    }

    fun updateMessageText(value: String) {
        _uiState.update { it.copy(messageText = value) }
    }

    fun sendMessage() {
        if (conversationId.isBlank()) return
        val text = _uiState.value.messageText
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            sendMessageUseCase(conversationId, text)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            messageText = "",
                            isSending = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: "Failed to send message"
                        )
                    }
                    _events.emit(error.message ?: "Failed to send message")
                }
        }
    }

    fun currentUserId(): String = currentUser?.uid.orEmpty()

    private fun observeConversation() {
        val userId = currentUser?.uid ?: return
        conversationJob?.cancel()
        conversationJob = viewModelScope.launch {
            observeConversationsUseCase(userId)
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversation = null,
                            messages = emptyList(),
                            errorMessage = "Chat is unavailable. Please log in again."
                        )
                    }
                }
                .collect { conversations ->
                    val conversation = conversations.firstOrNull { it.id == conversationId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversation = conversation,
                            errorMessage = if (conversation == null) "Conversation not found" else null
                        )
                    }
                }
        }
    }

    private fun observeMessages() {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            observeMessagesUseCase(conversationId)
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = emptyList(),
                            errorMessage = "Chat is unavailable. Please log in again."
                        )
                    }
                }
                .collect { messages ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
