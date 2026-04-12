package com.example.unimarket.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.order.GetSellerOrdersUseCase
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
    val title: String,
    val subtitle: String,
    val amount: Double,
    val isIncoming: Boolean,
    val isSuccessful: Boolean,
    val timestamp: Long
)

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
    private val getSellerOrdersUseCase: GetSellerOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    private var handledTopUpTimestamp: Long = 0L
    private var latestTopUpTransaction: WalletTransaction? = null

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

            val walletBalance = profileResult.getOrNull()?.walletBalance ?: 0.0
            val buyerOrders = buyerOrdersResult.getOrDefault(emptyList())
            val sellerOrders = sellerOrdersResult.getOrDefault(emptyList())

            val transactions = buildTransactions(buyerOrders, sellerOrders)
            val allTransactions = latestTopUpTransaction?.let { topUp ->
                if (transactions.any { it.id == topUp.id }) {
                    transactions
                } else {
                    listOf(topUp) + transactions
                }
            } ?: transactions
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
                )
            }
        }
    }

    fun onTopUpCompleted(amount: Long, timestamp: Long) {
        if (amount <= 0L || timestamp <= 0L) return
        if (timestamp == handledTopUpTimestamp) return

        handledTopUpTimestamp = timestamp
        latestTopUpTransaction = WalletTransaction(
            id = "topup_$timestamp",
            title = localizedText(
                english = "Wallet top-up",
                vietnamese = "Nạp tiền vào ví"
            ),
            subtitle = localizedText(
                english = "Transfer confirmed",
                vietnamese = "Đã xác nhận chuyển khoản"
            ),
            amount = amount.toDouble(),
            isIncoming = true,
            isSuccessful = true,
            timestamp = timestamp
        )
        refresh(showLoading = false)
    }

    private fun buildTransactions(
        buyerOrders: List<Order>,
        sellerOrders: List<Order>
    ): List<WalletTransaction> {
        val sellerTransactions = sellerOrders
            .filter { it.status == OrderStatus.DELIVERED }
            .map { order ->
                WalletTransaction(
                    id = "sell_${order.id}",
                    title = order.productName,
                    subtitle = order.status.label,
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
                    title = order.productName,
                    subtitle = order.status.label,
                    amount = orderAmount(order),
                    isIncoming = false,
                    isSuccessful = order.status == OrderStatus.DELIVERED,
                    timestamp = orderTimestamp(order)
                )
            }

        return (sellerTransactions + buyerTransactions)
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
}
