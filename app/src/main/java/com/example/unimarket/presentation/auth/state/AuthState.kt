package com.example.unimarket.presentation.auth.state

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
    data class PhoneNumberRequired(val flow: PhoneVerificationFlow) : AuthState()
    data class VerificationRequired(
        val phoneNumber: String,
        val flow: PhoneVerificationFlow
    ) : AuthState()
}

enum class PhoneVerificationFlow {
    SIGN_UP,
    GOOGLE_SETUP
}
