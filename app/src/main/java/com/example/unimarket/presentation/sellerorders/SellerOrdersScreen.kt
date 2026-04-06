package com.example.unimarket.presentation.sellerorders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.BlueReview
import com.example.unimarket.presentation.theme.BorderLightGray
import com.example.unimarket.presentation.theme.GreenBadge
import com.example.unimarket.presentation.theme.GreenBadgeBg
import com.example.unimarket.presentation.theme.LightBlueReviewBg
import com.example.unimarket.presentation.theme.OrangeBadge
import com.example.unimarket.presentation.theme.OrangeBadgeBg
import com.example.unimarket.presentation.theme.ProfileAvatarBorder
import com.example.unimarket.presentation.theme.RedDanger
import com.example.unimarket.presentation.theme.RedDangerBg
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TagBlueBg
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedDeliveryMethodLabel
import com.example.unimarket.presentation.util.localizedLabel
import com.example.unimarket.presentation.util.localizedPaymentMethodLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(
    onBackClick: () -> Unit,
    viewModel: SellerOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = SellerOrderTab.entries[selectedTabIndex]
    val filteredOrders = remember(uiState.orders, selectedTabIndex) {
        uiState.orders.filter { selectedTab.matches(it.status) }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.seller_orders_title), fontWeight = FontWeight.Bold, color = TextDarkBlack) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = TextDarkBlack
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.seller_orders_search_orders),
                            tint = TextDarkBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF7FBFF), BackgroundLight)
                    )
                )
                .padding(paddingValues)
        ) {
            SellerSummaryRow(
                orders = uiState.orders,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
            )

            SellerOrderTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabIndex = it.ordinal }
            )

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading && uiState.orders.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SecondaryBlue)
                        }
                    }

                    filteredOrders.isEmpty() -> {
                        SellerOrdersEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            title = if (uiState.orders.isEmpty()) {
                                stringResource(R.string.seller_orders_empty_title)
                            } else {
                                stringResource(R.string.seller_orders_empty_tab_title)
                            },
                            subtitle = if (uiState.orders.isEmpty()) {
                                stringResource(R.string.seller_orders_empty_subtitle)
                            } else {
                                stringResource(R.string.seller_orders_empty_tab_subtitle)
                            }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 16.dp,
                                bottom = 32.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredOrders, key = { it.documentPath.ifBlank { it.id } }) { order ->
                                SellerOrderCard(
                                    order = order,
                                    isUpdating = uiState.updatingOrderId == order.id,
                                    onAdvance = { viewModel.advanceOrder(order) },
                                    onCancel = { viewModel.cancelOrder(order) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerSummaryRow(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val awaitingCount = orders.count {
        it.status == OrderStatus.WAITING_CONFIRMATION || it.status == OrderStatus.WAITING_PICKUP
    }
    val inTransitCount = orders.count {
        it.status in setOf(OrderStatus.SHIPPING, OrderStatus.IN_TRANSIT, OrderStatus.OUT_FOR_DELIVERY)
    }
    val deliveredCount = orders.count { it.status == OrderStatus.DELIVERED }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SellerSummaryCard(stringResource(R.string.seller_orders_summary_awaiting), awaitingCount.toString(), SecondaryBlue, Modifier.weight(1f))
        SellerSummaryCard(stringResource(R.string.seller_orders_summary_shipping), inTransitCount.toString(), BlueReview, Modifier.weight(1f))
        SellerSummaryCard(stringResource(R.string.seller_orders_summary_delivered), deliveredCount.toString(), GreenBadge, Modifier.weight(1f))
    }
}

@Composable
private fun SellerSummaryCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightGray)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(text = label.uppercase(), color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SellerOrderTabBar(
    selectedTab: SellerOrderTab,
    onTabSelected: (SellerOrderTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SellerOrderTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.label(),
                        color = if (isSelected) SecondaryBlue else TextGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                color = if (isSelected) SecondaryBlue else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
            }
        }

        HorizontalDivider(color = BorderLightGray)
    }
}

@Composable
private fun SellerOrderCard(
    order: Order,
    isUpdating: Boolean,
    onAdvance: () -> Unit,
    onCancel: () -> Unit
) {
    val totalAmount = if (order.totalAmount > 0) order.totalAmount else order.unitPrice * order.quantity
    val primaryAction = primaryActionFor(order)
    val canCancel = canCancel(order)

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLightGray, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightBlueReviewBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BlueReview,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.buyerName.ifBlank { stringResource(R.string.seller_orders_student_buyer) },
                        color = TextDarkBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SellerStatusBadge(
                    status = order.status,
                    statusLabel = order.statusLabel
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                SellerOrderImage(
                    order = order,
                    modifier = Modifier.size(84.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.productName,
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.storeName,
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatVnd(totalAmount),
                        color = SecondaryBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                Text(
                    text = stringResource(R.string.common_qty, order.quantity),
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SellerInfoPillRow(
                deliveryMethod = order.deliveryMethod,
                paymentMethod = order.paymentMethod
            )

    val deliveryNote = sellerDeliveryNote(order)
            if (deliveryNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = deliveryNote,
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger)
                    ) {
                        Text(stringResource(R.string.common_cancel), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                if (primaryAction != null) {
                    Button(
                        onClick = onAdvance,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SurfaceWhite
                            )
                        } else {
                            Text(
                                text = stringResource(primaryAction),
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerInfoPillRow(
    deliveryMethod: String,
    paymentMethod: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SellerInfoPill(
            text = localizedDeliveryMethodLabel(deliveryMethod),
            icon = Icons.Default.LocalShipping,
            background = TagBlueBg,
            tint = SecondaryBlue,
            modifier = Modifier.weight(1f)
        )
        SellerInfoPill(
            text = localizedPaymentMethodLabel(paymentMethod),
            icon = Icons.Default.Inventory2,
            background = LightBlueReviewBg,
            tint = BlueReview,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SellerInfoPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SellerOrderImage(
    order: Order,
    modifier: Modifier = Modifier
) {
    if (order.productImageUrl.isNotBlank()) {
        AsyncImage(
            model = order.productImageUrl,
            contentDescription = order.productName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ProfileAvatarBorder)
        )
        return
    }

    val (icon, background, tint) = when (resolveSellerPlaceholderArtwork(order.productName, order.productDetail)) {
        SellerPlaceholderArtwork.BOOK -> Triple(Icons.AutoMirrored.Filled.MenuBook, Color(0xFFF9E7C9), Color(0xFFB88A44))
        SellerPlaceholderArtwork.CALCULATOR -> Triple(Icons.Default.Calculate, Color(0xFFE5E7EB), Color(0xFF5B6473))
        SellerPlaceholderArtwork.DRAFTING_KIT -> Triple(Icons.Default.Straighten, Color(0xFFEAF6E9), Color(0xFF4F8A4E))
        SellerPlaceholderArtwork.PACKAGE -> Triple(Icons.Default.Inventory2, Color(0xFFE8F1FE), Color(0xFF4E7ED8))
        SellerPlaceholderArtwork.BAG -> Triple(Icons.Default.ShoppingBag, Color(0xFFFDEDE5), Color(0xFFCE7B40))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun SellerStatusBadge(
    status: OrderStatus,
    statusLabel: String
) {
    val (background, contentColor) = when (status) {
        OrderStatus.WAITING_PAYMENT -> Color(0xFFFFF0D8) to Color(0xFFCB8A16)
        OrderStatus.WAITING_CONFIRMATION -> OrangeBadgeBg to OrangeBadge
        OrderStatus.WAITING_PICKUP -> TagBlueBg to SecondaryBlue
        OrderStatus.SHIPPING -> Color(0xFFFCE9F8) to Color(0xFFC457A8)
        OrderStatus.IN_TRANSIT -> LightBlueReviewBg to BlueReview
        OrderStatus.OUT_FOR_DELIVERY -> GreenBadgeBg to GreenBadge
        OrderStatus.DELIVERED -> Color(0xFFE7F8EF) to Color(0xFF249B5B)
        OrderStatus.CANCELLED -> RedDangerBg to RedDanger
        OrderStatus.UNKNOWN -> ProfileAvatarBorder to TextGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = status.localizedLabel(),
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SellerOrdersEmptyState(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(ProfileAvatarBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = TextDarkBlack,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 14.sp
            )
        }
    }
}

private enum class SellerOrderTab(val label: String) {
    ALL("All"),
    WAITING_CONFIRMATION("Chờ xác nhận"),
    WAITING_PICKUP("Chờ lấy hàng"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    CANCELLED("Hủy");

    fun matches(status: OrderStatus): Boolean {
        return when (this) {
            ALL -> true
            WAITING_CONFIRMATION -> status == OrderStatus.WAITING_CONFIRMATION
            WAITING_PICKUP -> status == OrderStatus.WAITING_PICKUP
            SHIPPING -> status in setOf(
                OrderStatus.SHIPPING,
                OrderStatus.IN_TRANSIT,
                OrderStatus.OUT_FOR_DELIVERY
            )
            DELIVERED -> status == OrderStatus.DELIVERED
            CANCELLED -> status == OrderStatus.CANCELLED
        }
    }
}

@Composable
private fun SellerOrderTab.label(): String {
    return when (this) {
        SellerOrderTab.ALL -> stringResource(R.string.orders_tab_all)
        SellerOrderTab.WAITING_CONFIRMATION -> stringResource(R.string.order_status_waiting_confirmation)
        SellerOrderTab.WAITING_PICKUP -> stringResource(R.string.order_status_waiting_pickup)
        SellerOrderTab.SHIPPING -> stringResource(R.string.order_status_shipping)
        SellerOrderTab.DELIVERED -> stringResource(R.string.order_status_delivered)
        SellerOrderTab.CANCELLED -> stringResource(R.string.order_status_cancelled)
    }
}

private enum class SellerPlaceholderArtwork {
    BOOK,
    CALCULATOR,
    DRAFTING_KIT,
    PACKAGE,
    BAG
}

private fun resolveSellerPlaceholderArtwork(
    productName: String,
    productDetail: String
): SellerPlaceholderArtwork {
    val haystack = "$productName $productDetail".uppercase()

    return when {
        "BOOK" in haystack || "CHEM" in haystack || "TEXT" in haystack -> SellerPlaceholderArtwork.BOOK
        "CALCULATOR" in haystack || "MATH" in haystack -> SellerPlaceholderArtwork.CALCULATOR
        "DRAFT" in haystack || "COMPASS" in haystack || "SCALE" in haystack -> SellerPlaceholderArtwork.DRAFTING_KIT
        "BAG" in haystack || "HOODIE" in haystack -> SellerPlaceholderArtwork.BAG
        else -> SellerPlaceholderArtwork.PACKAGE
    }
}

private fun primaryActionFor(order: Order): Int? {
    return when (order.status) {
        OrderStatus.WAITING_CONFIRMATION -> R.string.seller_orders_action_confirm_order
        OrderStatus.WAITING_PICKUP -> R.string.seller_orders_action_start_delivery
        OrderStatus.SHIPPING -> R.string.seller_orders_action_mark_delivered
        // Keep legacy states actionable so older orders can still be closed.
        OrderStatus.IN_TRANSIT -> R.string.seller_orders_action_mark_delivered
        OrderStatus.OUT_FOR_DELIVERY -> R.string.seller_orders_action_mark_delivered
        else -> null
    }
}

private fun canCancel(order: Order): Boolean {
    return order.status in setOf(
        OrderStatus.WAITING_CONFIRMATION,
        OrderStatus.WAITING_PICKUP,
        OrderStatus.SHIPPING,
        OrderStatus.IN_TRANSIT
    )
}

@Composable
private fun sellerDeliveryNote(order: Order): String {
    return when (order.deliveryMethod.uppercase()) {
        "DIRECT_MEET" -> stringResource(
            R.string.seller_orders_note_meeting_point,
            order.meetingPoint.ifBlank { stringResource(R.string.seller_orders_note_not_provided) }
        )
        "BUYER_TO_SELLER" -> stringResource(
            R.string.seller_orders_note_buyer_pickup,
            order.sellerAddress?.shortDisplay().orEmpty().ifBlank { stringResource(R.string.seller_orders_note_no_pickup) }
        )
        "SELLER_TO_BUYER" -> stringResource(
            R.string.seller_orders_note_deliver_to_buyer,
            order.buyerAddress?.shortDisplay().orEmpty().ifBlank { stringResource(R.string.seller_orders_note_no_buyer_address) }
        )
        "SHIPPING" -> stringResource(
            R.string.seller_orders_note_shipping_to,
            order.buyerAddress?.shortDisplay().orEmpty().ifBlank { stringResource(R.string.seller_orders_note_no_shipping_address) }
        )
        else -> order.buyerAddress?.shortDisplay().orEmpty()
    }
}
