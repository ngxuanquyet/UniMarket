package com.example.unimarket.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

                _uiState.value = currentState.copy(
                    products = products,
                    categories = categories,
                    isLoading = false,
                    filteredProducts = filterProducts(
                        products,
                        currentState.searchQuery,
                        currentState.selectedCategory
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
        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredProducts = filterProducts(currentState.products, query, currentState.selectedCategory)
        )
    }

    fun updateSelectedCategory(categoryName: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedCategory = categoryName,
            filteredProducts = filterProducts(currentState.products, currentState.searchQuery, categoryName)
        )
    }

    private fun filterProducts(products: List<com.example.unimarket.domain.model.Product>, query: String, categoryName: String): List<com.example.unimarket.domain.model.Product> {
        return products.filter { product ->
            val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true)
            // Assuming categoryName maps to category id for simplicity in this dataset, or if 'All Items' don't filter
            val matchesCategory = categoryName == "All Items" || categoryName == "All" || product.categoryId.equals(categoryName, ignoreCase = true) 
            matchesQuery && matchesCategory
        }
    }
}
