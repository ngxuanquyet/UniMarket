package com.example.unimarket.presentation.checkout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.DividerColor
import com.example.unimarket.presentation.theme.ProfileAvatarBorder
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.util.formatVnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    productId: String? = null,
    quantity: Int = 1,
    cartItemIds: List<String> = emptyList(),
    onBackClick: () -> Unit,
    onPurchaseCompleted: () -> Unit = {},
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var completedOrderInfo by remember {
        mutableStateOf<Pair<List<String>, Int>?>(null)
    }

    LaunchedEffect(productId, quantity, cartItemIds) {
        if (cartItemIds.isNotEmpty()) {
            viewModel.loadCartItems(cartItemIds)
        } else if (!productId.isNullOrBlank()) {
            viewModel.loadProduct(productId, quantity)
        }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is CheckoutViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is CheckoutViewModel.UiEvent.PurchaseCompleted -> {
                    completedOrderInfo = event.orderIds to event.requestedCount
                }
            }
        }
    }

    completedOrderInfo?.let { (orderIds, requestedCount) ->
        val completedCount = orderIds.size
        val isFullSuccess = completedCount == requestedCount

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(if (isFullSuccess) "Purchase confirmed" else "Partial purchase completed")
            },
            text = {
                Text(
                    if (isFullSuccess) {
                        "Created $completedCount order(s) successfully."
                    } else {
                        "Created $completedCount/$requestedCount order(s). Check the snackbar for failed orders."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        completedOrderInfo = null
                        onPurchaseCompleted()
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SecondaryBlue)
                }
            }

            uiState.orders.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "No order found", color = Color.Gray)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    SectionTitle("ORDER DETAILS")

                    uiState.orders.forEachIndexed { index, order ->
                        CheckoutOrderCard(
                            orderIndex = index + 1,
                            order = order,
                            buyerAddresses = uiState.buyerAddresses,
                            selectedBuyerAddressId = uiState.selectedBuyerAddressId,
                            onSelectDeliveryMethod = { method ->
                                viewModel.selectDeliveryMethod(order.id, method)
                            },
                            onSelectBuyerAddress = viewModel::selectBuyerAddress,
                            onSelectSellerAddress = { addressId ->
                                viewModel.selectSellerAddress(order.id, addressId)
                            },
                            onMeetingPointChange = { value ->
                                viewModel.updateMeetingPoint(order.id, value)
                            },
                            onSelectPaymentMethod = { method ->
                                viewModel.selectPaymentMethod(order.id, method)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OverallSummaryCard(uiState = uiState)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = viewModel::confirmPurchase,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Processing...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Confirm ${uiState.orders.size} Order(s)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        "SECURE CAMPUS TRANSACTION",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 16.dp, bottom = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutOrderCard(
    orderIndex: Int,
    order: CheckoutOrderUiState,
    buyerAddresses: List<UserAddress>,
    selectedBuyerAddressId: String?,
    onSelectDeliveryMethod: (DeliveryMethod) -> Unit,
    onSelectBuyerAddress: (String) -> Unit,
    onSelectSellerAddress: (String) -> Unit,
    onMeetingPointChange: (String) -> Unit,
    onSelectPaymentMethod: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order $orderIndex",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OrderItemSummary(order = order)

            Spacer(modifier = Modifier.height(20.dp))

            Text("Delivery Method", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            if (order.availableDeliveryMethods.isEmpty()) {
                Text(
                    text = "Người bán chưa thiết lập phương thức giao nhận.",
                    color = Color.Gray
                )
            } else {
                order.availableDeliveryMethods.forEach { method ->
                    DeliveryOptionCard(
                        title = method.title,
                        subtitle = method.subtitle,
                        isSelected = order.selectedDeliveryMethod == method,
                        icon = method.icon(),
                        onClick = { onSelectDeliveryMethod(method) }
                    ) {
                        if (order.selectedDeliveryMethod == method) {
                            DeliveryMethodDetails(
                                method = method,
                                buyerAddresses = buyerAddresses,
                                sellerAddresses = order.sellerAddresses,
                                selectedBuyerAddressId = selectedBuyerAddressId,
                                selectedSellerAddressId = order.selectedSellerAddressId,
                                meetingPoint = order.meetingPoint,
                                onBuyerAddressClick = onSelectBuyerAddress,
                                onSellerAddressClick = onSelectSellerAddress,
                                onMeetingPointChange = onMeetingPointChange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionCard(
                title = "Cash on delivery",
                isSelected = order.paymentMethod == "Cash on delivery",
                icon = Icons.Default.Money,
                onClick = { onSelectPaymentMethod("Cash on delivery") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionCard(
                title = "Bank Transfer",
                isSelected = order.paymentMethod == "Bank Transfer",
                icon = Icons.Default.AccountBalance,
                onClick = { onSelectPaymentMethod("Bank Transfer") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Order Total", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Subtotal", formatVnd(order.subtotal))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Platform Fee", formatVnd(order.platformFee))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Delivery", formatVnd(order.deliveryFee))

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    formatVnd(order.total),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SecondaryBlue
                )
            }
        }
    }
}

@Composable
private fun OrderItemSummary(order: CheckoutOrderUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = rememberAsyncImagePainter(model = order.product.imageUrls.firstOrNull()),
            contentDescription = order.product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProfileAvatarBorder)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(order.product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Condition: ${order.product.condition}", color = Color.Gray, fontSize = 12.sp)
            Text("Quantity: ${order.quantity}", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatVnd(order.product.price),
                color = SecondaryBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun OverallSummaryCard(uiState: CheckoutUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overall Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow("Orders", uiState.orders.size.toString())
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Items subtotal", formatVnd(uiState.grandSubtotal))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Platform fees", formatVnd(uiState.grandPlatformFee))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Delivery fees", formatVnd(uiState.grandDeliveryFee))

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Grand total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    formatVnd(uiState.grandTotal),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SecondaryBlue
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DeliveryMethodDetails(
    method: DeliveryMethod,
    buyerAddresses: List<UserAddress>,
    sellerAddresses: List<UserAddress>,
    selectedBuyerAddressId: String?,
    selectedSellerAddressId: String?,
    meetingPoint: String,
    onBuyerAddressClick: (String) -> Unit,
    onSellerAddressClick: (String) -> Unit,
    onMeetingPointChange: (String) -> Unit
) {
    when (method) {
        DeliveryMethod.DIRECT_MEET -> {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = meetingPoint,
                onValueChange = onMeetingPointChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Điểm gặp trực tiếp") },
                placeholder = { Text("VD: Cổng trường, canteen, sân trường...") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = SecondaryBlue
                )
            )
        }

        DeliveryMethod.BUYER_TO_SELLER -> {
            AddressSelector(
                title = "Địa chỉ người bán",
                addresses = sellerAddresses,
                selectedAddressId = selectedSellerAddressId,
                emptyState = "Người bán chưa có địa chỉ để bạn đến nhận hàng.",
                onAddressClick = onSellerAddressClick
            )
        }

        DeliveryMethod.SELLER_TO_BUYER -> {
            AddressSelector(
                title = "Địa chỉ người mua",
                addresses = buyerAddresses,
                selectedAddressId = selectedBuyerAddressId,
                emptyState = "Bạn chưa có địa chỉ. Hãy thêm địa chỉ trong Profile.",
                onAddressClick = onBuyerAddressClick
            )
        }

        DeliveryMethod.SHIPPING -> {
            AddressSelector(
                title = "Địa chỉ nhận hàng",
                addresses = buyerAddresses,
                selectedAddressId = selectedBuyerAddressId,
                emptyState = "Bạn chưa có địa chỉ giao hàng. Hãy thêm địa chỉ trong Profile.",
                onAddressClick = onBuyerAddressClick
            )
        }
    }
}

@Composable
private fun AddressSelector(
    title: String,
    addresses: List<UserAddress>,
    selectedAddressId: String?,
    emptyState: String,
    onAddressClick: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (addresses.isEmpty()) {
        Text(emptyState, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        addresses.forEach { address ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = if (selectedAddressId == address.id) SecondaryBlue else Color.LightGray,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onAddressClick(address.id) }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (selectedAddressId == address.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selectedAddressId == address.id) SecondaryBlue else Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildString {
                                append("Địa chỉ")
                                if (address.isDefault) append(" • Mặc định")
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(address.recipientName, style = MaterialTheme.typography.bodySmall)
                        if (address.phoneNumber.isNotBlank()) {
                            Text(address.phoneNumber, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(address.shortDisplay(), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    extraContent: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SecondaryBlue else Color.LightGray,
                shape = RoundedCornerShape(24.dp)
            )
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) SecondaryBlue else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) SecondaryBlue else Color.LightGray
                )
            }
            extraContent()
        }
    }
}

@Composable
private fun PaymentOptionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(24.dp)
            )
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) SecondaryBlue else Color.LightGray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) SecondaryBlue else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (subtitle != null) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.Gray, fontSize = 14.sp)
    }
}

private fun DeliveryMethod.icon(): ImageVector {
    return when (this) {
        DeliveryMethod.DIRECT_MEET -> Icons.Default.Handshake
        DeliveryMethod.BUYER_TO_SELLER -> Icons.Default.Storefront
        DeliveryMethod.SELLER_TO_BUYER -> Icons.Default.Place
        DeliveryMethod.SHIPPING -> Icons.Default.LocalShipping
    }
}
