package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class SaveUserPaymentMethodUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(method: SellerPaymentMethod): Result<Unit> {
        return authRepository.saveUserPaymentMethod(method)
    }
}
