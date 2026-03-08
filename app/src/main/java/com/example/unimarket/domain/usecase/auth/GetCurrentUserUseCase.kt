package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Any? {
        return authRepository.getCurrentUser()
    }
}
