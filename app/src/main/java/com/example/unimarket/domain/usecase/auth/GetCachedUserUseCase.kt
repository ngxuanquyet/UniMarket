package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.UserProfile
import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class GetCachedUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): UserProfile? {
        return authRepository.getCachedUser()
    }
}
