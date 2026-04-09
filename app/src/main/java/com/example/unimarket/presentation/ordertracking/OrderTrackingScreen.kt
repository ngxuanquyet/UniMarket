package com.example.unimarket.presentation.ordertracking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.unimarket.R
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.presentation.theme.ProfileAvatarBorder
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onBackClick: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val order = uiState.order

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.order_tracking_title),
                        fontWeight = FontWeight.Bold,
                        color = TextDarkBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = Color(0xFFF5F6FE)
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SecondaryBlue)
                }
            }

            order == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.order_tracking_not_found),
                        color = TextGray
                    )
                }
            }

            else -> {
                OrderTrackingContent(
                    order = order,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun OrderTrackingContent(order: Order, modifier: Modifier = Modifier) {
    val subtotal = order.unitPrice * order.quantity
    val total = if (order.totalAmount > 0) order.totalAmount else subtotal
    val fee = (total - subtotal).coerceAtLeast(0.0)
    val stages = orderStages()
    val activeStageIndex = stages.activeIndex(order.status)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFEFE8FF))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status.localizedLabel().uppercase(),
                        color = Color(0xFF7D4CC9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = order.arrivingHeadline(),
                    color = TextDarkBlack,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                StageProgressBar(stages = stages, activeIndex = activeStageIndex)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(14.dp)
        ) {
            Column {
                AsyncImage(
                    model = order.productImageUrl,
                    contentDescription = order.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F2F8)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = order.productName,
                    color = TextDarkBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (order.productDetail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.productDetail,
                        color = TextGray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF3D7BF2),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.order_tracking_sold_by, order.storeName),
                        color = Color(0xFF3D7BF2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.common_qty, order.quantity),
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatVnd(total),
                        color = TextDarkBlack,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        InfoCard(
            title = stringResource(R.string.order_tracking_eta_title),
            body = order.etaSubtitle()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.order_tracking_journey_title),
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                orderJourney(order.status).forEachIndexed { index, step ->
                    JourneyItem(
                        title = step.title,
                        subtitle = step.subtitle,
                        isDone = step.isDone,
                        isCurrent = step.isCurrent,
                        showLine = index < orderJourney(order.status).lastIndex
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.order_tracking_payment_summary),
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                SummaryRow(
                    label = stringResource(R.string.checkout_items_subtotal),
                    value = formatVnd(subtotal)
                )
                SummaryRow(
                    label = stringResource(R.string.checkout_platform_fees),
                    value = if (fee > 0) formatVnd(fee) else stringResource(R.string.order_tracking_free)
                )
                HorizontalDivider(color = ProfileAvatarBorder)
                SummaryRow(
                    label = stringResource(R.string.checkout_grand_total),
                    value = formatVnd(total),
                    isStrong = true
                )
            }
        }

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
        ) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.order_tracking_track_live_location),
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.mypurchases_contact_seller),
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = TextDarkBlack
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.order_tracking_get_help),
                    color = TextDarkBlack,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StageProgressBar(stages: List<OrderStatus>, activeIndex: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index <= activeIndex) SecondaryBlue else Color(0xFFE2E6F3)
                        )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stages.forEachIndexed { index, status ->
                Text(
                    text = status.localizedLabel().uppercase(),
                    modifier = Modifier.weight(1f),
                    color = if (index <= activeIndex) SecondaryBlue else TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = body,
                color = TextDarkBlack,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun JourneyItem(
    title: String,
    subtitle: String,
    isDone: Boolean,
    isCurrent: Boolean,
    showLine: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> SecondaryBlue
                            isCurrent -> Color(0xFF8AAAF8)
                            else -> Color(0xFFD8DCE8)
                        }
                    )
            )
            if (showLine) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(Color(0xFFE2E6F3))
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextDarkBlack,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isStrong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = if (isStrong) SecondaryBlue else TextDarkBlack,
            fontWeight = if (isStrong) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = if (isStrong) 20.sp else 14.sp
        )
    }
}

private data class JourneyStep(
    val title: String,
    val subtitle: String,
    val isDone: Boolean,
    val isCurrent: Boolean
)

private fun orderStages(): List<OrderStatus> {
    return listOf(
        OrderStatus.WAITING_CONFIRMATION,
        OrderStatus.SHIPPING,
        OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.DELIVERED
    )
}

private fun List<OrderStatus>.activeIndex(current: OrderStatus): Int {
    return when (current) {
        OrderStatus.WAITING_PAYMENT,
        OrderStatus.WAITING_CONFIRMATION,
        OrderStatus.WAITING_PICKUP -> 0
        OrderStatus.SHIPPING,
        OrderStatus.IN_TRANSIT -> 1
        OrderStatus.OUT_FOR_DELIVERY -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.CANCELLED,
        OrderStatus.UNKNOWN -> 0
    }
}

private fun orderJourney(status: OrderStatus): List<JourneyStep> {
    val index = orderStages().activeIndex(status)
    val titles = listOf(
        "Order placed",
        "Payment processed",
        "Shipped from hub",
        "Estimated delivery"
    )
    val subtitles = listOf(
        "Your order was confirmed by the seller.",
        "Payment has been received.",
        "Your package is moving to your campus.",
        "Pending arrival at your selected location."
    )
    return titles.indices.map { i ->
        JourneyStep(
            title = titles[i],
            subtitle = subtitles[i],
            isDone = i < index,
            isCurrent = i == index
        )
    }
}

private fun Order.arrivingHeadline(): String {
    return when (status) {
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.OUT_FOR_DELIVERY -> "Arriving today"
        OrderStatus.SHIPPING,
        OrderStatus.IN_TRANSIT -> "In transit"
        OrderStatus.WAITING_CONFIRMATION,
        OrderStatus.WAITING_PICKUP -> "Preparing order"
        OrderStatus.WAITING_PAYMENT -> "Waiting payment"
        OrderStatus.CANCELLED -> "Order cancelled"
        OrderStatus.UNKNOWN -> "Order in progress"
    }
}

private fun Order.etaSubtitle(): String {
    return when (status) {
        OrderStatus.DELIVERED -> "Delivered successfully"
        OrderStatus.OUT_FOR_DELIVERY -> "Today, between 2:00 PM - 4:00 PM"
        OrderStatus.SHIPPING,
        OrderStatus.IN_TRANSIT -> "Expected in the next 1-2 days"
        OrderStatus.WAITING_CONFIRMATION,
        OrderStatus.WAITING_PICKUP -> "Seller is preparing your order"
        OrderStatus.WAITING_PAYMENT -> "Waiting for payment confirmation"
        OrderStatus.CANCELLED -> "This order has been cancelled"
        OrderStatus.UNKNOWN -> "Delivery window will be updated soon"
    }
}
