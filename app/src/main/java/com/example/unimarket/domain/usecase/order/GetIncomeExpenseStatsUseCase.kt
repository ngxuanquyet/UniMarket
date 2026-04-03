package com.example.unimarket.domain.usecase.order

import com.example.unimarket.domain.model.IncomeExpenseStats
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.StatisticsChartEntry
import com.example.unimarket.domain.model.StatisticsPeriod
import com.example.unimarket.domain.model.StatisticsTransaction
import com.example.unimarket.domain.model.StatisticsTransactionType
import com.example.unimarket.domain.model.TopSellingProduct
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetIncomeExpenseStatsUseCase @Inject constructor() {

    operator fun invoke(
        buyerOrders: List<Order>,
        sellerOrders: List<Order>,
        period: StatisticsPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): IncomeExpenseStats {
        val filteredBuyerOrders = buyerOrders.filter { order ->
            isInSelectedPeriod(orderTimestamp(order), period, nowMillis)
        }
        val filteredSellerOrders = sellerOrders.filter { order ->
            isInSelectedPeriod(orderTimestamp(order), period, nowMillis)
        }

        val deliveredSellerOrders = filteredSellerOrders.filter { it.status == OrderStatus.DELIVERED }
        val deliveredBuyerOrders = filteredBuyerOrders.filter { it.status == OrderStatus.DELIVERED }
        val activeSellerOrders = filteredSellerOrders.filter {
            it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
        }

        val totalIncome = deliveredSellerOrders.sumOf(::orderAmount)
        val totalExpense = deliveredBuyerOrders.sumOf(::orderAmount)
        val pendingIncome = activeSellerOrders.sumOf(::orderAmount)
        val deliveredSalesCount = deliveredSellerOrders.size
        val deliveredPurchaseCount = deliveredBuyerOrders.size
        val averageDeliveredSale = if (deliveredSalesCount == 0) 0.0 else totalIncome / deliveredSalesCount

        return IncomeExpenseStats(
            period = period,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            pendingIncome = pendingIncome,
            deliveredSalesCount = deliveredSalesCount,
            deliveredPurchaseCount = deliveredPurchaseCount,
            averageDeliveredSale = averageDeliveredSale,
            chartEntries = buildChartEntries(
                period = period,
                buyerOrders = filteredBuyerOrders,
                sellerOrders = filteredSellerOrders,
                nowMillis = nowMillis
            ),
            topProducts = deliveredSellerOrders
                .groupBy { it.productId.ifBlank { it.productName.trim().lowercase(Locale.getDefault()) } }
                .map { (productKey, orders) ->
                    TopSellingProduct(
                        productId = productKey,
                        productName = orders.first().productName.ifBlank { "Unknown Item" },
                        orderCount = orders.size,
                        revenue = orders.sumOf(::orderAmount)
                    )
                }
                .sortedWith(
                    compareByDescending<TopSellingProduct> { it.revenue }
                        .thenByDescending { it.orderCount }
                )
                .take(5),
            recentTransactions = buildRecentTransactions(filteredBuyerOrders, filteredSellerOrders)
        )
    }

    private fun buildRecentTransactions(
        buyerOrders: List<Order>,
        sellerOrders: List<Order>
    ): List<StatisticsTransaction> {
        val sellerTransactions = sellerOrders
            .filter { it.status != OrderStatus.CANCELLED }
            .map { order ->
                StatisticsTransaction(
                    id = "income_${order.id}",
                    title = order.productName.ifBlank { "Sold item" },
                    amount = orderAmount(order),
                    timestamp = orderTimestamp(order),
                    type = StatisticsTransactionType.INCOME,
                    status = order.status
                )
            }

        val buyerTransactions = buyerOrders
            .filter { it.status != OrderStatus.CANCELLED }
            .map { order ->
                StatisticsTransaction(
                    id = "expense_${order.id}",
                    title = order.productName.ifBlank { "Purchased item" },
                    amount = orderAmount(order),
                    timestamp = orderTimestamp(order),
                    type = StatisticsTransactionType.EXPENSE,
                    status = order.status
                )
            }

        return (sellerTransactions + buyerTransactions)
            .sortedByDescending { it.timestamp }
            .take(8)
    }

    private fun buildChartEntries(
        period: StatisticsPeriod,
        buyerOrders: List<Order>,
        sellerOrders: List<Order>,
        nowMillis: Long
    ): List<StatisticsChartEntry> {
        val buckets = when (period) {
            StatisticsPeriod.LAST_7_DAYS -> buildDailyBuckets(days = 7, nowMillis = nowMillis)
            StatisticsPeriod.LAST_30_DAYS -> buildDayRangeBuckets(
                totalDays = 30,
                bucketSize = 5,
                nowMillis = nowMillis
            )
            StatisticsPeriod.THIS_MONTH -> buildWeekOfMonthBuckets(nowMillis)
            StatisticsPeriod.ALL_TIME -> buildMonthlyBuckets(monthCount = 6, nowMillis = nowMillis)
        }

        return buckets.map { bucket ->
            StatisticsChartEntry(
                label = bucket.label,
                income = sellerOrders
                    .filter { it.status == OrderStatus.DELIVERED }
                    .filter { bucket.contains(orderTimestamp(it)) }
                    .sumOf(::orderAmount),
                expense = buyerOrders
                    .filter { it.status == OrderStatus.DELIVERED }
                    .filter { bucket.contains(orderTimestamp(it)) }
                    .sumOf(::orderAmount)
            )
        }
    }

    private fun isInSelectedPeriod(
        timestamp: Long,
        period: StatisticsPeriod,
        nowMillis: Long
    ): Boolean {
        if (timestamp <= 0L) return period == StatisticsPeriod.ALL_TIME
        if (period == StatisticsPeriod.ALL_TIME) return true

        val periodStart = when (period) {
            StatisticsPeriod.LAST_7_DAYS -> startOfDay(nowMillis).addDays(-6)
            StatisticsPeriod.LAST_30_DAYS -> startOfDay(nowMillis).addDays(-29)
            StatisticsPeriod.THIS_MONTH -> startOfMonth(nowMillis)
            StatisticsPeriod.ALL_TIME -> 0L
        }

        return timestamp >= periodStart && timestamp <= nowMillis
    }

    private fun orderAmount(order: Order): Double {
        return if (order.totalAmount > 0.0) order.totalAmount else order.unitPrice * order.quantity
    }

    private fun orderTimestamp(order: Order): Long {
        return when {
            order.createdAt > 0L -> order.createdAt
            order.updatedAt > 0L -> order.updatedAt
            else -> 0L
        }
    }

    private fun buildDailyBuckets(days: Int, nowMillis: Long): List<TimeBucket> {
        val todayStart = startOfDay(nowMillis)
        val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())

        return (days - 1 downTo 0).map { offset ->
            val start = todayStart.addDays(-offset)
            TimeBucket(
                label = formatter.format(Date(start)),
                startMillis = start,
                endMillis = start.addDays(1)
            )
        }
    }

    private fun buildDayRangeBuckets(
        totalDays: Int,
        bucketSize: Int,
        nowMillis: Long
    ): List<TimeBucket> {
        val periodStart = startOfDay(nowMillis).addDays(-(totalDays - 1))
        val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        val bucketCount = totalDays / bucketSize

        return (0 until bucketCount).map { index ->
            val start = periodStart.addDays(index * bucketSize)
            val end = minOf(start.addDays(bucketSize), startOfDay(nowMillis).addDays(1))
            TimeBucket(
                label = "${formatter.format(Date(start))}-${formatter.format(Date(end.addDays(-1)))}",
                startMillis = start,
                endMillis = end
            )
        }
    }

    private fun buildWeekOfMonthBuckets(nowMillis: Long): List<TimeBucket> {
        val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        val monthStart = startOfMonth(nowMillis)
        val monthEnd = startOfMonth(nowMillis).addMonths(1)
        val buckets = mutableListOf<TimeBucket>()
        var currentStart = monthStart
        var weekIndex = 1

        while (currentStart < monthEnd) {
            val currentEnd = minOf(currentStart.addDays(7), monthEnd)
            buckets += TimeBucket(
                label = "W$weekIndex",
                startMillis = currentStart,
                endMillis = currentEnd,
                description = "${formatter.format(Date(currentStart))}-${formatter.format(Date(currentEnd.addDays(-1)))}"
            )
            currentStart = currentEnd
            weekIndex += 1
        }

        return buckets
    }

    private fun buildMonthlyBuckets(monthCount: Int, nowMillis: Long): List<TimeBucket> {
        val formatter = SimpleDateFormat("MM/yy", Locale.getDefault())
        val currentMonthStart = startOfMonth(nowMillis)

        return (monthCount - 1 downTo 0).map { offset ->
            val start = currentMonthStart.addMonths(-offset)
            TimeBucket(
                label = formatter.format(Date(start)),
                startMillis = start,
                endMillis = start.addMonths(1)
            )
        }
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfMonth(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun Long.addDays(days: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = this@addDays
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    private fun Long.addMonths(months: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = this@addMonths
            add(Calendar.MONTH, months)
        }.timeInMillis
    }

    private data class TimeBucket(
        val label: String,
        val startMillis: Long,
        val endMillis: Long,
        val description: String = label
    ) {
        fun contains(timestamp: Long): Boolean {
            return timestamp in startMillis until endMillis
        }
    }
}
