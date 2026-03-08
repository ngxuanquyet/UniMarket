package com.example.unimarket.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(name: String, email: String, studentId: String, password: String): Result<Unit>
    fun logout()
    fun getCurrentUser(): Any? // Can be mapped to a domain User model later if needed

    suspend fun updateProfile(name: String?, photoUrl: String?): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>
}
