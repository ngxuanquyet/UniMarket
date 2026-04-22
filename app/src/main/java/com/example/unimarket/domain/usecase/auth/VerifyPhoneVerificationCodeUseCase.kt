package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyPhoneVerificationCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, code: String): Result<Unit> {
        return authRepository.verifyPhoneVerificationCode(phoneNumber, code)
    }
}
