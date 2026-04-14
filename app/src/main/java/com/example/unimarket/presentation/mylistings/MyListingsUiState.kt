package com.example.unimarket.presentation.mylistings

import com.example.unimarket.domain.model.Product

data class MyListingsUiState(
    val activeListings: List<Product> = emptyList(),
    val soldListings: List<Product> = emptyList(),
    val pendingListings: List<Product> = emptyList(),
    val violationListings: List<Product> = emptyList(),
    val draftListings: List<Product> = emptyList(),
    val currentTab: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val displayedListings: List<Product>
        get() = when (currentTab) {
            0 -> activeListings
            1 -> soldListings
            2 -> pendingListings
            3 -> violationListings
            4 -> draftListings
            else -> emptyList()
        }
        
    val statItemsCount: Int
        get() = displayedListings.size

    val estimatedValue: Double
        get() = displayedListings.sumOf { it.price }
}
