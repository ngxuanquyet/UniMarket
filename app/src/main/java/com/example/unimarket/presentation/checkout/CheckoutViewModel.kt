package com.example.unimarket.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.usecase.auth.GetUserAddressesUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val getUserAddressesUseCase: GetUserAddressesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState(isLoading = true))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

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
}
