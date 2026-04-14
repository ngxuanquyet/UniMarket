package com.example.unimarket.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.TopUpPaymentStatus
import com.example.unimarket.domain.usecase.order.CheckTransferPaymentUseCase
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.order.UpdateOrderStatusUseCase
import com.example.unimarket.domain.usecase.wallet.CheckTopUpTransferPaymentUseCase
import com.example.unimarket.presentation.util.localizedText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrTransferUiState(
    val requestedOrderIds: List<String> = emptyList(),
    val orders: List<Order> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isCheckingPayment: Boolean = false,
    val isCancellingPayment: Boolean = false,
    val topUpAmount: Long = 0L,
    val topUpTransferContent: String = "",
    val isTopUpCompleted: Boolean = false,
    val appTransferBankCode: String = DEFAULT_APP_TRANSFER_BANK_CODE,
    val appTransferBankName: String = DEFAULT_APP_TRANSFER_BANK_NAME,
    val appTransferAccountName: String = DEFAULT_APP_TRANSFER_ACCOUNT_NAME,
    val appTransferAccountNumber: String = DEFAULT_APP_TRANSFER_ACCOUNT_NUMBER,
    val errorMessage: String? = null
) {
    val currentOrder: Order?
        get() = orders.getOrNull(currentIndex)
    val isTopUpMode: Boolean
        get() = topUpAmount > 0L
}

@HiltViewModel
class QrTransferViewModel @Inject constructor(
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val checkTransferPaymentUseCase: CheckTransferPaymentUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val checkTopUpTransferPaymentUseCase: CheckTopUpTransferPaymentUseCase,
    private val auth: FirebaseAuth,
    private val remoteConfig: FirebaseRemoteConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrTransferUiState(isLoading = true))
    val uiState: StateFlow<QrTransferUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        loadAppTransferConfig()
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class PaymentConfirmed(val orderId: String) : UiEvent()
        data class AllTransfersCompleted(val order: Order) : UiEvent()
        data object ExitAfterCancel : UiEvent()
        data object TopUpCompleted : UiEvent()
    }

    fun loadOrders(orderIds: List<String>) {
        if (_uiState.value.isTopUpMode) return
        if (orderIds.isEmpty()) {
            _uiState.value = QrTransferUiState(
                requestedOrderIds = emptyList(),
                isLoading = false,
                errorMessage = localizedText(
                    english = "No transfer order found",
                    vietnamese = "Không tìm thấy đơn chuyển khoản"
                )
            )
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    requestedOrderIds = orderIds,
                    isLoading = true,
                    errorMessage = null
                )
            }
            refreshOrders(
                orderIds = orderIds,
                preserveSelection = false
            )
        }
    }

    fun startTopUpFlow(amount: Long) {
        if (amount <= 0L) {
            _uiState.value = QrTransferUiState(
                isLoading = false,
                errorMessage = localizedText(
                    english = "Invalid top-up amount",
                    vietnamese = "Số tiền nạp không hợp lệ"
                )
            )
            return
        }

        val userSuffix = auth.currentUser?.uid?.takeLast(6)?.uppercase().orEmpty().ifBlank { "USER" }
        val transferContent = "UMTOPUP${System.currentTimeMillis()}$userSuffix"
        _uiState.value = QrTransferUiState(
            isLoading = false,
            topUpAmount = amount,
            topUpTransferContent = transferContent
        )
    }

    fun moveToNextOrder() {
        val state = _uiState.value
        val currentOrder = state.currentOrder
        if (state.currentIndex < state.orders.lastIndex) {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else if (currentOrder != null) {
            emitAllTransfersCompleted(currentOrder)
        }
    }

    fun onPaymentSuccessHandled(orderId: String) {
        val state = _uiState.value
        val currentOrder = state.currentOrder
        if (currentOrder?.id != orderId) return

        if (state.currentIndex < state.orders.lastIndex) {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else {
            emitAllTransfersCompleted(currentOrder)
        }
    }

    fun checkCurrentOrderPayment(showPendingMessage: Boolean) {
        val order = _uiState.value.currentOrder ?: return
        if (order.status != OrderStatus.WAITING_PAYMENT) return
        if (_uiState.value.isCheckingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPayment = true) }

            val result = checkTransferPaymentUseCase(order.id)
            _uiState.update { it.copy(isCheckingPayment = false) }

            val checkResult = result.getOrNull()
            if (checkResult != null) {
                _uiState.update { state ->
                    state.copy(
                        orders = state.orders.map { existing ->
                            if (existing.id == order.id) {
                                existing.copy(
                                    status = checkResult.status,
                                    statusLabel = checkResult.statusLabel,
                                    paymentExpiresAt = checkResult.paymentExpiresAt,
                                    paymentConfirmedAt = checkResult.paymentConfirmedAt,
                                    updatedAt = System.currentTimeMillis()
                                )
                            } else {
                                existing
                            }
                        }
                    )
                }

                when (checkResult.status) {
                    OrderStatus.WAITING_PAYMENT -> {
                        if (showPendingMessage) {
                            _uiEvent.emit(
                                UiEvent.ShowSnackbar(
                                    localizedText(
                                        english = "Payment not found yet. Please try again in a moment.",
                                        vietnamese = "Chưa ghi nhận thanh toán. Vui lòng thử lại sau ít phút."
                                    )
                                )
                            )
                        }
                    }

                    OrderStatus.CANCELLED -> {
                        _uiEvent.emit(
                            UiEvent.ShowSnackbar(
                                localizedText(
                                    english = "This transfer request has expired.",
                                    vietnamese = "Yêu cầu chuyển khoản này đã hết hạn."
                                )
                            )
                        )
                    }

                    else -> {
                        _uiEvent.emit(UiEvent.PaymentConfirmed(order.id))
                    }
                }
            } else {
                val error = result.exceptionOrNull()
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        error?.message ?: localizedText(
                            english = "Failed to verify transfer payment",
                            vietnamese = "Không thể xác minh thanh toán chuyển khoản"
                        )
                    )
                )
            }

        }
    }

    fun checkTopUpPayment(showPendingMessage: Boolean) {
        val state = _uiState.value
        if (!state.isTopUpMode || state.isTopUpCompleted) return
        if (state.isCheckingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPayment = true) }

            val result = checkTopUpTransferPaymentUseCase(
                amount = state.topUpAmount,
                transferContent = state.topUpTransferContent
            )
            _uiState.update { it.copy(isCheckingPayment = false) }

            val checkResult = result.getOrNull()
            if (checkResult != null) {
                if (checkResult.status == TopUpPaymentStatus.CONFIRMED) {
                    _uiState.update { it.copy(isTopUpCompleted = true) }
                    _uiEvent.emit(UiEvent.TopUpCompleted)
                } else if (showPendingMessage) {
                    _uiEvent.emit(
                        UiEvent.ShowSnackbar(
                            localizedText(
                                english = "Payment not found yet. Please try again in a moment.",
                                vietnamese = "Chưa ghi nhận thanh toán. Vui lòng thử lại sau ít phút."
                            )
                        )
                    )
                }
            } else {
                _uiState.update { it.copy(isCheckingPayment = false) }
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        result.exceptionOrNull()?.message ?: localizedText(
                            english = "Failed to verify transfer payment",
                            vietnamese = "Không thể xác minh thanh toán chuyển khoản"
                        )
                    )
                )
            }
        }
    }

    fun cancelPaymentAndExit() {
        val state = _uiState.value
        if (state.isTopUpMode) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ExitAfterCancel) }
            return
        }

        val order = state.currentOrder ?: return
        if (order.status != OrderStatus.WAITING_PAYMENT) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ExitAfterCancel) }
            return
        }
        if (state.isCancellingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCancellingPayment = true) }
            val cancelResult = updateOrderStatusUseCase(order, OrderStatus.CANCELLED)
            _uiState.update { it.copy(isCancellingPayment = false) }

            if (cancelResult.isSuccess) {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        localizedText(
                            english = "Payment has been cancelled.",
                            vietnamese = "Đã hủy thanh toán."
                        )
                    )
                )
                _uiEvent.emit(UiEvent.ExitAfterCancel)
            } else {
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        cancelResult.exceptionOrNull()?.message ?: localizedText(
                            english = "Failed to cancel payment",
                            vietnamese = "Không thể hủy thanh toán"
                        )
                    )
                )
            }
        }
    }

    private suspend fun refreshOrders(
        orderIds: List<String>,
        preserveSelection: Boolean
    ) {
        val previousCurrentOrderId = _uiState.value.currentOrder?.id

        getBuyerOrdersUseCase()
            .onSuccess { orders ->
                val mappedOrders = orderIds.mapNotNull { orderId ->
                    orders.firstOrNull { it.id == orderId }
                }

                val newIndex = when {
                    mappedOrders.isEmpty() -> 0
                    preserveSelection && previousCurrentOrderId != null -> {
                        mappedOrders.indexOfFirst { it.id == previousCurrentOrderId }
                            .takeIf { it >= 0 }
                            ?: 0
                    }
                    else -> 0
                }

                _uiState.update {
                    it.copy(
                        orders = mappedOrders,
                        currentIndex = newIndex.coerceIn(0, mappedOrders.lastIndex.coerceAtLeast(0)),
                        isLoading = false,
                        errorMessage = if (mappedOrders.isEmpty()) {
                            localizedText(
                                english = "Transfer order was not found",
                                vietnamese = "Không tìm thấy đơn chuyển khoản"
                            )
                        } else {
                            null
                        }
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        orders = emptyList(),
                        currentIndex = 0,
                        isLoading = false,
                        errorMessage = error.message ?: localizedText(
                            english = "Failed to load transfer details",
                            vietnamese = "Không thể tải thông tin chuyển khoản"
                        )
                    )
                }
            }
    }

    private fun emitAllTransfersCompleted(order: Order) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.AllTransfersCompleted(order))
        }
    }

    private fun loadAppTransferConfig() {
        viewModelScope.launch {
            runCatching { remoteConfig.fetchAndActivate().await() }

            val remoteAccountName = remoteConfig.getString(KEY_BANK_ACCOUNT_NAME).trim()
            val remoteAccountNumber = remoteConfig.getString(KEY_BANK_ACCOUNT_NUMBER).trim()
            val remoteBankCode = remoteConfig.getString(KEY_BANK_CODE).trim()
            val remoteBankName = remoteConfig.getString(KEY_BANK_NAME).trim()

            _uiState.update {
                it.copy(
                    appTransferBankCode = remoteBankCode.ifBlank { DEFAULT_APP_TRANSFER_BANK_CODE },
                    appTransferBankName = remoteBankName.ifBlank { DEFAULT_APP_TRANSFER_BANK_NAME },
                    appTransferAccountName = remoteAccountName.ifBlank { DEFAULT_APP_TRANSFER_ACCOUNT_NAME },
                    appTransferAccountNumber = remoteAccountNumber.ifBlank { DEFAULT_APP_TRANSFER_ACCOUNT_NUMBER }
                )
            }
        }
    }

    private companion object {
        const val KEY_BANK_ACCOUNT_NAME = "BANK_ACCOUNT_NAME"
        const val KEY_BANK_ACCOUNT_NUMBER = "BANK_ACCOUNT_NUMBER"
        const val KEY_BANK_CODE = "BANK_CODE"
        const val KEY_BANK_NAME = "BANK_NAME"
    }
}

private const val DEFAULT_APP_TRANSFER_BANK_CODE = "MB"
private const val DEFAULT_APP_TRANSFER_BANK_NAME = "MBBank"
private const val DEFAULT_APP_TRANSFER_ACCOUNT_NUMBER = "0356433860"
private const val DEFAULT_APP_TRANSFER_ACCOUNT_NAME = "NGUYEN XUAN QUYET"
