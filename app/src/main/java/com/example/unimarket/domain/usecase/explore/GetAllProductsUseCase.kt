package com.example.unimarket.domain.usecase.explore

import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> {
        return productRepository.getRecommendedProducts()
    }
}
