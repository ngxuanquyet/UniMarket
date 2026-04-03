package com.example.unimarket.presentation.statistics

import com.example.unimarket.domain.model.IncomeExpenseStats
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.StatisticsPeriod

data class StatisticsUiState(
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.LAST_30_DAYS,
    val stats: IncomeExpenseStats = IncomeExpenseStats(),
    val isLoading: Boolean = false,
    val buyerOrders: List<Order> = emptyList(),
    val sellerOrders: List<Order> = emptyList(),
    val errorMessage: String? = null
)
