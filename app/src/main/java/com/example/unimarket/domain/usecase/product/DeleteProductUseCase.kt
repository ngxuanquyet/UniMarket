package com.example.unimarket.domain.usecase.product

import com.example.unimarket.domain.repository.ProductRepository
import javax.inject.Inject

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Result<Unit> {
        return productRepository.deleteProduct(productId)
    }
}
