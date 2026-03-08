package com.example.unimarket.domain.usecase.product

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return productRepository.getCategories()
    }
}
