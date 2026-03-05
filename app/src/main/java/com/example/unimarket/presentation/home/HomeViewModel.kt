package com.example.unimarket.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val recommendedProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        productRepository.getCategories(),
        productRepository.getRecommendedProducts()
    ) { categories, products ->
        HomeUiState(
            categories = categories,
            recommendedProducts = products,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
}
