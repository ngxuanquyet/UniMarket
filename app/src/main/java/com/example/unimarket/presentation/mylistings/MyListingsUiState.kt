package com.example.unimarket.presentation.mylistings

import com.example.unimarket.domain.model.Product

data class MyListingsUiState(
    val activeListings: List<Product> = emptyList(),
    val soldListings: List<Product> = emptyList(),
    val draftListings: List<Product> = emptyList(),
    val currentTab: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val displayedListings: List<Product>
        get() = when (currentTab) {
            0 -> activeListings
            1 -> soldListings
            2 -> draftListings
            else -> emptyList()
        }
        
    val liveItemsCount: Int
        get() = activeListings.size

    val estimatedValue: Double
        get() = activeListings.sumOf { it.price }
}
