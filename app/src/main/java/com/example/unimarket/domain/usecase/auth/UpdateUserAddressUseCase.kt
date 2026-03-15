package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateUserAddressUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(address: UserAddress): Result<Unit> {
        return authRepository.updateUserAddress(address)
    }
}
