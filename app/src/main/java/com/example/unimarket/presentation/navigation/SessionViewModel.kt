package com.example.unimarket.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.GetCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.ObserveCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    getCachedUserUseCase: GetCachedUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    observeCachedUserUseCase: ObserveCachedUserUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionUiState(
            isAuthenticated = getCachedUserUseCase() != null || getCurrentUserUseCase() != null
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        if (getCurrentUserUseCase() != null) {
            refreshProfileInBackground()
        }

        viewModelScope.launch {
            observeCachedUserUseCase().collect { cachedUser ->
                _uiState.value = SessionUiState(
                    isAuthenticated = cachedUser != null || getCurrentUserUseCase() != null
                )
            }
        }
    }

    private fun refreshProfileInBackground() {
        viewModelScope.launch {
            refreshCurrentUserProfileUseCase()
        }
    }
}
