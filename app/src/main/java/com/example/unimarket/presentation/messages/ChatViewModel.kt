package com.example.unimarket.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.chat.MarkConversationAsSeenUseCase
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val markConversationAsSeenUseCase: MarkConversationAsSeenUseCase,
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
    private var markSeenJob: Job? = null
    private var syncedMessages: List<com.example.unimarket.domain.model.ChatMessage> = emptyList()
    private var pendingMessages: List<com.example.unimarket.domain.model.ChatMessage> = emptyList()

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
        val text = _uiState.value.messageText.trim()
        if (text.isBlank()) return

        val user = currentUser ?: return
        val clientMessageId = UUID.randomUUID().toString()
        val optimisticMessage = com.example.unimarket.domain.model.ChatMessage(
            id = "local-$clientMessageId",
            conversationId = conversationId,
            senderId = user.uid,
            senderName = user.displayName ?: "You",
            senderAvatarUrl = user.photoUrl?.toString().orEmpty(),
            text = text,
            createdAt = System.currentTimeMillis(),
            clientMessageId = clientMessageId
        )

        pendingMessages = pendingMessages + optimisticMessage
        _uiState.update {
            it.copy(
                messageText = "",
                errorMessage = null,
                messages = mergeMessages(syncedMessages, pendingMessages)
            )
        }

        viewModelScope.launch {
            sendMessageUseCase(conversationId, text, clientMessageId)
                .onSuccess { }
                .onFailure { error ->
                    pendingMessages = pendingMessages.filterNot {
                        it.clientMessageId == clientMessageId
                    }
                    _uiState.update {
                        it.copy(
                            messages = mergeMessages(syncedMessages, pendingMessages),
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
                    if (conversation?.unreadCount ?: 0 > 0) {
                        markConversationAsSeen()
                    }
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
                    syncedMessages = messages
                    val deliveredClientIds = messages.mapNotNull { message ->
                        message.clientMessageId.takeIf { it.isNotBlank() }
                    }.toSet()
                    pendingMessages = pendingMessages.filterNot { pending ->
                        pending.clientMessageId.isNotBlank() && pending.clientMessageId in deliveredClientIds
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = mergeMessages(syncedMessages, pendingMessages),
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun mergeMessages(
        synced: List<com.example.unimarket.domain.model.ChatMessage>,
        pending: List<com.example.unimarket.domain.model.ChatMessage>
    ): List<com.example.unimarket.domain.model.ChatMessage> {
        return (synced + pending)
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
    }

    private fun markConversationAsSeen() {
        if (conversationId.isBlank() || markSeenJob?.isActive == true) return
        markSeenJob = viewModelScope.launch {
            markConversationAsSeenUseCase(conversationId)
        }
    }
}
