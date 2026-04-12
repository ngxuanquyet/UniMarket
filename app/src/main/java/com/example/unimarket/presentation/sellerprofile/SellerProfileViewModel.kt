package com.example.unimarket.presentation.sellerprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.chat.CreateOrGetConversationUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.presentation.util.localizedText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
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
    private val firebaseAuth: FirebaseAuth,
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
                _events.emit(
                    SellerProfileEvent.ShowMessage(
                        localizedText(
                            english = "Seller has no active listing to start a chat",
                            vietnamese = "Người bán chưa có tin đang bán để bắt đầu trò chuyện"
                        )
                    )
                )
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
                            error.message ?: localizedText(
                                english = "Failed to open conversation",
                                vietnamese = "Không thể mở cuộc trò chuyện"
                            )
                        )
                    )
                }
        }
    }

    private fun loadSellerProfile() {
        if (sellerId.isBlank()) {
            _uiState.value = SellerProfileUiState(
                isLoading = false,
                errorMessage = localizedText(
                    english = "Seller not found",
                    vietnamese = "Không tìm thấy người bán"
                )
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
                    .ifBlank { "" }

                val avatarUrl = userDoc?.getString("avatarUrl").orEmpty()
                    .ifBlank { buildAvatarFallbackUrl(sellerName) }

                val ratingSource = sellerProducts.map { it.rating }.filter { it > 0 }
                val storedRatingCount = userDoc?.getLong("ratingCount")?.toInt() ?: 0
                val storedAverageRating = userDoc?.getDouble("averageRating") ?: 0.0
                val ratingCount = if (storedRatingCount > 0) storedRatingCount else ratingSource.size
                val averageRating = when {
                    storedRatingCount > 0 -> storedAverageRating
                    ratingSource.isNotEmpty() -> ratingSource.average()
                    else -> 0.0
                }
                val soldCount = userDoc?.getLong("soldCount")?.toInt()
                    ?: sellerProducts.count { it.quantityAvailable <= 0 }

                _uiState.value = SellerProfileUiState(
                    isLoading = false,
                    sellerId = sellerId,
                    sellerName = sellerName,
                    avatarUrl = avatarUrl,
                    studentId = userDoc?.getString("studentId").orEmpty(),
                    isVerifiedStudent = userDoc?.getString("studentId").isNullOrBlank().not(),
                    averageRating = averageRating,
                    ratingCount = ratingCount,
                    activeListings = sellerProducts,
                    soldCount = soldCount,
                    memberSinceLabel = buildMemberSinceLabel(userDoc?.getLong("createdAt")),
                    selectedProductForChat = selectedProduct
                )
            } catch (error: Exception) {
                _uiState.value = SellerProfileUiState(
                    isLoading = false,
                    sellerId = sellerId,
                    errorMessage = error.message ?: localizedText(
                        english = "Unable to load seller profile",
                        vietnamese = "Không thể tải hồ sơ người bán"
                    )
                )
            }
        }
    }

    private fun buildAvatarFallbackUrl(name: String): String {
        val encodedName = URLEncoder.encode(name.ifBlank { "User" }, StandardCharsets.UTF_8.toString())
        return "https://ui-avatars.com/api/?name=$encodedName&background=random"
    }

    private fun buildMemberSinceLabel(createdAtMillis: Long?): String {
        if (createdAtMillis == null || createdAtMillis <= 0) return ""
        val calendar = Calendar.getInstance().apply { timeInMillis = createdAtMillis }
        return calendar.get(Calendar.YEAR).toString()
    }

    fun submitSellerReport(
        reasonCode: String,
        reasonLabel: String,
        details: String
    ) {
        val reporterId = firebaseAuth.currentUser?.uid.orEmpty()
        if (reporterId.isBlank()) {
            viewModelScope.launch {
                _events.emit(
                    SellerProfileEvent.ShowMessage(
                        localizedText(
                            english = "Please sign in before submitting a report",
                            vietnamese = "Vui lòng đăng nhập trước khi gửi báo cáo"
                        )
                    )
                )
            }
            return
        }

        val targetSellerId = _uiState.value.sellerId.ifBlank { sellerId }
        if (targetSellerId.isBlank()) {
            viewModelScope.launch {
                _events.emit(
                    SellerProfileEvent.ShowMessage(
                        localizedText(
                            english = "Unable to identify seller for reporting",
                            vietnamese = "Không xác định được người bán để báo cáo"
                        )
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val payload = hashMapOf<String, Any>(
                    "targetType" to "SELLER",
                    "targetId" to targetSellerId,
                    "sellerId" to targetSellerId,
                    "reasonCode" to reasonCode,
                    "reasonLabel" to reasonLabel,
                    "description" to details,
                    "reporterId" to reporterId,
                    "status" to "OPEN",
                    "source" to "ANDROID_APP",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("reports").add(payload).await()
                _events.emit(
                    SellerProfileEvent.ShowMessage(
                        localizedText(
                            english = "Report submitted. We will review it soon.",
                            vietnamese = "Đã gửi báo cáo. Chúng tôi sẽ xem xét sớm."
                        )
                    )
                )
            } catch (_: Exception) {
                _events.emit(
                    SellerProfileEvent.ShowMessage(
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

sealed interface SellerProfileEvent {
    data class OpenConversation(val conversationId: String) : SellerProfileEvent
    data class ShowMessage(val message: String) : SellerProfileEvent
}
