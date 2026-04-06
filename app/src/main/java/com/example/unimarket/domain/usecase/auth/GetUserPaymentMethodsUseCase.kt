package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserPaymentMethodsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String? = null): Result<List<SellerPaymentMethod>> {
        return if (userId.isNullOrBlank()) {
            authRepository.getUserPaymentMethods()
        } else {
            authRepository.getPaymentMethodsByUserId(userId)
        }
    }
}
