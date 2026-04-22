package com.example.unimarket.presentation.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.BuildConfig
import com.example.unimarket.data.notification.FcmTokenManager
import com.example.unimarket.domain.usecase.auth.GetCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.LogoutUseCase
import com.example.unimarket.domain.usecase.auth.ObserveCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.auth.UpdateUniversityUseCase
import com.example.unimarket.domain.usecase.chat.ObserveConversationsUseCase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SessionUiState(
    val isAuthenticated: Boolean = false,
    val unreadMessageCount: Int = 0,
    val isAccountLocked: Boolean = false,
    val isUniversityRequired: Boolean = false,
    val isPhoneRequired: Boolean = false,
    val isUpdatingUniversity: Boolean = false
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    getCachedUserUseCase: GetCachedUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    observeCachedUserUseCase: ObserveCachedUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val updateUniversityUseCase: UpdateUniversityUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionUiState(
            isAuthenticated = (getCachedUserUseCase()?.isLock != true) &&
                (getCachedUserUseCase() != null || getCurrentUserUseCase().isVerifiedSessionUser()),
            isPhoneRequired = isPhoneSetupRequired(
                cachedUser = getCachedUserUseCase(),
                currentUser = getCurrentUserUseCase() as? FirebaseUser
            )
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
    private var unreadMessagesJob: Job? = null

    init {
        if (getCurrentUserUseCase().isVerifiedSessionUser()) {
            refreshProfileInBackground()
            syncFcmTokenInBackground()
            logIdTokenInBackground()
        }
        observeUnreadMessages()

        viewModelScope.launch {
            observeCachedUserUseCase().collect { cachedUser ->
                val isLocked = cachedUser?.isLock == true
                val isAuthenticated =
                    !isLocked && (cachedUser != null || getCurrentUserUseCase().isVerifiedSessionUser())
                val isPhoneRequired = isPhoneSetupRequired(
                    cachedUser = cachedUser,
                    currentUser = getCurrentUserUseCase() as? FirebaseUser
                )
                _uiState.update { currentState ->
                    currentState.copy(
                        isAuthenticated = isAuthenticated,
                        unreadMessageCount = if (isAuthenticated) currentState.unreadMessageCount else 0,
                        isAccountLocked = currentState.isAccountLocked || isLocked,
                        isUniversityRequired = isAuthenticated && cachedUser != null && cachedUser.university.isBlank(),
                        isPhoneRequired = isAuthenticated && isPhoneRequired
                    )
                }
                if (isLocked) {
                    forceLogoutForLockedAccount()
                }
                if (isAuthenticated) {
                    syncFcmTokenInBackground()
                    logIdTokenInBackground()
                }
                observeUnreadMessages()
            }
        }
    }

    fun consumeAccountLockedNotice() {
        _uiState.update { it.copy(isAccountLocked = false) }
    }

    fun updateUniversity(university: String) {
        val trimmedUniversity = university.trim()
        if (trimmedUniversity.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingUniversity = true) }
            updateUniversityUseCase(trimmedUniversity)
                .onSuccess {
                    refreshCurrentUserProfileUseCase()
                }
            _uiState.update { it.copy(isUpdatingUniversity = false) }
        }
    }

    private fun forceLogoutForLockedAccount() {
        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (_: Exception) {
            }
            _uiState.update { it.copy(isAuthenticated = false, unreadMessageCount = 0) }
        }
    }

    private fun refreshProfileInBackground() {
        viewModelScope.launch {
            refreshCurrentUserProfileUseCase()
        }
    }

    private fun syncFcmTokenInBackground() {
        viewModelScope.launch {
            runCatching { fcmTokenManager.syncCurrentUserToken() }
        }
    }

    private fun logIdTokenInBackground() {
        if (!BuildConfig.DEBUG) return

        val currentUser = getCurrentUserUseCase() as? FirebaseUser ?: return
        viewModelScope.launch {
            runCatching {
                val token = currentUser.getIdToken(false).await().token.orEmpty()
                if (token.isNotBlank()) {
                    Log.d(TAG, "firebase_id_token=${token}")
                }
            }.onFailure { error ->
                Log.w(TAG, "Failed to log Firebase ID token", error)
            }
        }
    }

    private fun observeUnreadMessages() {
        val currentUser = (getCurrentUserUseCase() as? FirebaseUser)?.takeIf { it.isVerifiedSessionUser() }
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

    private companion object {
        const val TAG = "SessionViewModel"
    }
}

private fun isPhoneSetupRequired(cachedUser: com.example.unimarket.domain.model.UserProfile?, currentUser: FirebaseUser?): Boolean {
    if (currentUser == null || !currentUser.isVerifiedSessionUser()) return false
    val isGoogleLogin = currentUser.providerData.any { provider -> provider.providerId == "google.com" }
    if (!isGoogleLogin) return false
    if (!currentUser.phoneNumber.isNullOrBlank()) return false
    return cachedUser?.phoneNumber.isNullOrBlank()
}

private fun Any?.isVerifiedSessionUser(): Boolean {
    val user = this as? FirebaseUser ?: return false
    return user.isVerifiedSessionUser()
}

private fun FirebaseUser.isVerifiedSessionUser(): Boolean {
    val signedInWithPassword = providerData.any { provider ->
        provider.providerId == EmailAuthProvider.PROVIDER_ID
    }
    return !signedInWithPassword || isEmailVerified
}
