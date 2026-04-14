package com.example.unimarket.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.order.GetSellerOrdersUseCase
import com.example.unimarket.domain.usecase.wallet.GetWalletLedgerUseCase
import com.example.unimarket.presentation.util.localizedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WalletTransaction(
    val id: String,
    val kind: WalletTransactionKind,
    val title: String,
    val statusLabel: String,
    val amount: Double,
    val isIncoming: Boolean,
    val isSuccessful: Boolean,
    val timestamp: Long
)

enum class WalletTransactionKind {
    ORDER_SALE,
    ORDER_PURCHASE,
    TOP_UP,
    WITHDRAW
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val walletBalance: Double = 0.0,
    val monthlyEarned: Double = 0.0,
    val monthlySpent: Double = 0.0,
    val transactions: List<WalletTransaction> = emptyList(),
    val recentTransactions: List<WalletTransaction> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val getSellerOrdersUseCase: GetSellerOrdersUseCase,
    private val getWalletLedgerUseCase: GetWalletLedgerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    private var handledTopUpTimestamp: Long = 0L
    private var handledWithdrawTimestamp: Long = 0L

    init {
        refresh(showLoading = true)
    }

    fun refresh(showLoading: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = if (showLoading) true else it.isLoading,
                    isRefreshing = !showLoading,
                    errorMessage = null
                )
            }

            val profileResult = refreshCurrentUserProfileUseCase()
            val buyerOrdersResult = getBuyerOrdersUseCase()
            val sellerOrdersResult = getSellerOrdersUseCase()
            val walletLedgerResult = getWalletLedgerUseCase(limit = 100)

            val walletBalance = profileResult.getOrNull()?.walletBalance ?: 0.0
            val buyerOrders = buyerOrdersResult.getOrDefault(emptyList())
            val sellerOrders = sellerOrdersResult.getOrDefault(emptyList())
            val walletLedger = walletLedgerResult.getOrDefault(emptyList())

            val allTransactions = buildTransactions(buyerOrders, sellerOrders, walletLedger)
            val (earned, spent) = computeMonthlyInsights(allTransactions)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    walletBalance = walletBalance,
                    monthlyEarned = earned,
                    monthlySpent = spent,
                    transactions = allTransactions,
                    recentTransactions = allTransactions.take(20),
                    errorMessage = profileResult.exceptionOrNull()?.message
                        ?: buyerOrdersResult.exceptionOrNull()?.message
                        ?: sellerOrdersResult.exceptionOrNull()?.message
                        ?: walletLedgerResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun onTopUpCompleted(amount: Long, timestamp: Long) {
        if (amount <= 0L || timestamp <= 0L) return
        if (timestamp == handledTopUpTimestamp) return

        handledTopUpTimestamp = timestamp
        refresh(showLoading = false)
    }

    fun onWithdrawalRequested(amount: Long, timestamp: Long) {
        if (amount <= 0L || timestamp <= 0L) return
        if (timestamp == handledWithdrawTimestamp) return

        handledWithdrawTimestamp = timestamp
        refresh(showLoading = false)
    }

    private fun buildTransactions(
        buyerOrders: List<Order>,
        sellerOrders: List<Order>,
        walletLedger: List<com.example.unimarket.domain.model.WalletLedgerEntry>
    ): List<WalletTransaction> {
        val sellerTransactions = sellerOrders
            .filter { it.status == OrderStatus.DELIVERED }
            .map { order ->
                WalletTransaction(
                    id = "sell_${order.id}",
                    kind = WalletTransactionKind.ORDER_SALE,
                    title = order.productName,
                    statusLabel = localizedText(
                        english = "Success",
                        vietnamese = "Thành công"
                    ),
                    amount = orderAmount(order),
                    isIncoming = true,
                    isSuccessful = true,
                    timestamp = orderTimestamp(order)
                )
            }

        val buyerTransactions = buyerOrders
            .filter { it.status != OrderStatus.CANCELLED }
            .filter { it.paymentMethod.equals("WALLET", ignoreCase = true) }
            .map { order ->
                WalletTransaction(
                    id = "buy_${order.id}",
                    kind = WalletTransactionKind.ORDER_PURCHASE,
                    title = order.productName,
                    statusLabel = if (order.status == OrderStatus.DELIVERED) {
                        localizedText(english = "Success", vietnamese = "Thành công")
                    } else {
                        localizedText(english = "Pending", vietnamese = "Đang xử lý")
                    },
                    amount = orderAmount(order),
                    isIncoming = false,
                    isSuccessful = order.status == OrderStatus.DELIVERED,
                    timestamp = orderTimestamp(order)
                )
            }

        val walletTransactions = walletLedger.map { entry ->
            val isIncoming = entry.type == "TOP_UP"
            WalletTransaction(
                id = "wallet_${entry.id}",
                kind = if (isIncoming) WalletTransactionKind.TOP_UP else WalletTransactionKind.WITHDRAW,
                title = entry.title.ifBlank {
                    if (isIncoming) {
                        localizedText(english = "Wallet top-up", vietnamese = "Nạp tiền vào ví")
                    } else {
                        localizedText(english = "Wallet withdrawal", vietnamese = "Yêu cầu rút tiền")
                    }
                },
                statusLabel = payoutStatusLabel(entry.status),
                amount = entry.amount,
                isIncoming = isIncoming,
                isSuccessful = entry.status in setOf("APPROVED", "COMPLETED"),
                timestamp = entry.createdAt.takeIf { it > 0L } ?: entry.updatedAt
            )
        }

        return (walletTransactions + sellerTransactions + buyerTransactions)
            .sortedByDescending { it.timestamp }
    }

    private fun computeMonthlyInsights(
        transactions: List<WalletTransaction>
    ): Pair<Double, Double> {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        val monthlyTransactions = transactions.filter { item ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = item.timestamp
            }
            calendar.get(Calendar.YEAR) == currentYear &&
                calendar.get(Calendar.MONTH) == currentMonth
        }

        val earned = monthlyTransactions
            .filter { it.isIncoming && it.isSuccessful }
            .sumOf { it.amount }
        val spent = monthlyTransactions
            .filter { !it.isIncoming }
            .sumOf { it.amount }

        return earned to spent
    }

    private fun orderAmount(order: Order): Double {
        return if (order.totalAmount > 0.0) order.totalAmount else order.unitPrice * order.quantity
    }

    private fun orderTimestamp(order: Order): Long {
        return order.updatedAt.takeIf { it > 0L } ?: order.createdAt
    }

    private fun payoutStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> localizedText(english = "Pending", vietnamese = "Chờ duyệt")
            "APPROVED" -> localizedText(english = "Approved", vietnamese = "Đã duyệt")
            "COMPLETED" -> localizedText(english = "Completed", vietnamese = "Hoàn tất")
            "REJECTED" -> localizedText(english = "Rejected", vietnamese = "Từ chối")
            "CANCELLED" -> localizedText(english = "Cancelled", vietnamese = "Đã hủy")
            "FAILED" -> localizedText(english = "Failed", vietnamese = "Thất bại")
            else -> status.ifBlank { localizedText(english = "Pending", vietnamese = "Đang xử lý") }
        }
    }
}
