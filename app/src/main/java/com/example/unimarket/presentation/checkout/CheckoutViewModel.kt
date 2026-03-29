package com.example.unimarket.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.model.PurchaseRequest
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.auth.GetUserAddressesUseCase
import com.example.unimarket.domain.usecase.checkout.ConfirmBuyNowPurchaseUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val product: Product? = null,
    val availableDeliveryMethods: List<DeliveryMethod> = emptyList(),
    val buyerAddresses: List<UserAddress> = emptyList(),
    val sellerAddresses: List<UserAddress> = emptyList(),
    val selectedBuyerAddressId: String? = null,
    val selectedSellerAddressId: String? = null,
    val selectedDeliveryMethod: DeliveryMethod? = null,
    val meetingPoint: String = "",
    val paymentMethod: String = "Cash on delivery",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedBuyerAddress: UserAddress?
        get() = buyerAddresses.firstOrNull { it.id == selectedBuyerAddressId }

    val selectedSellerAddress: UserAddress?
        get() = sellerAddresses.firstOrNull { it.id == selectedSellerAddressId }
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getUserAddressesUseCase: GetUserAddressesUseCase,
    private val confirmBuyNowPurchaseUseCase: ConfirmBuyNowPurchaseUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState(isLoading = true))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class PurchaseCompleted(val orderId: String) : UiEvent()
    }

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Reusing GetAllProductsUseCase due to lack of GetProductByIdUseCase yet
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
                        val availableMethods = product.deliveryMethodsAvailable
                        _uiState.value = _uiState.value.copy(
                            product = product,
                            availableDeliveryMethods = availableMethods,
                            selectedDeliveryMethod = availableMethods.firstOrNull(),
                            isLoading = false
                        )
                        loadAddresses(product.userId)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Product not found"
                        )
                    }
                }
        }
    }

    private fun loadAddresses(sellerUserId: String) {
        viewModelScope.launch {
            val buyerResult = getUserAddressesUseCase()
            val sellerResult = getUserAddressesUseCase(sellerUserId)

            val buyerAddresses = buyerResult.getOrDefault(emptyList())
            val sellerAddresses = sellerResult.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                buyerAddresses = buyerAddresses,
                sellerAddresses = sellerAddresses,
                selectedBuyerAddressId = buyerAddresses.firstOrNull { it.isDefault }?.id ?: buyerAddresses.firstOrNull()?.id,
                selectedSellerAddressId = sellerAddresses.firstOrNull { it.isDefault }?.id ?: sellerAddresses.firstOrNull()?.id,
                errorMessage = buyerResult.exceptionOrNull()?.message ?: sellerResult.exceptionOrNull()?.message
            )
        }
    }

    fun selectDeliveryMethod(method: DeliveryMethod) {
        if (_uiState.value.availableDeliveryMethods.contains(method)) {
            _uiState.value = _uiState.value.copy(selectedDeliveryMethod = method)
        }
    }

    fun selectBuyerAddress(addressId: String) {
        _uiState.value = _uiState.value.copy(selectedBuyerAddressId = addressId)
    }

    fun selectSellerAddress(addressId: String) {
        _uiState.value = _uiState.value.copy(selectedSellerAddressId = addressId)
    }

    fun updateMeetingPoint(value: String) {
        _uiState.value = _uiState.value.copy(meetingPoint = value)
    }

    fun selectPaymentMethod(value: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = value)
    }

    fun confirmPurchase(quantity: Int) {
        val state = _uiState.value
        if (state.isSubmitting) return

        val product = state.product
        if (product == null) {
            emitSnackbar("Product not found")
            return
        }

        val validationError = validateCheckoutState(state, product, quantity)
        if (validationError != null) {
            emitSnackbar(validationError)
            return
        }

        val selectedDeliveryMethod = state.selectedDeliveryMethod ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val purchaseResult = confirmBuyNowPurchaseUseCase(
                PurchaseRequest(
                    productId = product.id,
                    quantity = quantity,
                    deliveryMethod = selectedDeliveryMethod,
                    paymentMethod = state.paymentMethod,
                    meetingPoint = state.meetingPoint.trim(),
                    buyerAddress = state.selectedBuyerAddress,
                    sellerAddress = state.selectedSellerAddress
                )
            )

            val confirmation = purchaseResult.getOrNull()
            if (confirmation != null) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        product = product.copy(quantityAvailable = confirmation.remainingQuantity)
                    )
                }
                refreshCurrentUserProfileUseCase()
                _uiEvent.emit(UiEvent.PurchaseCompleted(confirmation.orderId))
            } else {
                _uiState.update { it.copy(isSubmitting = false) }
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        purchaseResult.exceptionOrNull()?.message ?: "Failed to confirm purchase"
                    )
                )
            }
        }
    }

    private fun validateCheckoutState(
        state: CheckoutUiState,
        product: Product,
        quantity: Int
    ): String? {
        if (quantity <= 0) return "Invalid quantity selected"
        if (product.userId.isBlank()) return "Seller information is missing"
        if (product.quantityAvailable <= 0) return "This product is out of stock"
        if (quantity > product.quantityAvailable) {
            return "Only ${product.quantityAvailable} item(s) left in stock"
        }
        if (state.availableDeliveryMethods.isEmpty()) {
            return "This seller has not configured any delivery method yet"
        }

        return when (state.selectedDeliveryMethod) {
            null -> "Please select a delivery method"
            DeliveryMethod.DIRECT_MEET -> {
                if (state.meetingPoint.isBlank()) {
                    "Please enter a meeting point"
                } else {
                    null
                }
            }

            DeliveryMethod.BUYER_TO_SELLER -> {
                if (state.selectedSellerAddress == null) {
                    "Please choose a seller pickup address"
                } else {
                    null
                }
            }

            DeliveryMethod.SELLER_TO_BUYER,
            DeliveryMethod.SHIPPING -> {
                if (state.selectedBuyerAddress == null) {
                    "Please choose your delivery address"
                } else {
                    null
                }
            }
        }
    }

    private fun emitSnackbar(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar(message))
        }
    }
}
