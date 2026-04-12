package com.example.unimarket.presentation.productdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.CartRepository
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.chat.CreateOrGetConversationUseCase
import com.example.unimarket.domain.usecase.image.GetUserAvatarUrl
import com.example.unimarket.presentation.util.localizedText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val cartRepository: CartRepository,
    private val createOrGetConversationUseCase: CreateOrGetConversationUseCase,
    private val getUserAvatarUrlUseCase: GetUserAvatarUrl,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
                        errorMessage = error.message ?: localizedText(
                            english = "Failed to load product",
                            vietnamese = "Không thể tải sản phẩm"
                        )
                    )
                }
                .collect { products ->
                    val product = products.find { it.id == productId }
                    if (product != null) {
                        val sellerProducts = products.filter { it.userId == product.userId }
                        _uiState.value = _uiState.value.copy(
                            product = product,
                            isLoading = false
                        )
                        loadSellerMetadata(
                            userId = product.userId,
                            sellerName = product.sellerName,
                            sellerProducts = sellerProducts
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = localizedText(
                                english = "Product not found",
                                vietnamese = "Không tìm thấy sản phẩm"
                            )
                        )
                    }
                }
        }
    }

    private fun loadSellerMetadata(
        userId: String,
        sellerName: String,
        sellerProducts: List<Product>
    ) {
        viewModelScope.launch {
            val fallbackAvatarUrl = buildAvatarFallbackUrl(sellerName)
            _uiState.value = _uiState.value.copy(sellerAvatarUrl = fallbackAvatarUrl)

            val userDoc = try {
                firestore.collection("users").document(userId).get().await()
            } catch (_: FirebaseFirestoreException) {
                null
            } catch (_: Exception) {
                null
            }

            val ratingSource = sellerProducts.map { it.rating }.filter { it > 0 }
            val storedRatingCount = userDoc?.getLong("ratingCount")?.toInt() ?: 0
            val storedAverageRating = userDoc?.getDouble("averageRating") ?: 0.0
            val sellerRatingCount = if (storedRatingCount > 0) storedRatingCount else ratingSource.size
            val sellerAverageRating = when {
                storedRatingCount > 0 -> storedAverageRating
                ratingSource.isNotEmpty() -> ratingSource.average()
                else -> 0.0
            }
            val sellerSoldCount = userDoc?.getLong("soldCount")?.toInt()
                ?: sellerProducts.count { it.quantityAvailable <= 0 }
            val isSellerVerifiedStudent = userDoc?.getString("studentId").isNullOrBlank().not()

            val avatarResult = getUserAvatarUrlUseCase(userId)
            val avatarUrl = avatarResult
                .getOrNull()
                .orEmpty()
                .ifBlank { fallbackAvatarUrl }

            _uiState.value = _uiState.value.copy(
                sellerAvatarUrl = avatarUrl,
                sellerAverageRating = sellerAverageRating,
                sellerRatingCount = sellerRatingCount,
                sellerSoldCount = sellerSoldCount,
                isSellerVerifiedStudent = isSellerVerifiedStudent
            )
            Log.d(
                "check_avatar",
                "id=$userId sellerName=$sellerName sellerAvatarUrl=$avatarUrl error=${avatarResult.exceptionOrNull()?.message}"
            )
        }
    }

    private fun buildAvatarFallbackUrl(name: String): String {
        val encodedName = URLEncoder.encode(
            name.ifBlank { "Student Seller" },
            StandardCharsets.UTF_8.toString()
        )
        return "https://ui-avatars.com/api/?name=$encodedName&background=random"
    }

    fun addToCart(product: Product, quantity: Int) {
        viewModelScope.launch {
            try {
                cartRepository.addToCart(product, quantity)
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        localizedText(
                            english = "Added $quantity item(s) to cart",
                            vietnamese = "Đã thêm $quantity sản phẩm vào giỏ hàng"
                        )
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        localizedText(
                            english = "Failed to add to cart: ${e.message}",
                            vietnamese = "Thêm vào giỏ thất bại: ${e.message}"
                        )
                    )
                )
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
                            error.message ?: localizedText(
                                english = "Failed to open conversation",
                                vietnamese = "Không thể mở cuộc trò chuyện"
                            )
                        )
                    )
                }
        }
    }

    fun submitProductReport(
        productId: String,
        sellerId: String,
        reasonCode: String,
        reasonLabel: String,
        details: String
    ) {
        val reporterId = firebaseAuth.currentUser?.uid.orEmpty()
        if (reporterId.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        localizedText(
                            english = "Please sign in before submitting a report",
                            vietnamese = "Vui lòng đăng nhập trước khi gửi báo cáo"
                        )
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val payload = hashMapOf<String, Any>(
                    "targetType" to "PRODUCT",
                    "targetId" to productId,
                    "productId" to productId,
                    "sellerId" to sellerId,
                    "reasonCode" to reasonCode,
                    "reasonLabel" to reasonLabel,
                    "description" to details,
                    "reporterId" to reporterId,
                    "status" to "OPEN",
                    "source" to "ANDROID_APP",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("reports").add(payload).await()
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        localizedText(
                            english = "Report submitted. We will review it soon.",
                            vietnamese = "Đã gửi báo cáo. Chúng tôi sẽ xem xét sớm."
                        )
                    )
                )
            } catch (_: Exception) {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        localizedText(
                            english = "Failed to submit report. Please try again.",
                            vietnamese = "Gửi báo cáo thất bại. Vui lòng thử lại."
                        )
                    )
                )
            }
        }
    }
}
