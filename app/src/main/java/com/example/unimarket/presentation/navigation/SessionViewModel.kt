package com.example.unimarket.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.GetCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.ObserveCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.chat.ObserveConversationsUseCase
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

data class SessionUiState(
    val isAuthenticated: Boolean = false,
    val unreadMessageCount: Int = 0
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    getCachedUserUseCase: GetCachedUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    observeCachedUserUseCase: ObserveCachedUserUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionUiState(
            isAuthenticated = getCachedUserUseCase() != null || getCurrentUserUseCase() != null
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
    private var unreadMessagesJob: Job? = null

    init {
        if (getCurrentUserUseCase() != null) {
            refreshProfileInBackground()
        }
        observeUnreadMessages()

        viewModelScope.launch {
            observeCachedUserUseCase().collect { cachedUser ->
                val isAuthenticated = cachedUser != null || getCurrentUserUseCase() != null
                _uiState.update { currentState ->
                    currentState.copy(
                        isAuthenticated = isAuthenticated,
                        unreadMessageCount = if (isAuthenticated) currentState.unreadMessageCount else 0
                    )
                }
                observeUnreadMessages()
            }
        }
    }

    private fun refreshProfileInBackground() {
        viewModelScope.launch {
            refreshCurrentUserProfileUseCase()
        }
    }
    private fun observeUnreadMessages() {
        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        if (currentUser == null) {
            unreadMessagesJob?.cancel()
            _uiState.update { it.copy(unreadMessageCount = 0) }
            return
        }

        unreadMessagesJob?.cancel()
        unreadMessagesJob = viewModelScope.launch {
            observeConversationsUseCase(currentUser.uid)
                .catch {
                    _uiState.update { it.copy(unreadMessageCount = 0) }
                }
                .collect { conversations ->
                    _uiState.update {
                        it.copy(
                            unreadMessageCount = conversations.sumOf { conversation -> conversation.unreadCount }
                        )
                    }
                }
        }
    }
}
