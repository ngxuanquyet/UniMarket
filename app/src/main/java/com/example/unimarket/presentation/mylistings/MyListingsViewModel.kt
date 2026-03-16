package com.example.unimarket.presentation.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.product.DeleteProductUseCase
import com.example.unimarket.data.local.DraftProduct
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.draft.DeleteDraftUseCase
import com.example.unimarket.domain.usecase.draft.GetDraftsUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val getDraftsUseCase: GetDraftsUseCase,
    private val deleteDraftUseCase: DeleteDraftUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListingsUiState(isLoading = true))
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var deleteJob: Job? = null
    private var pendingDeleteProduct: com.example.unimarket.domain.model.Product? = null

    init {
        loadMyListings()
    }

    fun refresh() {
        loadMyListings()
    }

    private fun loadMyListings() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        val currentUid = currentUser?.uid
        
        loadJob = viewModelScope.launch {
            try {
                val products = getAllProductsUseCase().first()
                val drafts = getDraftsUseCase(currentUid ?: "").first()
                val myProducts = products.filter { it.userId == currentUid }

                val mappedDrafts = drafts.map { draftItem ->
                    Product(
                        id = draftItem.id,
                        name = draftItem.name,
                        price = draftItem.price ?: 0.0,
                        description = draftItem.description,
                        imageUrls = draftItem.imageUrls,
                        categoryId = draftItem.categoryId,
                        condition = draftItem.condition,
                        sellerName = "Draft",
                        rating = 0.0,
                        location = "Draft",
                        timeAgo = "Drafted recently",
                        isFavorite = false,
                        isNegotiable = draftItem.isNegotiable,
                        userId = draftItem.userId,
                        specifications = draftItem.specifications,
                        deliveryMethodsAvailable = draftItem.deliveryMethodsAvailable
                    )
                }

                _uiState.value = _uiState.value.copy(
                    activeListings = myProducts,
                    soldListings = emptyList(),
                    draftListings = mappedDrafts,
                    isLoading = false,
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load listings"
                )
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
    }

    fun deleteListing(product: Product) {
        // Cancel any pending delete job just in case
        deleteJob?.cancel()

        val isDraft = _uiState.value.currentTab == 2

        // 1. Optimistic UI update: Remove from list temporarily
        pendingDeleteProduct = product
        if (isDraft) {
            val updatedListings = _uiState.value.draftListings.filter { it.id != product.id }
            _uiState.value = _uiState.value.copy(draftListings = updatedListings)
        } else {
            val updatedListings = _uiState.value.activeListings.filter { it.id != product.id }
            _uiState.value = _uiState.value.copy(activeListings = updatedListings)
        }

        // 2. Start a delayed job to actually delete it
        deleteJob = viewModelScope.launch {
            delay(4000) // Wait 4 seconds for undo
            
            // If the coroutine wasn't cancelled by undo, proceed with deletion
            if (isDraft) {
                try {
                    deleteDraftUseCase(product.id)
                } catch (e: Exception) {
                    undoDelete()
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete draft: ${e.message}"
                    )
                }
            } else {
                val result = deleteProductUseCase(product.id)
                result.onFailure { error ->
                    // If deletion fails, put it back and show error
                    undoDelete() 
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete listing: ${error.message}"
                    )
                }
            }
            pendingDeleteProduct = null
        }
    }

    fun undoDelete() {
        deleteJob?.cancel()
        pendingDeleteProduct?.let { product ->
            // Add it back to the list
            val isDraft = _uiState.value.currentTab == 2
            if (isDraft) {
                val updatedListings = _uiState.value.draftListings.toMutableList()
                updatedListings.add(product)
                _uiState.value = _uiState.value.copy(draftListings = updatedListings)
            } else {
                val updatedListings = _uiState.value.activeListings.toMutableList()
                updatedListings.add(product)
                _uiState.value = _uiState.value.copy(activeListings = updatedListings)
            }
        }
        pendingDeleteProduct = null
    }
}
