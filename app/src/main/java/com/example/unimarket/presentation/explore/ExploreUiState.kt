package com.example.unimarket.presentation.explore

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product

enum class ExplorePriceFilter(
    val label: String,
    private val minPrice: Double?,
    private val maxPrice: Double?
) {
    ALL("All Prices", null, null),
    UP_TO_200K("Up to 200K", null, 200_000.0),
    FROM_200K_TO_500K("200K - 500K", 200_000.0, 500_000.0),
    FROM_500K_TO_1M("500K - 1M", 500_000.0, 1_000_000.0),
    FROM_1M("1M+", 1_000_000.0, null);

    fun matches(price: Double): Boolean {
        val matchesMin = minPrice?.let { price >= it } ?: true
        val matchesMax = maxPrice?.let { price <= it } ?: true
        return matchesMin && matchesMax
    }
}

enum class ExplorePriceSort(val label: String) {
    RECOMMENDED("Recommended"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low")
}

data class ExploreUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String = "All Items",
    val selectedPriceFilter: ExplorePriceFilter = ExplorePriceFilter.ALL,
    val selectedPriceSort: ExplorePriceSort = ExplorePriceSort.RECOMMENDED,
    val matchedSellers: List<ExploreSellerPreview> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val errorMessage: String? = null
)

data class ExploreSellerPreview(
    val sellerId: String,
    val sellerName: String,
    val avatarUrl: String,
    val previewProducts: List<Product>,
    val totalListings: Int
)
