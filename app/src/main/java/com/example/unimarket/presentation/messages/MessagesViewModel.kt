package com.example.unimarket.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.chat.ObserveConversationsUseCase
import com.example.unimarket.presentation.util.localizedText
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeConversations()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isRefreshing = true,
                errorMessage = null
            )
        }
        observeConversations()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun observeConversations() {
        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        if (currentUser == null) {
            _uiState.value = MessagesUiState(
                isLoading = false,
                isRefreshing = false,
                errorMessage = localizedText(
                    english = "Please log in to see messages",
                    vietnamese = "Vui lòng đăng nhập để xem tin nhắn"
                )
            )
            return
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeConversationsUseCase(currentUser.uid)
                .catch {
                    _uiState.value = MessagesUiState(
                        isLoading = false,
                        isRefreshing = false,
                        conversations = emptyList(),
                        errorMessage = localizedText(
                            english = "Please log in to see messages",
                            vietnamese = "Vui lòng đăng nhập để xem tin nhắn"
                        )
                    )
                }
                .collect { conversations ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            conversations = conversations,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
