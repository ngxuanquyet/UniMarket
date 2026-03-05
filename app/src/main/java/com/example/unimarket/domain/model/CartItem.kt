package com.example.unimarket.domain.model

data class CartItem(
    val id: String,
    val product: Product,
    val quantity: Int
)
