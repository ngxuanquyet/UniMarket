package com.example.unimarket.presentation.productdetail

import com.example.unimarket.domain.model.Product

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
