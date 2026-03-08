package com.example.unimarket.presentation.explore

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product

data class ExploreUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String = "All Items",
    val filteredProducts: List<Product> = emptyList(),
    val errorMessage: String? = null
)
