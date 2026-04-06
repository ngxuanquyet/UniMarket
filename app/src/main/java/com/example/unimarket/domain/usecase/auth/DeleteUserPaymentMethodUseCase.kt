package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class DeleteUserPaymentMethodUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(methodId: String): Result<Unit> {
        return authRepository.deleteUserPaymentMethod(methodId)
    }
}
