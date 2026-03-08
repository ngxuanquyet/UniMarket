package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String?, photoUrl: String?): Result<Unit> {
        return authRepository.updateProfile(name, photoUrl)
    }
}
