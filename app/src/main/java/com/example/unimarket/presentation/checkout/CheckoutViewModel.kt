package com.example.unimarket.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.model.PurchaseRequest
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.usecase.auth.GetUserAddressesUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.cart.GetCartItemsUseCase
import com.example.unimarket.domain.usecase.cart.RemoveFromCartUseCase
import com.example.unimarket.domain.usecase.checkout.ConfirmBuyNowPurchaseUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEFAULT_PAYMENT_METHOD = "Cash on delivery"
private const val PLATFORM_FEE = 1500.0
private const val SHIPPING_FEE = 30000.0

data class CheckoutOrderUiState(
    val id: String,
    val cartItemId: String? = null,
    val product: Product,
    val quantity: Int,
    val availableDeliveryMethods: List<DeliveryMethod> = emptyList(),
    val sellerAddresses: List<UserAddress> = emptyList(),
    val selectedSellerAddressId: String? = null,
    val selectedDeliveryMethod: DeliveryMethod? = null,
    val meetingPoint: String = "",
    val paymentMethod: String = DEFAULT_PAYMENT_METHOD
) {
    val selectedSellerAddress: UserAddress?
        get() = sellerAddresses.firstOrNull { it.id == selectedSellerAddressId }

    val subtotal: Double
        get() = product.price * quantity

    val platformFee: Double
        get() = PLATFORM_FEE

    val deliveryFee: Double
        get() = if (selectedDeliveryMethod == DeliveryMethod.SHIPPING) SHIPPING_FEE else 0.0

    val total: Double
        get() = subtotal + platformFee + deliveryFee
}

data class CheckoutUiState(
    val orders: List<CheckoutOrderUiState> = emptyList(),
    val buyerAddresses: List<UserAddress> = emptyList(),
    val selectedBuyerAddressId: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedBuyerAddress: UserAddress?
        get() = buyerAddresses.firstOrNull { it.id == selectedBuyerAddressId }

    val grandSubtotal: Double
        get() = orders.sumOf { it.subtotal }

    val grandPlatformFee: Double
        get() = orders.sumOf { it.platformFee }

    val grandDeliveryFee: Double
        get() = orders.sumOf { it.deliveryFee }

    val grandTotal: Double
        get() = orders.sumOf { it.total }
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getCartItemsUseCase: GetCartItemsUseCase,
    private val getUserAddressesUseCase: GetUserAddressesUseCase,
    private val confirmBuyNowPurchaseUseCase: ConfirmBuyNowPurchaseUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState(isLoading = true))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class PurchaseCompleted(
            val orderIds: List<String>,
            val requestedCount: Int
        ) : UiEvent()
    }

    fun loadProduct(productId: String, quantity: Int) {
        viewModelScope.launch {
            _uiState.value = CheckoutUiState(isLoading = true)

            runCatching {
                getAllProductsUseCase().first()
            }.onSuccess { products ->
                val product = products.find { it.id == productId }
                if (product == null) {
                    _uiState.value = CheckoutUiState(
                        isLoading = false,
                        errorMessage = "Product not found"
                    )
                    return@onSuccess
                }

                val baseOrder = createOrderState(
                    id = "buy_now_${product.id}",
                    cartItemId = null,
                    product = product,
                    quantity = quantity
                )
                loadCheckoutData(listOf(baseOrder))
            }.onFailure { error ->
                _uiState.value = CheckoutUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load product"
                )
            }
        }
    }

    fun loadCartItems(cartItemIds: List<String>) {
        viewModelScope.launch {
            _uiState.value = CheckoutUiState(isLoading = true)

            if (cartItemIds.isEmpty()) {
                _uiState.value = CheckoutUiState(
                    isLoading = false,
                    errorMessage = "No cart items selected"
                )
                return@launch
            }

            runCatching {
                getCartItemsUseCase().first()
            }.onSuccess { cartItems ->
                val selectedItems = cartItems.filter { it.id in cartItemIds }
                if (selectedItems.isEmpty()) {
                    _uiState.value = CheckoutUiState(
                        isLoading = false,
                        errorMessage = "Selected cart items were not found"
                    )
                    return@onSuccess
                }

                val orders = selectedItems.map { cartItem ->
                    createOrderState(
                        id = cartItem.id,
                        cartItemId = cartItem.id,
                        product = cartItem.product,
                        quantity = cartItem.quantity
                    )
                }
                loadCheckoutData(orders)
            }.onFailure { error ->
                _uiState.value = CheckoutUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load cart items"
                )
            }
        }
    }

    private suspend fun loadCheckoutData(baseOrders: List<CheckoutOrderUiState>) {
        val buyerResult = getUserAddressesUseCase()
        val buyerAddresses = buyerResult.getOrDefault(emptyList())

        val sellerAddressMap = mutableMapOf<String, List<UserAddress>>()
        val sellerErrorMessages = mutableListOf<String>()

        baseOrders.map { it.product.userId }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { sellerUserId ->
                val sellerResult = getUserAddressesUseCase(sellerUserId)
                sellerAddressMap[sellerUserId] = sellerResult.getOrDefault(emptyList())
                sellerResult.exceptionOrNull()?.message?.let(sellerErrorMessages::add)
            }

        val ordersWithAddresses = baseOrders.map { order ->
            val sellerAddresses = order.product.sellerPickupAddress?.let { listOf(it) }
                ?: sellerAddressMap[order.product.userId].orEmpty()
            order.copy(
                sellerAddresses = sellerAddresses,
                selectedSellerAddressId = sellerAddresses.firstOrNull { it.isDefault }?.id
                    ?: sellerAddresses.firstOrNull()?.id
            )
        }

        _uiState.value = CheckoutUiState(
            orders = ordersWithAddresses,
            buyerAddresses = buyerAddresses,
            selectedBuyerAddressId = buyerAddresses.firstOrNull { it.isDefault }?.id
                ?: buyerAddresses.firstOrNull()?.id,
            isLoading = false,
            errorMessage = buyerResult.exceptionOrNull()?.message ?: sellerErrorMessages.firstOrNull()
        )
    }

    fun selectDeliveryMethod(orderId: String, method: DeliveryMethod) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId && order.availableDeliveryMethods.contains(method)) {
                        order.copy(selectedDeliveryMethod = method)
                    } else {
                        order
                    }
                }
            )
        }
    }

    fun selectBuyerAddress(addressId: String) {
        _uiState.update { it.copy(selectedBuyerAddressId = addressId) }
    }

    fun selectSellerAddress(orderId: String, addressId: String) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId) {
                        order.copy(selectedSellerAddressId = addressId)
                    } else {
                        order
                    }
                }
            )
        }
    }

    fun updateMeetingPoint(orderId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId) {
                        order.copy(meetingPoint = value)
                    } else {
                        order
                    }
                }
            )
        }
    }

    fun selectPaymentMethod(orderId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId) {
                        order.copy(paymentMethod = value)
                    } else {
                        order
                    }
                }
            )
        }
    }

    fun confirmPurchase() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (state.orders.isEmpty()) {
            emitSnackbar("No order selected for checkout")
            return
        }

        val validationError = state.orders.firstNotNullOfOrNull { order ->
            validateOrder(state, order)
        }
        if (validationError != null) {
            emitSnackbar(validationError)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val createdOrderIds = mutableListOf<String>()
            val failedMessages = mutableListOf<String>()

            state.orders.forEach { order ->
                val selectedDeliveryMethod = order.selectedDeliveryMethod ?: return@forEach
                val result = confirmBuyNowPurchaseUseCase(
                    PurchaseRequest(
                        productId = order.product.id,
                        quantity = order.quantity,
                        deliveryMethod = selectedDeliveryMethod,
                        paymentMethod = order.paymentMethod,
                        meetingPoint = order.meetingPoint.trim(),
                        buyerAddress = state.selectedBuyerAddress,
                        sellerAddress = order.selectedSellerAddress
                    )
                )

                val confirmation = result.getOrNull()
                if (confirmation != null) {
                    createdOrderIds += confirmation.orderId
                    order.cartItemId?.let { cartItemId ->
                        removeFromCartUseCase(cartItemId)
                    }
                } else {
                    failedMessages += "${order.product.name}: ${result.exceptionOrNull()?.message ?: "Failed to confirm purchase"}"
                }
            }

            _uiState.update { it.copy(isSubmitting = false) }

            if (createdOrderIds.isNotEmpty()) {
                refreshCurrentUserProfileUseCase()
                _uiEvent.emit(
                    UiEvent.PurchaseCompleted(
                        orderIds = createdOrderIds,
                        requestedCount = state.orders.size
                    )
                )
                if (failedMessages.isNotEmpty()) {
                    _uiEvent.emit(UiEvent.ShowSnackbar(failedMessages.joinToString(separator = "\n")))
                }
            } else {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        failedMessages.firstOrNull() ?: "Failed to confirm purchase"
                    )
                )
            }
        }
    }

    private fun validateOrder(
        state: CheckoutUiState,
        order: CheckoutOrderUiState
    ): String? {
        if (order.quantity <= 0) return "Invalid quantity selected for ${order.product.name}"
        if (order.product.userId.isBlank()) return "Seller information is missing for ${order.product.name}"
        if (order.product.quantityAvailable <= 0) return "${order.product.name} is out of stock"
        if (order.quantity > order.product.quantityAvailable) {
            return "Only ${order.product.quantityAvailable} item(s) left for ${order.product.name}"
        }
        if (order.availableDeliveryMethods.isEmpty()) {
            return "${order.product.name} has no delivery method configured"
        }

        return when (order.selectedDeliveryMethod) {
            null -> "Please select a delivery method for ${order.product.name}"
            DeliveryMethod.DIRECT_MEET -> {
                if (order.meetingPoint.isBlank()) {
                    "Please enter a meeting point for ${order.product.name}"
                } else {
                    null
                }
            }

            DeliveryMethod.BUYER_TO_SELLER -> {
                if (order.selectedSellerAddress == null) {
                    "Please choose a seller pickup address for ${order.product.name}"
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

    private fun createOrderState(
        id: String,
        cartItemId: String?,
        product: Product,
        quantity: Int
    ): CheckoutOrderUiState {
        val availableMethods = product.deliveryMethodsAvailable
        return CheckoutOrderUiState(
            id = id,
            cartItemId = cartItemId,
            product = product,
            quantity = quantity,
            availableDeliveryMethods = availableMethods,
            selectedDeliveryMethod = availableMethods.firstOrNull()
        )
    }

    private fun emitSnackbar(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar(message))
        }
    }
}
