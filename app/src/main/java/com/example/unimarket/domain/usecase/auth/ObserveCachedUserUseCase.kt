package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.model.UserProfile
import com.example.unimarket.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCachedUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserProfile?> {
        return authRepository.observeCachedUser()
    }
}
