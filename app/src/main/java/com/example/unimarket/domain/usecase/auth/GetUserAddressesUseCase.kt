package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserAddressesUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String? = null): Result<List<UserAddress>> {
        return if (userId.isNullOrBlank()) {
            authRepository.getUserAddresses()
        } else {
            authRepository.getAddressesByUserId(userId)
        }
    }
}
