package com.example.unimarket.presentation.mypurchases

import android.content.Intent
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.unimarket.presentation.util.localizedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPurchasesScreen(
    onBackClick: () -> Unit,
    onPendingPaymentClick: (String) -> Unit = {},
    onTrackOrderClick: (String) -> Unit = {},
    onConversationOpen: (String) -> Unit = {},
    viewModel: MyPurchasesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var reviewTargetOrder by remember { mutableStateOf<Order?>(null) }
    var pendingRating by rememberSaveable { mutableIntStateOf(0) }
    var pendingComment by rememberSaveable { mutableStateOf("") }
    var showReviewSuccessDialog by rememberSaveable { mutableStateOf(false) }
    val selectedTab = PurchaseOrderTab.entries[selectedTabIndex]
    val filteredOrders = remember(uiState.orders, selectedTabIndex) {
        uiState.orders.filter { selectedTab.matches(it.status) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            reviewTargetOrder = null
            pendingRating = 0
            pendingComment = ""
            showReviewSuccessDialog = true
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is MyPurchasesEvent.OpenConversation -> onConversationOpen(event.conversationId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.mypurchases_title),
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
                        colors = listOf(Color(0xFFF8FAFF), BackgroundLight)
                    )
                )
                .padding(paddingValues)
        ) {
            PurchaseTabBar(
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
                        EmptyPurchasesState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            title = if (uiState.orders.isEmpty()) {
                                stringResource(R.string.mypurchases_empty_title)
                            } else {
                                stringResource(R.string.mypurchases_empty_tab_title)
                            },
                            subtitle = if (uiState.orders.isEmpty()) {
                                stringResource(R.string.mypurchases_empty_subtitle)
                            } else {
                                stringResource(R.string.mypurchases_empty_tab_subtitle)
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
                                PurchaseOrderCard(
                                    order = order,
                                    isSubmittingReview = uiState.submittingReviewOrderId == order.id,
                                    onPendingPaymentClick = onPendingPaymentClick,
                                    onTrackOrderClick = onTrackOrderClick,
                                    onContactSeller = { viewModel.contactSeller(order) },
                                    onRateSeller = {
                                        reviewTargetOrder = order
                                        pendingRating = order.reviewRating ?: 0
                                        pendingComment = order.reviewComment
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    reviewTargetOrder?.let { order ->
        ReviewSellerDialog(
            order = order,
            rating = pendingRating,
            comment = pendingComment,
            isSubmitting = uiState.submittingReviewOrderId == order.id,
            onDismiss = {
                if (uiState.submittingReviewOrderId == null) {
                    reviewTargetOrder = null
                    pendingRating = 0
                    pendingComment = ""
                }
            },
            onRatingChange = { pendingRating = it },
            onCommentChange = { pendingComment = it },
            onSubmit = { viewModel.submitReview(order, pendingRating, pendingComment) }
        )
    }

    if (showReviewSuccessDialog) {
        ReviewSuccessDialog(
            onDismiss = { showReviewSuccessDialog = false }
        )
    }
}

@Composable
private fun PurchaseTabBar(
    selectedTab: PurchaseOrderTab,
    onTabSelected: (PurchaseOrderTab) -> Unit
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
            PurchaseOrderTab.entries.forEach { tab ->
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
                        fontSize = 13.sp,
                        maxLines = 1
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
private fun PurchaseOrderCard(
    order: Order,
    isSubmittingReview: Boolean,
    onPendingPaymentClick: (String) -> Unit,
    onTrackOrderClick: (String) -> Unit,
    onContactSeller: () -> Unit,
    onRateSeller: () -> Unit
) {
    val totalAmount = if (order.totalAmount > 0) order.totalAmount else order.unitPrice * order.quantity
    val isWaitingPayment = order.status == OrderStatus.WAITING_PAYMENT
    val isDelivered = order.status == OrderStatus.DELIVERED
    val hasReview = order.reviewRating != null
    val primaryActionLabel = when {
        isWaitingPayment -> stringResource(R.string.mypurchases_pay_now)
        hasReview -> stringResource(R.string.mypurchases_rated_seller, order.reviewRating ?: 0)
        isDelivered -> stringResource(R.string.mypurchases_rate_seller)
        else -> stringResource(R.string.mypurchases_track_order)
    }

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
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = BlueReview,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.storeName,
                        color = TextDarkBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(
                    status = order.status,
                    statusLabel = order.statusLabel
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                PurchaseProductImage(
                    order = order,
                    modifier = Modifier.size(84.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.productName,
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (order.productDetail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = order.productDetail,
                            color = TextGray,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formatVnd(order.unitPrice),
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (order.quantity == 1) {
                        stringResource(R.string.mypurchases_one_item)
                    } else {
                        stringResource(R.string.mypurchases_many_items, order.quantity)
                    },
                    color = TextGray,
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mypurchases_order_total),
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatVnd(totalAmount),
                        color = SecondaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onContactSeller,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDarkBlack)
                ) {
                    Text(
                        text = stringResource(R.string.mypurchases_contact_seller),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (isWaitingPayment) {
                            onPendingPaymentClick(order.id)
                        } else if (isDelivered && !hasReview) {
                            onRateSeller()
                        } else {
                            onTrackOrderClick(order.id)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = (isWaitingPayment || !hasReview) && !isSubmittingReview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasReview) ProfileAvatarBorder else SecondaryBlue,
                        disabledContainerColor = ProfileAvatarBorder,
                        disabledContentColor = TextGray
                    )
                ) {
                    if (isSubmittingReview) {
                        CircularProgressIndicator(
                            color = SurfaceWhite,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = primaryActionLabel,
                            color = if (hasReview && !isWaitingPayment) TextGray else SurfaceWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSellerDialog(
    order: Order,
    rating: Int,
    comment: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.mypurchases_rate_store,
                    order.storeName.ifBlank { stringResource(R.string.mypurchases_seller_fallback) }
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.mypurchases_product_name, order.productName),
                    color = TextDarkBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.mypurchases_quantity_value, order.quantity),
                    color = TextGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BackgroundLight)
                        .border(1.dp, BorderLightGray, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (index in 1..5) {
                            Icon(
                                painter = painterResource(
                                    id = if (index <= rating) {
                                        R.drawable.star_yellow
                                    } else {
                                        R.drawable.star_light_gray
                                    }
                                ),
                                contentDescription = stringResource(R.string.mypurchases_rate_stars, index),
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onRatingChange(index) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    placeholder = {
                        Text(stringResource(R.string.mypurchases_comment_placeholder))
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = rating in 1..5 && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = SurfaceWhite
                    )
                } else {
                    Text(stringResource(R.string.mypurchases_submit))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun ReviewSuccessDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(32.dp),
            color = SurfaceWhite,
            tonalElevation = 0.dp,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SecondaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.mypurchases_success),
                        tint = SurfaceWhite,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.mypurchases_review_success_title),
                    color = TextDarkBlack,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.mypurchases_review_success_message),
                    color = TextGray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                ) {
                    Text(
                        text = stringResource(R.string.mypurchases_back_to_orders),
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.mypurchases_share_unimarket),
                    color = SecondaryBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                context.getString(R.string.mypurchases_share_text)
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.mypurchases_share_unimarket)))
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF8D8F2))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9C3FA0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.mypurchases_verified_review),
                            color = Color(0xFF9C3FA0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseProductImage(
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

    val placeholderArtwork = resolvePlaceholderArtwork(order.productName, order.productDetail)
    val (icon, background, tint) = when (placeholderArtwork) {
        PlaceholderArtwork.BOOK -> Triple(Icons.AutoMirrored.Filled.MenuBook, Color(0xFFF9E7C9), Color(0xFFB88A44))
        PlaceholderArtwork.CALCULATOR -> Triple(Icons.Default.Calculate, Color(0xFFE5E7EB), Color(0xFF5B6473))
        PlaceholderArtwork.DRAFTING_KIT -> Triple(Icons.Default.Straighten, Color(0xFFEAF6E9), Color(0xFF4F8A4E))
        PlaceholderArtwork.PACKAGE -> Triple(Icons.Default.Inventory2, Color(0xFFE8F1FE), Color(0xFF4E7ED8))
        PlaceholderArtwork.BAG -> Triple(Icons.Default.ShoppingBag, Color(0xFFFDEDE5), Color(0xFFCE7B40))
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
private fun StatusBadge(
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
private fun PurchaseOrderTab.label(): String {
    return when (this) {
        PurchaseOrderTab.ALL -> stringResource(R.string.orders_tab_all)
        PurchaseOrderTab.WAITING_PAYMENT -> stringResource(R.string.order_status_waiting_payment)
        PurchaseOrderTab.WAIT_FOR_CONFIRMATION -> stringResource(R.string.order_status_waiting_confirmation)
        PurchaseOrderTab.WAIT_FOR_PICKUP -> stringResource(R.string.order_status_waiting_pickup)
        PurchaseOrderTab.SHIPPING -> stringResource(R.string.order_status_shipping)
        PurchaseOrderTab.DELIVERED -> stringResource(R.string.order_status_delivered)
        PurchaseOrderTab.CANCELLED -> stringResource(R.string.order_status_cancelled)
    }
}

@Composable
private fun EmptyPurchasesState(
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

private enum class PurchaseOrderTab(val label: String) {
    ALL("All"),
    WAITING_PAYMENT("Wait for Payment"),
    WAIT_FOR_CONFIRMATION("Wait for Confirmation"),
    WAIT_FOR_PICKUP("Wait for Pickup"),
    SHIPPING("Shipping"),
    DELIVERED("Đã giao"),
    CANCELLED("Hủy");

    fun matches(status: OrderStatus): Boolean {
        return when (this) {
            ALL -> true
            WAITING_PAYMENT -> status == OrderStatus.WAITING_PAYMENT
            WAIT_FOR_CONFIRMATION -> status == OrderStatus.WAITING_CONFIRMATION
            WAIT_FOR_PICKUP -> status == OrderStatus.WAITING_PICKUP
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

private enum class PlaceholderArtwork {
    BOOK,
    CALCULATOR,
    DRAFTING_KIT,
    PACKAGE,
    BAG
}

private fun resolvePlaceholderArtwork(
    productName: String,
    productDetail: String
): PlaceholderArtwork {
    val haystack = "$productName $productDetail".uppercase()

    return when {
        "BOOK" in haystack || "CHEM" in haystack || "TEXT" in haystack -> PlaceholderArtwork.BOOK
        "CALCULATOR" in haystack || "MATH" in haystack -> PlaceholderArtwork.CALCULATOR
        "DRAFT" in haystack || "COMPASS" in haystack || "SCALE" in haystack -> PlaceholderArtwork.DRAFTING_KIT
        "BAG" in haystack || "HOODIE" in haystack -> PlaceholderArtwork.BAG
        else -> PlaceholderArtwork.PACKAGE
    }
}
