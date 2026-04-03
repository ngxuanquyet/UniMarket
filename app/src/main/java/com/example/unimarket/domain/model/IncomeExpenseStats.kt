package com.example.unimarket.domain.model

data class IncomeExpenseStats(
    val period: StatisticsPeriod = StatisticsPeriod.LAST_30_DAYS,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val pendingIncome: Double = 0.0,
    val deliveredSalesCount: Int = 0,
    val deliveredPurchaseCount: Int = 0,
    val averageDeliveredSale: Double = 0.0,
    val chartEntries: List<StatisticsChartEntry> = emptyList(),
    val topProducts: List<TopSellingProduct> = emptyList(),
    val recentTransactions: List<StatisticsTransaction> = emptyList()
)

enum class StatisticsPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,
    ALL_TIME
}

data class StatisticsChartEntry(
    val label: String,
    val income: Double,
    val expense: Double
)

data class TopSellingProduct(
    val productId: String,
    val productName: String,
    val orderCount: Int,
    val revenue: Double
)

data class StatisticsTransaction(
    val id: String,
    val title: String,
    val amount: Double,
    val timestamp: Long,
    val type: StatisticsTransactionType,
    val status: OrderStatus
)

enum class StatisticsTransactionType {
    INCOME,
    EXPENSE
}
