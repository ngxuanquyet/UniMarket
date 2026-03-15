package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.UserAddress

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(name: String, email: String, studentId: String, password: String): Result<Unit>
    fun logout()
    fun getCurrentUser(): Any? // Can be mapped to a domain User model later if needed

    suspend fun updateProfile(name: String?, photoUrl: String?): Result<Unit>
    suspend fun getUserAddresses(): Result<List<UserAddress>>
    suspend fun getAddressesByUserId(userId: String): Result<List<UserAddress>>
    suspend fun addUserAddress(address: UserAddress): Result<Unit>
    suspend fun updateUserAddress(address: UserAddress): Result<Unit>
    suspend fun deleteUserAddress(addressId: String): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>
}
