package com.example.unimarket.domain.usecase.product

import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ProductRepository
import javax.inject.Inject

class AddProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product): Result<Unit> {
        return productRepository.addProduct(product)
    }
}
