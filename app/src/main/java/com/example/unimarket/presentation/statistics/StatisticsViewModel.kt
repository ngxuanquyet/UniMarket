package com.example.unimarket.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.order.GetIncomeExpenseStatsUseCase
import com.example.unimarket.domain.usecase.order.GetSellerOrdersUseCase
import com.example.unimarket.domain.model.StatisticsPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val getSellerOrdersUseCase: GetSellerOrdersUseCase,
    private val getIncomeExpenseStatsUseCase: GetIncomeExpenseStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val buyerOrdersDeferred = async { getBuyerOrdersUseCase() }
            val sellerOrdersDeferred = async { getSellerOrdersUseCase() }

            val buyerOrdersResult = buyerOrdersDeferred.await()
            val sellerOrdersResult = sellerOrdersDeferred.await()

            val errorMessage = buyerOrdersResult.exceptionOrNull()?.message
                ?: sellerOrdersResult.exceptionOrNull()?.message

            val buyerOrders = buyerOrdersResult.getOrElse { emptyList() }
            val sellerOrders = sellerOrdersResult.getOrElse { emptyList() }

            _uiState.update { current ->
                val selectedPeriod = current.selectedPeriod
                current.copy(
                    buyerOrders = buyerOrders,
                    sellerOrders = sellerOrders,
                    stats = getIncomeExpenseStatsUseCase(
                        buyerOrders = buyerOrders,
                        sellerOrders = sellerOrders,
                        period = selectedPeriod
                    ),
                    isLoading = false,
                    errorMessage = if (buyerOrders.isEmpty() && sellerOrders.isEmpty()) {
                        errorMessage ?: "Failed to load statistics"
                    } else {
                        errorMessage
                    }
                )
            }
        }
    }

    fun selectPeriod(period: StatisticsPeriod) {
        _uiState.update { current ->
            current.copy(
                selectedPeriod = period,
                stats = getIncomeExpenseStatsUseCase(
                    buyerOrders = current.buyerOrders,
                    sellerOrders = current.sellerOrders,
                    period = period
                )
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
