package com.example.unimarket.presentation.auth

import android.util.Patterns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.HasPhoneNumberUseCase
import com.example.unimarket.domain.usecase.auth.LoginUseCase
import com.example.unimarket.domain.usecase.auth.SavePhoneNumberUseCase
import com.example.unimarket.domain.usecase.auth.SendPhoneVerificationCodeUseCase
import com.example.unimarket.domain.usecase.auth.SignUpUseCase
import com.example.unimarket.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.LogoutUseCase
import com.example.unimarket.domain.usecase.auth.VerifyPhoneVerificationCodeUseCase
import com.example.unimarket.presentation.auth.state.AuthState
import com.example.unimarket.presentation.auth.state.PhoneVerificationFlow
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
    private val sendPhoneVerificationCodeUseCase: SendPhoneVerificationCodeUseCase,
    private val verifyPhoneVerificationCodeUseCase: VerifyPhoneVerificationCodeUseCase,
    private val hasPhoneNumberUseCase: HasPhoneNumberUseCase,
    private val savePhoneNumberUseCase: SavePhoneNumberUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private companion object {
        const val TAG = "AuthViewModel"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private var pendingSignUpPayload: PendingSignUpPayload? = null
    private var pendingVerificationContext: PendingVerificationContext? = null

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

    fun signUp(
        name: String,
        email: String,
        university: String,
        password: String,
        phoneNumber: String
    ) {
        if (name.isBlank() || email.isBlank() || university.isBlank() || password.isBlank() || phoneNumber.isBlank()) {
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

        val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedPhoneNumber == null) {
            _authState.value =
                AuthState.Error("Phone number must be in E.164 format, e.g. +84901234567")
            return
        }

        _authState.value = AuthState.Loading
        Log.d(
            TAG,
            "signUp start: email=${email.trim().maskEmailForLog()} phone=${normalizedPhoneNumber.maskPhoneForLog()} university=${university.trim()}"
        )
        viewModelScope.launch {
            sendPhoneVerificationCodeUseCase(normalizedPhoneNumber)
                .onSuccess {
                    pendingSignUpPayload = PendingSignUpPayload(
                        name = name.trim(),
                        email = email.trim(),
                        university = university.trim(),
                        password = password,
                        phoneNumber = normalizedPhoneNumber
                    )
                    pendingVerificationContext = PendingVerificationContext(
                        phoneNumber = normalizedPhoneNumber,
                        flow = PhoneVerificationFlow.SIGN_UP
                    )
                    _authState.value = AuthState.VerificationRequired(
                        phoneNumber = normalizedPhoneNumber,
                        flow = PhoneVerificationFlow.SIGN_UP
                    )
                    Log.d(TAG, "signUp otp sent -> move to verification")
                }
                .onFailure { error ->
                    Log.e(TAG, "signUp failed to send otp: ${error.message}", error)
                    _authState.value = AuthState.Error(error.message ?: "Failed to send verification code")
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _authState.value = AuthState.Error("Invalid Google ID Token")
            return
        }

        _authState.value = AuthState.Loading
        Log.d(TAG, "loginWithGoogle start")
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    Log.d(TAG, "loginWithGoogle success, checking phone")
                    hasPhoneNumberUseCase()
                        .onSuccess { hasPhoneNumber ->
                            if (hasPhoneNumber) {
                                Log.d(TAG, "loginWithGoogle phone exists -> success")
                                _authState.value = AuthState.Success()
                            } else {
                                Log.w(TAG, "loginWithGoogle missing phone -> phone setup required")
                                _authState.value = AuthState.PhoneNumberRequired(
                                    flow = PhoneVerificationFlow.GOOGLE_SETUP
                                )
                            }
                        }
                        .onFailure { error ->
                            Log.e(TAG, "loginWithGoogle failed to check phone: ${error.message}", error)
                            _authState.value = AuthState.Error(
                                error.message ?: "Failed to validate phone number"
                            )
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "loginWithGoogle failed: ${error.message}", error)
                    _authState.value = AuthState.Error(error.message ?: "Google Login failed")
                }
        }
    }

    fun requestGooglePhoneVerification(phoneNumber: String) {
        val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedPhoneNumber == null) {
            _authState.value =
                AuthState.Error("Phone number must be in E.164 format, e.g. +84901234567")
            return
        }

        _authState.value = AuthState.Loading
        Log.d(TAG, "requestGooglePhoneVerification for ${normalizedPhoneNumber.maskPhoneForLog()}")
        viewModelScope.launch {
            sendPhoneVerificationCodeUseCase(normalizedPhoneNumber)
                .onSuccess {
                    pendingVerificationContext = PendingVerificationContext(
                        phoneNumber = normalizedPhoneNumber,
                        flow = PhoneVerificationFlow.GOOGLE_SETUP
                    )
                    _authState.value = AuthState.VerificationRequired(
                        phoneNumber = normalizedPhoneNumber,
                        flow = PhoneVerificationFlow.GOOGLE_SETUP
                    )
                    Log.d(TAG, "requestGooglePhoneVerification otp sent")
                }
                .onFailure { error ->
                    Log.e(TAG, "requestGooglePhoneVerification failed: ${error.message}", error)
                    _authState.value =
                        AuthState.Error(error.message ?: "Failed to send verification code")
                }
        }
    }

    fun verifyPhoneCode(code: String) {
        val verificationContext = pendingVerificationContext
        if (verificationContext == null) {
            _authState.value = AuthState.Error("Verification session not found")
            return
        }
        val normalizedCode = code.trim()
        if (!normalizedCode.matches(Regex("^\\d{4}$"))) {
            _authState.value = AuthState.Error("Please enter a valid verification code")
            return
        }

        _authState.value = AuthState.Loading
        Log.d(
            TAG,
            "verifyPhoneCode start: flow=${verificationContext.flow}, phone=${verificationContext.phoneNumber.maskPhoneForLog()}, codeLength=${normalizedCode.length}"
        )
        viewModelScope.launch {
            verifyPhoneVerificationCodeUseCase(verificationContext.phoneNumber, normalizedCode)
                .onSuccess {
                    when (verificationContext.flow) {
                        PhoneVerificationFlow.SIGN_UP -> completeSignUpAfterPhoneVerified()
                        PhoneVerificationFlow.GOOGLE_SETUP -> completeGooglePhoneSetup(verificationContext.phoneNumber)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "verifyPhoneCode failed: ${error.message}", error)
                    _authState.value = AuthState.Error(error.message ?: "Verification failed")
                }
        }
    }

    fun resendPhoneCode() {
        val verificationContext = pendingVerificationContext
        if (verificationContext == null) {
            _authState.value = AuthState.Error("Verification session not found")
            return
        }

        _authState.value = AuthState.Loading
        Log.d(
            TAG,
            "resendPhoneCode start: flow=${verificationContext.flow}, phone=${verificationContext.phoneNumber.maskPhoneForLog()}"
        )
        viewModelScope.launch {
            sendPhoneVerificationCodeUseCase(verificationContext.phoneNumber)
                .onSuccess {
                    _authState.value = AuthState.VerificationRequired(
                        phoneNumber = verificationContext.phoneNumber,
                        flow = verificationContext.flow
                    )
                    Log.d(TAG, "resendPhoneCode success")
                }
                .onFailure { error ->
                    Log.e(TAG, "resendPhoneCode failed: ${error.message}", error)
                    _authState.value =
                        AuthState.Error(error.message ?: "Failed to resend verification code")
                }
        }
    }

    fun consumeNavigationState() {
        when (_authState.value) {
            is AuthState.PhoneNumberRequired, is AuthState.VerificationRequired -> {
                _authState.value = AuthState.Idle
            }
            else -> Unit
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

    private suspend fun completeSignUpAfterPhoneVerified() {
        val payload = pendingSignUpPayload
        if (payload == null) {
            _authState.value = AuthState.Error("Sign up data is missing")
            return
        }
        signUpUseCase(
            name = payload.name,
            email = payload.email,
            university = payload.university,
            password = payload.password,
            phoneNumber = payload.phoneNumber
        ).onSuccess {
            pendingSignUpPayload = null
            pendingVerificationContext = null
            Log.d(TAG, "completeSignUpAfterPhoneVerified success")
            _authState.value =
                AuthState.Success("Verification link sent. Please check your email before logging in.")
        }.onFailure { error ->
            Log.e(TAG, "completeSignUpAfterPhoneVerified failed: ${error.message}", error)
            _authState.value = AuthState.Error(error.message ?: "Sign up failed")
        }
    }

    private suspend fun completeGooglePhoneSetup(phoneNumber: String) {
        savePhoneNumberUseCase(phoneNumber)
            .onSuccess {
                pendingVerificationContext = null
                Log.d(TAG, "completeGooglePhoneSetup success: ${phoneNumber.maskPhoneForLog()}")
                _authState.value = AuthState.Success()
            }
            .onFailure { error ->
                Log.e(TAG, "completeGooglePhoneSetup failed: ${error.message}", error)
                _authState.value =
                    AuthState.Error(error.message ?: "Failed to save phone number")
            }
    }
}

private data class PendingSignUpPayload(
    val name: String,
    val email: String,
    val university: String,
    val password: String,
    val phoneNumber: String
)

private data class PendingVerificationContext(
    val phoneNumber: String,
    val flow: PhoneVerificationFlow
)

private fun normalizePhoneNumber(rawInput: String): String? {
    val compact = rawInput.trim()
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")

    if (compact.isBlank()) return null

    val candidate = when {
        compact.startsWith("+") -> compact
        compact.startsWith("00") -> "+${compact.drop(2)}"
        compact.all(Char::isDigit) -> {
            when {
                compact.startsWith("084") -> "+84${compact.drop(3)}"
                compact.startsWith("0") -> "+84${compact.drop(1)}"
                compact.startsWith("84") -> "+$compact"
                compact.length in 10..15 -> "+$compact"
                else -> return null
            }
        }

        else -> return null
    }

    val digits = candidate.drop(1)
    if (!candidate.startsWith("+") || !digits.matches(Regex("^\\d{8,15}$"))) {
        return null
    }

    return "+$digits"
}

private fun String.maskPhoneForLog(): String {
    if (length <= 6) return "***"
    return "${take(3)}***${takeLast(2)}"
}

private fun String.maskEmailForLog(): String {
    val at = indexOf('@')
    if (at <= 1) return "***"
    return "${take(2)}***${substring(at)}"
}

private fun Any?.isVerifiedSessionUser(): Boolean {
    val user = this as? FirebaseUser ?: return false
    val signedInWithPassword = user.providerData.any { provider ->
        provider.providerId == EmailAuthProvider.PROVIDER_ID
    }
    return !signedInWithPassword || user.isEmailVerified
}
