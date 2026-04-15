package com.example.unimarket.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.LoginUseCase
import com.example.unimarket.domain.usecase.auth.SignUpUseCase
import com.example.unimarket.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.LogoutUseCase
import com.example.unimarket.presentation.auth.state.AuthState
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        if (getCurrentUserUseCase().isVerifiedSessionUser()) {
            _authState.value = AuthState.Success()
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            loginUseCase(email, password)
                .onSuccess {
                    _authState.value = AuthState.Success()
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun signUp(name: String, email: String, university: String, password: String) {
        if (name.isBlank() || email.isBlank() || university.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields must be filled")
            return
        }

        if (password.length < 8) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            _authState.value = AuthState.Error("Invalid email format")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            signUpUseCase(name, email, university.trim(), password)
                .onSuccess {
                    _authState.value = AuthState.Success("Verification link sent. Please check your email before logging in.")
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Sign up failed")
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _authState.value = AuthState.Error("Invalid Google ID Token")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _authState.value = AuthState.Success()
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Google Login failed")
                }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _authState.value = AuthState.Idle
        }
    }
}

private fun Any?.isVerifiedSessionUser(): Boolean {
    val user = this as? FirebaseUser ?: return false
    val signedInWithPassword = user.providerData.any { provider ->
        provider.providerId == EmailAuthProvider.PROVIDER_ID
    }
    return !signedInWithPassword || user.isEmailVerified
}
