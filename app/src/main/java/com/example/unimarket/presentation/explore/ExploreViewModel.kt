package com.example.unimarket.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.product.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        loadJob = viewModelScope.launch {
            try {
                val products = getAllProductsUseCase().first()
                val categories = getCategoriesUseCase().first()
                val currentState = _uiState.value

                _uiState.value = updateFilteredProducts(
                    currentState.copy(
                        products = products,
                        categories = categories,
                        isLoading = false
                    )
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load explore data"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(searchQuery = query))
    }

    fun updateSelectedCategory(categoryName: String) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(selectedCategory = categoryName))
    }

    fun updateSelectedPriceFilter(priceFilter: ExplorePriceFilter) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(selectedPriceFilter = priceFilter))
    }

    fun updateSelectedPriceSort(priceSort: ExplorePriceSort) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(selectedPriceSort = priceSort))
    }

    private fun updateFilteredProducts(state: ExploreUiState): ExploreUiState {
        return state.copy(
            filteredProducts = filterProducts(
                products = state.products,
                query = state.searchQuery,
                categoryName = state.selectedCategory,
                categories = state.categories,
                priceFilter = state.selectedPriceFilter,
                priceSort = state.selectedPriceSort
            )
        )
    }

    private fun filterProducts(
        products: List<Product>,
        query: String,
        categoryName: String,
        categories: List<Category>,
        priceFilter: ExplorePriceFilter,
        priceSort: ExplorePriceSort
    ): List<Product> {
        val selectedCategory = categories.firstOrNull { category ->
            category.name.equals(categoryName, ignoreCase = true) ||
                category.id.equals(categoryName, ignoreCase = true)
        }

        val filteredProducts = products.filter { product ->
            val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true)
            val matchesCategory = isAllCategory(categoryName) ||
                product.categoryId.equals(categoryName, ignoreCase = true) ||
                selectedCategory?.let { category ->
                    product.categoryId.equals(category.id, ignoreCase = true) ||
                        product.categoryId.equals(category.name, ignoreCase = true)
                } == true
            val matchesPrice = priceFilter.matches(product.price)
            matchesQuery && matchesCategory && matchesPrice
        }

        return when (priceSort) {
            ExplorePriceSort.RECOMMENDED -> filteredProducts
            ExplorePriceSort.PRICE_LOW_TO_HIGH -> filteredProducts.sortedBy { it.price }
            ExplorePriceSort.PRICE_HIGH_TO_LOW -> filteredProducts.sortedByDescending { it.price }
        }
    }

    private fun isAllCategory(categoryName: String): Boolean {
        return categoryName.equals("All Items", ignoreCase = true) ||
            categoryName.equals("All", ignoreCase = true)
    }
}
