package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.UserProfile
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.UserAddress
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(
        name: String,
        email: String,
        studentId: String,
        password: String
    ): Result<Unit>

    suspend fun logout()
    fun getCurrentUser(): Any? // Can be mapped to a domain User model later if needed
    fun getCachedUser(): UserProfile?
    fun observeCachedUser(): Flow<UserProfile?>
    suspend fun refreshCurrentUserProfile(): Result<UserProfile>

    suspend fun updateProfile(name: String?, avatarUrl: String?): Result<Unit>
    suspend fun getUserAddresses(): Result<List<UserAddress>>
    suspend fun getUserPaymentMethods(): Result<List<SellerPaymentMethod>>
    suspend fun getUserAvatarUrl(id: String): Result<String>
    suspend fun getAddressesByUserId(userId: String): Result<List<UserAddress>>
    suspend fun getPaymentMethodsByUserId(userId: String): Result<List<SellerPaymentMethod>>
    suspend fun addUserAddress(address: UserAddress): Result<Unit>
    suspend fun updateUserAddress(address: UserAddress): Result<Unit>
    suspend fun deleteUserAddress(addressId: String): Result<Unit>
    suspend fun saveUserPaymentMethod(method: SellerPaymentMethod): Result<Unit>
    suspend fun deleteUserPaymentMethod(methodId: String): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>
}
