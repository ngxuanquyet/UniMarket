package com.example.unimarket.presentation.sellerprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.chat.CreateOrGetConversationUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SellerProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore,
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val createOrGetConversationUseCase: CreateOrGetConversationUseCase
) : ViewModel() {

    private val sellerId: String = savedStateHandle.get<String>("sellerId").orEmpty()
    private val initialProductId: String = savedStateHandle.get<String>("productId").orEmpty()

    private val _uiState = MutableStateFlow(SellerProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SellerProfileEvent>()
    val events: SharedFlow<SellerProfileEvent> = _events.asSharedFlow()

    init {
        loadSellerProfile()
    }

    fun startConversation() {
        val product = _uiState.value.selectedProductForChat ?: _uiState.value.activeListings.firstOrNull()
        if (product == null) {
            viewModelScope.launch {
                _events.emit(SellerProfileEvent.ShowMessage("Seller has no active listing to start a chat"))
            }
            return
        }

        viewModelScope.launch {
            createOrGetConversationUseCase(product)
                .onSuccess { conversationId ->
                    _events.emit(SellerProfileEvent.OpenConversation(conversationId))
                }
                .onFailure { error ->
                    _events.emit(
                        SellerProfileEvent.ShowMessage(
                            error.message ?: "Failed to open conversation"
                        )
                    )
                }
        }
    }

    private fun loadSellerProfile() {
        if (sellerId.isBlank()) {
            _uiState.value = SellerProfileUiState(
                isLoading = false,
                errorMessage = "Seller not found"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val allProducts = getAllProductsUseCase().first()
                val sellerProducts = allProducts.filter { it.userId == sellerId }
                val selectedProduct = sellerProducts.firstOrNull { it.id == initialProductId }
                    ?: sellerProducts.firstOrNull()

                val userDoc = try {
                    firestore.collection("users").document(sellerId).get().await()
                } catch (_: FirebaseFirestoreException) {
                    null
                } catch (_: Exception) {
                    null
                }

                val sellerName = userDoc?.getString("name").orEmpty()
                    .ifBlank { userDoc?.getString("displayName").orEmpty() }
                    .ifBlank { selectedProduct?.sellerName.orEmpty() }
                    .ifBlank { "Student Seller" }

                val avatarUrl = userDoc?.getString("avatarUrl").orEmpty()
                    .ifBlank { userDoc?.getString("photoUrl").orEmpty() }
                    .ifBlank { buildAvatarFallbackUrl(sellerName) }

                val ratingSource = sellerProducts.map { it.rating }.filter { it > 0 }
                val averageRating = if (ratingSource.isNotEmpty()) ratingSource.average() else 4.9
                val soldCount = sellerProducts.count { it.quantityAvailable <= 0 }

                _uiState.value = SellerProfileUiState(
                    isLoading = false,
                    sellerId = sellerId,
                    sellerName = sellerName,
                    avatarUrl = avatarUrl,
                    studentId = userDoc?.getString("studentId").orEmpty(),
                    isVerifiedStudent = userDoc?.getString("studentId").isNullOrBlank().not(),
                    averageRating = averageRating,
                    activeListings = sellerProducts,
                    soldCount = soldCount,
                    memberSinceLabel = buildMemberSinceLabel(userDoc?.getLong("createdAt")),
                    selectedProductForChat = selectedProduct
                )
            } catch (error: Exception) {
                _uiState.value = SellerProfileUiState(
                    isLoading = false,
                    sellerId = sellerId,
                    errorMessage = error.message ?: "Unable to load seller profile"
                )
            }
        }
    }

    private fun buildAvatarFallbackUrl(name: String): String {
        val encodedName = URLEncoder.encode(name.ifBlank { "User" }, StandardCharsets.UTF_8.toString())
        return "https://ui-avatars.com/api/?name=$encodedName&background=random"
    }

    private fun buildMemberSinceLabel(createdAtMillis: Long?): String {
        if (createdAtMillis == null || createdAtMillis <= 0) return "New"
        val calendar = Calendar.getInstance().apply { timeInMillis = createdAtMillis }
        return calendar.get(Calendar.YEAR).toString()
    }
}

sealed interface SellerProfileEvent {
    data class OpenConversation(val conversationId: String) : SellerProfileEvent
    data class ShowMessage(val message: String) : SellerProfileEvent
}
