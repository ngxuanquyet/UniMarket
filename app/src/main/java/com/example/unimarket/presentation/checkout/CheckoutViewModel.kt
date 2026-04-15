package com.example.unimarket.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.model.PurchaseRequest
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.usecase.auth.GetUserAddressesUseCase
import com.example.unimarket.domain.usecase.auth.GetUserPaymentMethodsUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.cart.GetCartItemsUseCase
import com.example.unimarket.domain.usecase.cart.RemoveFromCartUseCase
import com.example.unimarket.domain.usecase.checkout.ConfirmBuyNowPurchaseUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.presentation.util.localizedText
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

private const val PLATFORM_FEE = 1500.0
private const val SHIPPING_FEE = 30000.0
private const val TRANSFER_OPTION_ID = "transfer"
private const val WALLET_OPTION_ID = "wallet"
private const val CASH_ON_DELIVERY_OPTION_ID = "cash_on_delivery"

data class CheckoutPaymentOption(
    val id: String,
    val type: SellerPaymentMethodType,
    val sellerMethod: SellerPaymentMethod? = null
) {
    val paymentMethodCode: String
        get() = type.storageValue
}

data class CheckoutOrderUiState(
    val id: String,
    val cartItemId: String? = null,
    val product: Product,
    val quantity: Int,
    val availableDeliveryMethods: List<DeliveryMethod> = emptyList(),
    val sellerAddresses: List<UserAddress> = emptyList(),
    val availablePaymentOptions: List<CheckoutPaymentOption> = defaultCheckoutPaymentOptions(),
    val selectedSellerAddressId: String? = null,
    val selectedDeliveryMethod: DeliveryMethod? = null,
    val selectedPaymentOptionId: String = CASH_ON_DELIVERY_OPTION_ID,
    val meetingPoint: String = ""
) {
    val selectedSellerAddress: UserAddress?
        get() = sellerAddresses.firstOrNull { it.id == selectedSellerAddressId }

    val selectedPaymentOption: CheckoutPaymentOption?
        get() = availablePaymentOptions.firstOrNull { it.id == selectedPaymentOptionId }

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
    val buyerWalletBalance: Double = 0.0,
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
    private val getUserPaymentMethodsUseCase: GetUserPaymentMethodsUseCase,
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
            val requestedCount: Int,
            val transferOrderIds: List<String> = emptyList()
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
                        errorMessage = localizedText(
                            english = "Product not found",
                            vietnamese = "Không tìm thấy sản phẩm"
                        )
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
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to load product",
                        vietnamese = "Không thể tải sản phẩm"
                    )
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
                    errorMessage = localizedText(
                        english = "No cart items selected",
                        vietnamese = "Chưa chọn sản phẩm nào trong giỏ"
                    )
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
                        errorMessage = localizedText(
                            english = "Selected cart items were not found",
                            vietnamese = "Không tìm thấy sản phẩm đã chọn trong giỏ"
                        )
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
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to load cart items",
                        vietnamese = "Không thể tải giỏ hàng"
                    )
                )
            }
        }
    }

    private suspend fun loadCheckoutData(baseOrders: List<CheckoutOrderUiState>) {
        val profileResult = refreshCurrentUserProfileUseCase()
        val buyerResult = getUserAddressesUseCase()
        val buyerAddresses = buyerResult.getOrDefault(emptyList())
        val walletBalance = profileResult.getOrNull()?.walletBalance ?: 0.0

        val sellerAddressMap = mutableMapOf<String, List<UserAddress>>()
        val sellerPaymentMap = mutableMapOf<String, List<SellerPaymentMethod>>()
        val sellerErrorMessages = mutableListOf<String>()

        baseOrders.map { it.product.userId }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { sellerUserId ->
                val sellerAddressResult = getUserAddressesUseCase(sellerUserId)
                sellerAddressMap[sellerUserId] = sellerAddressResult.getOrDefault(emptyList())
                sellerAddressResult.exceptionOrNull()?.message?.let(sellerErrorMessages::add)

                val sellerPaymentResult = getUserPaymentMethodsUseCase(sellerUserId)
                sellerPaymentMap[sellerUserId] = sellerPaymentResult.getOrDefault(emptyList())
                sellerPaymentResult.exceptionOrNull()?.message?.let(sellerErrorMessages::add)
            }

        val ordersWithData = baseOrders.map { order ->
            val sellerAddresses = order.product.sellerPickupAddress?.let { listOf(it) }
                ?: sellerAddressMap[order.product.userId].orEmpty()
            val paymentOptions = buildPaymentOptions(
                sellerPaymentMap[order.product.userId].orEmpty()
            )
            order.copy(
                sellerAddresses = sellerAddresses,
                availablePaymentOptions = paymentOptions,
                selectedSellerAddressId = sellerAddresses.firstOrNull { it.isDefault }?.id
                    ?: sellerAddresses.firstOrNull()?.id,
                selectedPaymentOptionId = paymentOptions.firstOrNull { it.id == order.selectedPaymentOptionId }?.id
                    ?: paymentOptions.firstOrNull()?.id.orEmpty()
            )
        }

        _uiState.value = CheckoutUiState(
            orders = ordersWithData,
            buyerAddresses = buyerAddresses,
            selectedBuyerAddressId = buyerAddresses.firstOrNull { it.isDefault }?.id
                ?: buyerAddresses.firstOrNull()?.id,
            buyerWalletBalance = walletBalance,
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

    fun selectPaymentMethod(orderId: String, optionId: String) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId && order.availablePaymentOptions.any { it.id == optionId }) {
                        order.copy(selectedPaymentOptionId = optionId)
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
            emitSnackbar(
                localizedText(
                    english = "No order selected for checkout",
                    vietnamese = "Không có đơn hàng nào được chọn để thanh toán"
                )
            )
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
            val transferOrderIds = mutableListOf<String>()
            val failedMessages = mutableListOf<String>()

            state.orders.forEach { order ->
                val selectedDeliveryMethod = order.selectedDeliveryMethod ?: return@forEach
                val selectedPaymentOption = order.selectedPaymentOption ?: return@forEach
                val result = confirmBuyNowPurchaseUseCase(
                    PurchaseRequest(
                        productId = order.product.id,
                        quantity = order.quantity,
                        deliveryMethod = selectedDeliveryMethod,
                        paymentMethod = selectedPaymentOption.paymentMethodCode,
                        paymentMethodDetails = selectedPaymentOption.sellerMethod,
                        meetingPoint = order.meetingPoint.trim(),
                        buyerAddress = state.selectedBuyerAddress,
                        sellerAddress = order.selectedSellerAddress
                    )
                )

                val confirmation = result.getOrNull()
                if (confirmation != null) {
                    createdOrderIds += confirmation.orderId
                    if (selectedPaymentOption.type == SellerPaymentMethodType.BANK_TRANSFER ||
                        selectedPaymentOption.type == SellerPaymentMethodType.MOMO
                    ) {
                        transferOrderIds += confirmation.orderId
                    }
                    order.cartItemId?.let { cartItemId ->
                        removeFromCartUseCase(cartItemId)
                    }
                } else {
                    failedMessages += "${order.product.name}: ${
                        result.exceptionOrNull()?.message ?: localizedText(
                            english = "Failed to confirm purchase",
                            vietnamese = "Không thể xác nhận mua hàng"
                        )
                    }"
                }
            }

            _uiState.update { it.copy(isSubmitting = false) }

            if (createdOrderIds.isNotEmpty()) {
                refreshCurrentUserProfileUseCase()
                _uiEvent.emit(
                    UiEvent.PurchaseCompleted(
                        orderIds = createdOrderIds,
                        requestedCount = state.orders.size,
                        transferOrderIds = transferOrderIds
                    )
                )
                if (failedMessages.isNotEmpty()) {
                    _uiEvent.emit(UiEvent.ShowSnackbar(failedMessages.joinToString(separator = "\n")))
                }
            } else {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        failedMessages.firstOrNull() ?: localizedText(
                            english = "Failed to confirm purchase",
                            vietnamese = "Không thể xác nhận mua hàng"
                        )
                    )
                )
            }
        }
    }

    private fun validateOrder(
        state: CheckoutUiState,
        order: CheckoutOrderUiState
    ): String? {
        if (order.quantity <= 0) {
            return localizedText(
                english = "Invalid quantity selected for ${order.product.name}",
                vietnamese = "Số lượng đã chọn không hợp lệ cho ${order.product.name}"
            )
        }
        if (order.product.userId.isBlank()) {
            return localizedText(
                english = "Seller information is missing for ${order.product.name}",
                vietnamese = "Thiếu thông tin người bán cho ${order.product.name}"
            )
        }
        if (order.product.quantityAvailable <= 0) {
            return localizedText(
                english = "${order.product.name} is out of stock",
                vietnamese = "${order.product.name} đã hết hàng"
            )
        }
        if (order.quantity > order.product.quantityAvailable) {
            return localizedText(
                english = "Only ${order.product.quantityAvailable} item(s) left for ${order.product.name}",
                vietnamese = "Chỉ còn ${order.product.quantityAvailable} sản phẩm cho ${order.product.name}"
            )
        }
        if (order.availableDeliveryMethods.isEmpty()) {
            return localizedText(
                english = "${order.product.name} has no delivery method configured",
                vietnamese = "${order.product.name} chưa có phương thức giao hàng"
            )
        }
        val selectedPaymentOption = order.selectedPaymentOption
            ?: return localizedText(
                english = "Please select a payment method for ${order.product.name}",
                vietnamese = "Vui lòng chọn phương thức thanh toán cho ${order.product.name}"
            )
        when (selectedPaymentOption.type) {
            SellerPaymentMethodType.BANK_TRANSFER -> Unit

            SellerPaymentMethodType.MOMO -> {
                val sellerMethod = selectedPaymentOption.sellerMethod
                    ?: return localizedText(
                        english = "Seller MoMo information is unavailable",
                        vietnamese = "Thông tin MoMo của người bán không khả dụng"
                    )
                if (sellerMethod.phoneNumber.isBlank()) {
                    return localizedText(
                        english = "Seller MoMo phone number is missing",
                        vietnamese = "Thiếu số điện thoại MoMo của người bán"
                    )
                }
            }

            SellerPaymentMethodType.CASH_ON_DELIVERY -> Unit
            SellerPaymentMethodType.WALLET -> {
                if (state.buyerWalletBalance < order.total) {
                    return localizedText(
                        english = "Insufficient wallet balance for ${order.product.name}",
                        vietnamese = "Số dư ví không đủ để thanh toán ${order.product.name}"
                    )
                }
            }
        }

        return when (order.selectedDeliveryMethod) {
            null -> localizedText(
                english = "Please select a delivery method for ${order.product.name}",
                vietnamese = "Vui lòng chọn phương thức giao hàng cho ${order.product.name}"
            )
            DeliveryMethod.DIRECT_MEET -> {
                if (order.meetingPoint.isBlank()) {
                    localizedText(
                        english = "Please enter a meeting point for ${order.product.name}",
                        vietnamese = "Vui lòng nhập điểm hẹn cho ${order.product.name}"
                    )
                } else {
                    null
                }
            }

            DeliveryMethod.BUYER_TO_SELLER -> {
                if (order.selectedSellerAddress == null) {
                    localizedText(
                        english = "Please choose a seller pickup address for ${order.product.name}",
                        vietnamese = "Vui lòng chọn địa chỉ lấy hàng của người bán cho ${order.product.name}"
                    )
                } else {
                    null
                }
            }

            DeliveryMethod.SELLER_TO_BUYER,
            DeliveryMethod.SHIPPING -> {
                if (state.selectedBuyerAddress == null) {
                    localizedText(
                        english = "Please choose your delivery address",
                        vietnamese = "Vui lòng chọn địa chỉ nhận hàng"
                    )
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
        val paymentOptions = defaultCheckoutPaymentOptions()
        return CheckoutOrderUiState(
            id = id,
            cartItemId = cartItemId,
            product = product,
            quantity = quantity,
            availableDeliveryMethods = availableMethods,
            availablePaymentOptions = paymentOptions,
            selectedDeliveryMethod = availableMethods.firstOrNull(),
            selectedPaymentOptionId = paymentOptions.firstOrNull()?.id.orEmpty()
        )
    }

    private fun emitSnackbar(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar(message))
        }
    }
}

private fun buildPaymentOptions(methods: List<SellerPaymentMethod>): List<CheckoutPaymentOption> {
    val transferMethods = methods.filter { method ->
        method.type == SellerPaymentMethodType.BANK_TRANSFER ||
            method.type == SellerPaymentMethodType.MOMO
    }
    val preferredTransferMethod = transferMethods.firstOrNull { it.isDefault }
        ?: transferMethods.firstOrNull()
    val transferType = preferredTransferMethod?.type ?: SellerPaymentMethodType.BANK_TRANSFER

    return listOf(
        CheckoutPaymentOption(
            id = TRANSFER_OPTION_ID,
            type = transferType,
            sellerMethod = preferredTransferMethod
        ),
        CheckoutPaymentOption(
            id = WALLET_OPTION_ID,
            type = SellerPaymentMethodType.WALLET
        ),
        CheckoutPaymentOption(
            id = CASH_ON_DELIVERY_OPTION_ID,
            type = SellerPaymentMethodType.CASH_ON_DELIVERY
        )
    )
}

private fun defaultCheckoutPaymentOptions(): List<CheckoutPaymentOption> {
    return buildPaymentOptions(emptyList())
}
