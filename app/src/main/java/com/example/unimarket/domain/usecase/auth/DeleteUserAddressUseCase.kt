package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class DeleteUserAddressUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(addressId: String): Result<Unit> {
        return authRepository.deleteUserAddress(addressId)
    }
}
