package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.UserProfile
import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshCurrentUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<UserProfile> {
        return authRepository.refreshCurrentUserProfile()
    }
}
