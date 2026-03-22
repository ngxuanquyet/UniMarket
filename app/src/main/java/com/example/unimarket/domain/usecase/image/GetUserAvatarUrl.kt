package com.example.unimarket.domain.usecase.image

import com.example.unimarket.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserAvatarUrl @Inject constructor(
    private val authRepository: AuthRepository
){
    suspend operator fun invoke(id: String): Result<String>{
        return authRepository.getUserAvatarUrl(id)
    }
}
