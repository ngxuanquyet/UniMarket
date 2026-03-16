package com.example.unimarket.presentation.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.CartRepository
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.chat.CreateOrGetConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val cartRepository: CartRepository,
    private val createOrGetConversationUseCase: CreateOrGetConversationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState(isLoading = true))
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class OpenConversation(val conversationId: String) : UiEvent()
    }

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // For now, fetch all products and find the matching one.
            // Ideally, there should be a GetProductByIdUseCase.
            getAllProductsUseCase()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load product"
                    )
                }
                .collect { products ->
                    val product = products.find { it.id == productId }
                    if (product != null) {
                        _uiState.value = _uiState.value.copy(
                            product = product,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Product not found"
                        )
                    }
                }
        }
    }

    fun addToCart(product: Product, quantity: Int) {
        viewModelScope.launch {
            try {
                cartRepository.addToCart(product, quantity)
                _uiEvent.emit(UiEvent.ShowSnackbar("Added $quantity item(s) to cart"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Failed to add to cart: ${e.message}"))
            }
        }
    }

    fun startConversation(product: Product) {
        viewModelScope.launch {
            createOrGetConversationUseCase(product)
                .onSuccess { conversationId ->
                    _uiEvent.emit(UiEvent.OpenConversation(conversationId))
                }
                .onFailure { error ->
                    _uiEvent.emit(
                        UiEvent.ShowSnackbar(
                            error.message ?: "Failed to open conversation"
                        )
                    )
                }
        }
    }
}
