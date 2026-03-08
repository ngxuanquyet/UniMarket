package com.example.unimarket.presentation.home

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val recommendedProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false
)
