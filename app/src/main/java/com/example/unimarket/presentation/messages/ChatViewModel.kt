package com.example.unimarket.presentation.messages

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.chat.MarkConversationAsSeenUseCase
import com.example.unimarket.domain.usecase.chat.ObserveConversationsUseCase
import com.example.unimarket.domain.usecase.chat.ObserveMessagesUseCase
import com.example.unimarket.domain.usecase.chat.SendMessageUseCase
import com.example.unimarket.domain.usecase.image.UploadImageUseCase
import com.example.unimarket.presentation.util.localizedText
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val markConversationAsSeenUseCase: MarkConversationAsSeenUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val firestore: FirebaseFirestore
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
                errorMessage = localizedText(
                    english = "Conversation not found",
                    vietnamese = "Không tìm thấy cuộc trò chuyện"
                )
            )
        } else {
            observeConversation()
            observeMessages()
        }
    }

    fun updateMessageText(value: String) {
        _uiState.update { it.copy(messageText = value) }
    }

    fun updateSelectedImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, errorMessage = null) }
    }

    fun clearSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null) }
    }

    fun sendMessage() {
        if (conversationId.isBlank()) return
        val text = _uiState.value.messageText.trim()
        val selectedImageUri = _uiState.value.selectedImageUri
        if (text.isBlank() && selectedImageUri == null) return

        val user = currentUser ?: return
        val clientMessageId = UUID.randomUUID().toString()
        val optimisticMessage = com.example.unimarket.domain.model.ChatMessage(
            id = "local-$clientMessageId",
            conversationId = conversationId,
            senderId = user.uid,
            senderName = user.displayName ?: "You",
            senderAvatarUrl = user.photoUrl?.toString().orEmpty(),
            text = text,
            imageUrl = selectedImageUri?.toString().orEmpty(),
            createdAt = System.currentTimeMillis(),
            clientMessageId = clientMessageId
        )

        pendingMessages = pendingMessages + optimisticMessage
        _uiState.update {
            it.copy(
                messageText = "",
                selectedImageUri = null,
                isSending = true,
                errorMessage = null,
                messages = mergeMessages(syncedMessages, pendingMessages)
            )
        }

        viewModelScope.launch {
            val uploadedImageUrl = if (selectedImageUri != null) {
                uploadImageUseCase(selectedImageUri)
                    .getOrElse { error ->
                        pendingMessages = pendingMessages.filterNot {
                            it.clientMessageId == clientMessageId
                        }
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                messages = mergeMessages(syncedMessages, pendingMessages),
                                errorMessage = error.message ?: localizedText(
                                    english = "Failed to upload image",
                                    vietnamese = "Không thể tải ảnh lên"
                                )
                            )
                        }
                        _events.emit(
                            error.message ?: localizedText(
                                english = "Failed to upload image",
                                vietnamese = "Không thể tải ảnh lên"
                            )
                        )
                        return@launch
                    }
            } else {
                ""
            }

            sendMessageUseCase(conversationId, text, uploadedImageUrl, clientMessageId)
                .onSuccess {
                    _uiState.update { it.copy(isSending = false) }
                }
                .onFailure { error ->
                    pendingMessages = pendingMessages.filterNot {
                        it.clientMessageId == clientMessageId
                    }
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messages = mergeMessages(syncedMessages, pendingMessages),
                            errorMessage = error.message ?: localizedText(
                                english = "Failed to send message",
                                vietnamese = "Không thể gửi tin nhắn"
                            )
                        )
                    }
                    _events.emit(
                        error.message ?: localizedText(
                            english = "Failed to send message",
                            vietnamese = "Không thể gửi tin nhắn"
                        )
                    )
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
                            errorMessage = localizedText(
                                english = "Chat is unavailable. Please log in again.",
                                vietnamese = "Trò chuyện hiện không khả dụng. Vui lòng đăng nhập lại."
                            )
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
                            errorMessage = if (conversation == null) {
                                localizedText(
                                    english = "Conversation not found",
                                    vietnamese = "Không tìm thấy cuộc trò chuyện"
                                )
                            } else {
                                null
                            }
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
                            errorMessage = localizedText(
                                english = "Chat is unavailable. Please log in again.",
                                vietnamese = "Trò chuyện hiện không khả dụng. Vui lòng đăng nhập lại."
                            )
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
                            isSending = false,
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

    fun submitConversationReport(
        reasonCode: String,
        reasonLabel: String,
        details: String
    ) {
        val user = currentUser
        val conversation = _uiState.value.conversation
        if (user == null || conversation == null) {
            viewModelScope.launch {
                _events.emit(
                    localizedText(
                        english = "Unable to submit report right now",
                        vietnamese = "Hiện không thể gửi báo cáo"
                    )
                )
            }
            return
        }

        val payload = hashMapOf<String, Any>(
            "targetType" to "CONVERSATION",
            "targetId" to conversation.id,
            "conversationId" to conversation.id,
            "productId" to conversation.productId,
            "reportedUserId" to conversation.otherUser.id,
            "reasonCode" to reasonCode,
            "reasonLabel" to reasonLabel,
            "description" to details,
            "reporterId" to user.uid,
            "status" to "OPEN",
            "source" to "ANDROID_APP",
            "createdAt" to FieldValue.serverTimestamp()
        )
        submitReport(payload)
    }

    private fun submitReport(payload: Map<String, Any>) {
        viewModelScope.launch {
            try {
                firestore.collection("reports").add(payload).await()
                _events.emit(
                    localizedText(
                        english = "Report submitted. We will review it soon.",
                        vietnamese = "Đã gửi báo cáo. Chúng tôi sẽ xem xét sớm."
                    )
                )
            } catch (_: Exception) {
                _events.emit(
                    localizedText(
                        english = "Failed to submit report. Please try again.",
                        vietnamese = "Gửi báo cáo thất bại. Vui lòng thử lại."
                    )
                )
            }
        }
    }
}
