package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getCategories(): Flow<List<Category>>
    fun getRecommendedProducts(): Flow<List<Product>>
    suspend fun addProduct(product: Product): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
}
