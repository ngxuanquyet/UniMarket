package com.example.unimarket.presentation.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.product.DeleteProductUseCase
import com.example.unimarket.data.local.DraftProduct
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.draft.DeleteDraftUseCase
import com.example.unimarket.domain.usecase.draft.GetDraftsUseCase
import com.example.unimarket.domain.usecase.order.GetSellerOrdersUseCase
import com.example.unimarket.presentation.util.localizedText
import com.example.unimarket.presentation.util.toRelativeTimeLabel
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
    private val deleteDraftUseCase: DeleteDraftUseCase,
    private val getSellerOrdersUseCase: GetSellerOrdersUseCase
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
                val activeProducts = myProducts.filter { it.quantityAvailable > 0 }
                val deliveredOrders = getSellerOrdersUseCase()
                    .getOrDefault(emptyList())
                    .filter { it.status == OrderStatus.DELIVERED }
                val soldProducts = resolveSoldProducts(myProducts, deliveredOrders)

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
                        postedAt = 0L,
                        isFavorite = false,
                        isNegotiable = draftItem.isNegotiable,
                        quantityAvailable = draftItem.quantityAvailable ?: 1,
                        userId = draftItem.userId,
                        specifications = draftItem.specifications,
                        deliveryMethodsAvailable = draftItem.deliveryMethodsAvailable
                    )
                }

                _uiState.value = _uiState.value.copy(
                    activeListings = activeProducts,
                    soldListings = soldProducts,
                    draftListings = mappedDrafts,
                    isLoading = false,
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to load listings",
                        vietnamese = "Không thể tải danh sách tin đăng"
                    )
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
        val isSold = _uiState.value.currentTab == 1

        // 1. Optimistic UI update: Remove from list temporarily
        pendingDeleteProduct = product
        if (isDraft) {
            val updatedListings = _uiState.value.draftListings.filter { it.id != product.id }
            _uiState.value = _uiState.value.copy(draftListings = updatedListings)
        } else if (isSold) {
            val updatedListings = _uiState.value.soldListings.filter { it.id != product.id }
            _uiState.value = _uiState.value.copy(soldListings = updatedListings)
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
                        errorMessage = localizedText(
                            english = "Failed to delete draft: ${e.message}",
                            vietnamese = "Xóa bản nháp thất bại: ${e.message}"
                        )
                    )
                }
            } else {
                val result = deleteProductUseCase(product.id)
                result.onFailure { error ->
                    // If deletion fails, put it back and show error
                    undoDelete() 
                    _uiState.value = _uiState.value.copy(
                        errorMessage = localizedText(
                            english = "Failed to delete listing: ${error.message}",
                            vietnamese = "Xóa tin đăng thất bại: ${error.message}"
                        )
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
            val isSold = _uiState.value.currentTab == 1
            if (isDraft) {
                val updatedListings = _uiState.value.draftListings.toMutableList()
                updatedListings.add(product)
                _uiState.value = _uiState.value.copy(draftListings = updatedListings)
            } else if (isSold) {
                val updatedListings = _uiState.value.soldListings.toMutableList()
                updatedListings.add(product)
                _uiState.value = _uiState.value.copy(soldListings = updatedListings)
            } else {
                val updatedListings = _uiState.value.activeListings.toMutableList()
                updatedListings.add(product)
                _uiState.value = _uiState.value.copy(activeListings = updatedListings)
            }
        }
        pendingDeleteProduct = null
    }

    private fun resolveSoldProducts(myProducts: List<Product>, deliveredOrders: List<Order>): List<Product> {
        if (deliveredOrders.isEmpty()) return emptyList()

        val listingById = myProducts.associateBy { it.id }

        // Keep one card per delivered seller order so Sold tab matches Seller Orders data.
        return deliveredOrders.map { order ->
            val sourceListing = listingById[order.productId]
            Product(
                id = order.productId.ifBlank { sourceListing?.id.orEmpty().ifBlank { order.id } },
                name = order.productName.ifBlank { sourceListing?.name.orEmpty().ifBlank { "Sold item" } },
                price = when {
                    order.totalAmount > 0.0 -> order.totalAmount
                    order.unitPrice > 0.0 -> order.unitPrice * order.quantity
                    else -> sourceListing?.price ?: 0.0
                },
                description = order.productDetail.ifBlank { sourceListing?.description.orEmpty() },
                imageUrls = listOfNotNull(
                    order.productImageUrl.takeIf { it.isNotBlank() }
                        ?: sourceListing?.imageUrls?.firstOrNull()
                ),
                categoryId = sourceListing?.categoryId.orEmpty(),
                condition = sourceListing?.condition.orEmpty(),
                sellerName = order.storeName.ifBlank { sourceListing?.sellerName.orEmpty() },
                rating = sourceListing?.rating ?: 0.0,
                location = sourceListing?.location.orEmpty(),
                timeAgo = order.updatedAt
                    .takeIf { it > 0L }
                    ?.toRelativeTimeLabel()
                    .orEmpty()
                    .ifBlank { order.createdAt.toRelativeTimeLabel() }
                    .ifBlank { "Delivered" },
                postedAt = order.updatedAt.takeIf { it > 0L } ?: order.createdAt,
                isFavorite = sourceListing?.isFavorite ?: false,
                isNegotiable = sourceListing?.isNegotiable ?: false,
                quantityAvailable = 0,
                userId = order.sellerId.ifBlank { sourceListing?.userId.orEmpty() },
                specifications = sourceListing?.specifications ?: emptyMap(),
                deliveryMethodsAvailable = sourceListing?.deliveryMethodsAvailable ?: emptyList(),
                sellerPickupAddress = sourceListing?.sellerPickupAddress
            )
        }
    }
}
