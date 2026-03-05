package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getCategories(): Flow<List<Category>>
    fun getRecommendedProducts(): Flow<List<Product>>
}
