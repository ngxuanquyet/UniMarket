package com.example.unimarket.domain.usecase.product

import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecommendedProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> {
        return productRepository.getRecommendedProducts()
    }
}
