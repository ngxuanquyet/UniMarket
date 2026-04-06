package com.example.unimarket.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimarket.R
import com.example.unimarket.domain.model.IncomeExpenseStats
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.StatisticsPeriod
import com.example.unimarket.domain.model.StatisticsTransaction
import com.example.unimarket.domain.model.StatisticsTransactionType
import com.example.unimarket.domain.model.TopSellingProduct
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.BlueReview
import com.example.unimarket.presentation.theme.BorderLightGray
import com.example.unimarket.presentation.theme.GreenBadge
import com.example.unimarket.presentation.theme.GreenBadgeBg
import com.example.unimarket.presentation.theme.LightBlueReviewBg
import com.example.unimarket.presentation.theme.OrangeBadge
import com.example.unimarket.presentation.theme.OrangeBadgeBg
import com.example.unimarket.presentation.theme.RedDanger
import com.example.unimarket.presentation.theme.RedDangerBg
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import com.example.unimarket.presentation.util.formatVnd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.stats_title),
                        fontWeight = FontWeight.Bold,
                        color = TextDarkBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = TextDarkBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF7FBFF), BackgroundLight)
                    )
                )
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.buyerOrders.isEmpty() && uiState.sellerOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SecondaryBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PeriodFilterRow(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = viewModel::selectPeriod
                    )

                    BalanceHeroCard(stats = uiState.stats)

                    MetricsGrid(stats = uiState.stats)

                    ChartCard(stats = uiState.stats)

                    TopProductsCard(products = uiState.stats.topProducts)

                    RecentTransactionsCard(transactions = uiState.stats.recentTransactions)

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: StatisticsPeriod,
    onPeriodSelected: (StatisticsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatisticsPeriod.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.displayLabel()) }
            )
        }
    }
}

@Composable
private fun BalanceHeroCard(stats: IncomeExpenseStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0F4C81), Color(0xFF3B82F6), Color(0xFF7DD3FC))
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_net_balance),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatVnd(stats.netBalance),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroPill(
                    label = stringResource(R.string.stats_income),
                    value = formatVnd(stats.totalIncome),
                    accent = GreenBadgeBg
                )
                HeroPill(
                    label = stringResource(R.string.stats_expense),
                    value = formatVnd(stats.totalExpense),
                    accent = RedDangerBg
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricsGrid(stats: IncomeExpenseStats) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_delivered_sales),
                value = stats.deliveredSalesCount.toString(),
                icon = Icons.Default.Sell,
                accent = BlueReview,
                background = LightBlueReviewBg
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_delivered_purchases),
                value = stats.deliveredPurchaseCount.toString(),
                icon = Icons.Default.Payments,
                accent = OrangeBadge,
                background = OrangeBadgeBg
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_pending_income),
                value = formatVnd(stats.pendingIncome),
                icon = Icons.Default.NorthEast,
                accent = GreenBadge,
                background = GreenBadgeBg
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_avg_sale_value),
                value = formatVnd(stats.averageDeliveredSale),
                icon = Icons.Default.SouthWest,
                accent = RedDanger,
                background = RedDangerBg
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, color = TextGray, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextDarkBlack,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartCard(stats: IncomeExpenseStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightGray)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.stats_cashflow_trend),
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(stats.period.chartSubtitleRes()),
                        color = TextGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = SecondaryBlue
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (stats.chartEntries.all { it.income <= 0.0 && it.expense <= 0.0 }) {
                Text(
                    text = stringResource(R.string.stats_no_delivered_transactions),
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val maxValue = stats.chartEntries
                    .flatMap { listOf(it.income, it.expense) }
                    .maxOrNull()
                    ?.takeIf { it > 0.0 }
                    ?: 1.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    stats.chartEntries.forEach { entry ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxHeight(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Bar(
                                        ratio = (entry.income / maxValue).toFloat(),
                                        color = SecondaryBlue
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Bar(
                                        ratio = (entry.expense / maxValue).toFloat(),
                                        color = OrangeBadge
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = entry.label,
                                color = TextGray,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendDot(label = stringResource(R.string.stats_income), color = SecondaryBlue)
                    LegendDot(label = stringResource(R.string.stats_expense), color = OrangeBadge)
                }
            }
        }
    }
}

@Composable
private fun Bar(
    ratio: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight(ratio.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(color)
    )
}

@Composable
private fun LegendDot(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = TextGray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TopProductsCard(products: List<TopSellingProduct>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightGray)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_top_products),
                color = TextDarkBlack,
                fontWeight = FontWeight.Bold
            )

            if (products.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_top_products),
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                products.forEachIndexed { index, product ->
                    TopProductRow(
                        rank = index + 1,
                        product = product
                    )
                    if (index != products.lastIndex) {
                        HorizontalDivider(color = BorderLightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProductRow(
    rank: Int,
    product: TopSellingProduct
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LightBlueReviewBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = rank.toString(), color = BlueReview, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.productName,
                color = TextDarkBlack,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.stats_orders_count, product.orderCount),
                color = TextGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = formatVnd(product.revenue),
            color = SecondaryBlue,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecentTransactionsCard(transactions: List<StatisticsTransaction>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightGray)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_recent_transactions),
                color = TextDarkBlack,
                fontWeight = FontWeight.Bold
            )

            if (transactions.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_transactions),
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                transactions.forEachIndexed { index, transaction ->
                    TransactionRow(transaction = transaction)
                    if (index != transactions.lastIndex) {
                        HorizontalDivider(color = BorderLightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: StatisticsTransaction) {
    val amountColor = when (transaction.type) {
        StatisticsTransactionType.INCOME -> GreenBadge
        StatisticsTransactionType.EXPENSE -> RedDanger
    }
    val amountPrefix = when (transaction.type) {
        StatisticsTransactionType.INCOME -> "+"
        StatisticsTransactionType.EXPENSE -> "-"
    }
    val iconBackground = when (transaction.type) {
        StatisticsTransactionType.INCOME -> GreenBadgeBg
        StatisticsTransactionType.EXPENSE -> RedDangerBg
    }
    val iconTint = when (transaction.type) {
        StatisticsTransactionType.INCOME -> GreenBadge
        StatisticsTransactionType.EXPENSE -> RedDanger
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (transaction.type == StatisticsTransactionType.INCOME) {
                    Icons.Default.NorthEast
                } else {
                    Icons.Default.SouthWest
                },
                contentDescription = null,
                tint = iconTint
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                color = TextDarkBlack,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${transaction.status.displayLabel()} • ${transaction.timestamp.displayDate()}",
                color = TextGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = amountPrefix + formatVnd(transaction.amount),
            color = amountColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatisticsPeriod.displayLabel(): String {
    return stringResource(labelRes())
}

private fun StatisticsPeriod.labelRes(): Int {
    return when (this) {
        StatisticsPeriod.LAST_7_DAYS -> R.string.stats_period_7_days
        StatisticsPeriod.LAST_30_DAYS -> R.string.stats_period_30_days
        StatisticsPeriod.THIS_MONTH -> R.string.stats_period_this_month
        StatisticsPeriod.ALL_TIME -> R.string.stats_period_all_time
    }
}

private fun StatisticsPeriod.chartSubtitleRes(): Int {
    return when (this) {
        StatisticsPeriod.LAST_7_DAYS -> R.string.stats_chart_subtitle_7_days
        StatisticsPeriod.LAST_30_DAYS -> R.string.stats_chart_subtitle_30_days
        StatisticsPeriod.THIS_MONTH -> R.string.stats_chart_subtitle_this_month
        StatisticsPeriod.ALL_TIME -> R.string.stats_chart_subtitle_all_time
    }
}

@Composable
private fun OrderStatus.displayLabel(): String {
    return stringResource(
        when (this) {
            OrderStatus.WAITING_PAYMENT -> R.string.order_status_waiting_payment
            OrderStatus.WAITING_CONFIRMATION -> R.string.order_status_waiting_confirmation
            OrderStatus.WAITING_PICKUP -> R.string.order_status_waiting_pickup
            OrderStatus.SHIPPING -> R.string.order_status_shipping
            OrderStatus.IN_TRANSIT -> R.string.order_status_in_transit
            OrderStatus.OUT_FOR_DELIVERY -> R.string.order_status_out_for_delivery
            OrderStatus.DELIVERED -> R.string.order_status_delivered
            OrderStatus.CANCELLED -> R.string.order_status_cancelled
            OrderStatus.UNKNOWN -> R.string.order_status_unknown
        }
    )
}

@Composable
private fun Long.displayDate(): String {
    if (this <= 0L) return stringResource(R.string.stats_no_date)
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
}
