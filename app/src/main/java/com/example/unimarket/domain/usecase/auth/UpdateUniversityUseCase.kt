package com.example.unimarket.domain.usecase.auth

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateUniversityUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(university: String): Result<Unit> {
        return authRepository.updateUniversity(university)
    }
}
