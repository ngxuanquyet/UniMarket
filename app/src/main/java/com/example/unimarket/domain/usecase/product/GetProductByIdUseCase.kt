package com.example.unimarket.domain.usecase.product

import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductByIdUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Result<Product> {
        return productRepository.getProductById(productId)
    }
}
