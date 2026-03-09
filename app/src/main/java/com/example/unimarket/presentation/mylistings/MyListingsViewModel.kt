package com.example.unimarket.presentation.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.product.DeleteProductUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListingsUiState(isLoading = true))
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    private var deleteJob: Job? = null
    private var pendingDeleteProduct: com.example.unimarket.domain.model.Product? = null

    init {
        loadMyListings()
    }

    private fun loadMyListings() {
        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        val currentUid = currentUser?.uid
        
        viewModelScope.launch {
            getAllProductsUseCase()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load listings"
                    )
                }
                .collect { products ->
                    val myProducts = products.filter { it.userId == currentUid }
                    
                    _uiState.value = _uiState.value.copy(
                        activeListings = myProducts,
                        // Dummy data for other tabs to show UI capabilities
                        soldListings = emptyList(), 
                        draftListings = emptyList(),
                        isLoading = false
                    )
                }
        }
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
    }

    fun deleteListing(product: com.example.unimarket.domain.model.Product) {
        // Cancel any pending delete job just in case
        deleteJob?.cancel()

        // 1. Optimistic UI update: Remove from list temporarily
        pendingDeleteProduct = product
        val updatedListings = _uiState.value.activeListings.filter { it.id != product.id }
        _uiState.value = _uiState.value.copy(activeListings = updatedListings)

        // 2. Start a delayed job to actually delete it
        deleteJob = viewModelScope.launch {
            delay(4000) // Wait 4 seconds for undo
            
            // If the coroutine wasn't cancelled by undo, proceed with deletion
            val result = deleteProductUseCase(product.id)
            result.onFailure { error ->
                // If deletion fails, put it back and show error
                undoDelete() 
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete listing: ${error.message}"
                )
            }
            pendingDeleteProduct = null
        }
    }

    fun undoDelete() {
        deleteJob?.cancel()
        pendingDeleteProduct?.let { product ->
            // Add it back to the list
            val updatedListings = _uiState.value.activeListings.toMutableList()
            updatedListings.add(product)
            _uiState.value = _uiState.value.copy(activeListings = updatedListings)
        }
        pendingDeleteProduct = null
    }
}
