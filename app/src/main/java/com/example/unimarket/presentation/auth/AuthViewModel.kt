package com.example.unimarket.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.login(email, password)
                .onSuccess {
                    _authState.value = AuthState.Success
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun signUp(name: String, email: String, studentId: String, password: String) {
        if (name.isBlank() || email.isBlank() || studentId.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields must be filled")
            return
        }

        if (password.length < 8) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }

        // Basic domain validation example
        if (!email.endsWith(".edu") && !email.endsWith(".edu.vn")) {
            _authState.value = AuthState.Error("Only .edu or university domains are accepted")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.signUp(name, email, studentId, password)
                .onSuccess {
                    _authState.value = AuthState.Success
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Sign up failed")
                }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
