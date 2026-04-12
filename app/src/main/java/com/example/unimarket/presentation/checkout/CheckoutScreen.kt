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
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.DividerColor
import com.example.unimarket.presentation.theme.ProfileAvatarBorder
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedConditionLabel
import com.example.unimarket.presentation.util.localizedPaymentMethodLabel
import com.example.unimarket.presentation.util.localizedSubtitle
import com.example.unimarket.presentation.util.localizedTitle

private data class CompletedOrderResult(
    val orderIds: List<String>,
    val requestedCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    productId: String? = null,
    quantity: Int = 1,
    cartItemIds: List<String> = emptyList(),
    onBackClick: () -> Unit,
    onTransferOrdersReady: (List<String>) -> Unit = {},
    onPurchaseCompleted: () -> Unit = {},
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var completedOrderInfo by remember { mutableStateOf<CompletedOrderResult?>(null) }

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
                    if (event.transferOrderIds.isNotEmpty()) {
                        onTransferOrdersReady(event.transferOrderIds)
                    } else {
                        completedOrderInfo = CompletedOrderResult(
                            orderIds = event.orderIds,
                            requestedCount = event.requestedCount
                        )
                    }
                }
            }
        }
    }

    completedOrderInfo?.let { completed ->
        PurchaseCompletedDialog(
            completedCount = completed.orderIds.size,
            requestedCount = completed.requestedCount,
            onDismiss = {
                completedOrderInfo = null
                onPurchaseCompleted()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
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
                    Text(
                        uiState.errorMessage ?: stringResource(R.string.checkout_no_order_found),
                        color = Color.Gray
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    SectionTitle(stringResource(R.string.checkout_order_details))

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
                            onSelectPaymentMethod = { optionId ->
                                viewModel.selectPaymentMethod(order.id, optionId)
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
                                stringResource(R.string.checkout_processing),
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
                                stringResource(R.string.checkout_confirm_orders, uiState.orders.size),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.checkout_secure_campus_transaction),
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
private fun PurchaseCompletedDialog(
    completedCount: Int,
    requestedCount: Int,
    onDismiss: () -> Unit
) {
    val isFullSuccess = completedCount == requestedCount

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                if (isFullSuccess) {
                    stringResource(R.string.checkout_purchase_confirmed)
                } else {
                    stringResource(R.string.checkout_purchase_partial)
                }
            )
        },
        text = {
            Text(
                if (isFullSuccess) {
                    stringResource(R.string.checkout_purchase_confirmed_message, completedCount)
                } else {
                    stringResource(
                        R.string.checkout_purchase_partial_message,
                        completedCount,
                        requestedCount
                    )
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.auth_continue))
            }
        }
    )
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
                text = stringResource(R.string.checkout_order_index, orderIndex),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OrderItemSummary(order = order)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                stringResource(R.string.checkout_delivery_method),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (order.availableDeliveryMethods.isEmpty()) {
                Text(
                    text = stringResource(R.string.checkout_seller_no_delivery),
                    color = Color.Gray
                )
            } else {
                order.availableDeliveryMethods.forEach { method ->
                    DeliveryOptionCard(
                        title = method.localizedTitle(),
                        subtitle = method.localizedSubtitle(),
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

            Text(
                stringResource(R.string.checkout_payment_method),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            order.availablePaymentOptions.forEach { option ->
                PaymentOptionCard(
                    title = option.localizedTitle(),
                    subtitle = option.localizedSubtitle(),
                    isSelected = order.selectedPaymentOptionId == option.id,
                    icon = option.icon(),
                    onClick = { onSelectPaymentMethod(option.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                stringResource(R.string.checkout_order_total),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(stringResource(R.string.checkout_subtotal), formatVnd(order.subtotal))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(stringResource(R.string.checkout_platform_fee), formatVnd(order.platformFee))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(stringResource(R.string.checkout_delivery_fee), formatVnd(order.deliveryFee))

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.checkout_total), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            Text(
                stringResource(
                    R.string.checkout_condition_value,
                    localizedConditionLabel(order.product.condition)
                ),
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                stringResource(R.string.checkout_quantity_value, order.quantity),
                color = Color.Gray,
                fontSize = 12.sp
            )
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
                text = stringResource(R.string.checkout_overall_summary),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(stringResource(R.string.checkout_orders), uiState.orders.size.toString())
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(stringResource(R.string.checkout_items_subtotal), formatVnd(uiState.grandSubtotal))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(stringResource(R.string.checkout_platform_fees), formatVnd(uiState.grandPlatformFee))
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(stringResource(R.string.checkout_delivery_fees), formatVnd(uiState.grandDeliveryFee))

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.checkout_grand_total), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                label = { Text(stringResource(R.string.checkout_meeting_point)) },
                placeholder = { Text(stringResource(R.string.checkout_meeting_point_placeholder)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = SecondaryBlue
                )
            )
        }

        DeliveryMethod.BUYER_TO_SELLER -> {
            AddressSelector(
                title = stringResource(R.string.checkout_seller_address),
                addresses = sellerAddresses,
                selectedAddressId = selectedSellerAddressId,
                emptyState = stringResource(R.string.checkout_seller_address_empty),
                onAddressClick = onSellerAddressClick
            )
        }

        DeliveryMethod.SELLER_TO_BUYER -> {
            AddressSelector(
                title = stringResource(R.string.checkout_buyer_address),
                addresses = buyerAddresses,
                selectedAddressId = selectedBuyerAddressId,
                emptyState = stringResource(R.string.checkout_buyer_address_empty),
                onAddressClick = onBuyerAddressClick
            )
        }

        DeliveryMethod.SHIPPING -> {
            AddressSelector(
                title = stringResource(R.string.checkout_shipping_address),
                addresses = buyerAddresses,
                selectedAddressId = selectedBuyerAddressId,
                emptyState = stringResource(R.string.checkout_shipping_address_empty),
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
                        imageVector = if (selectedAddressId == address.id) {
                            Icons.Default.RadioButtonChecked
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        tint = if (selectedAddressId == address.id) SecondaryBlue else Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildString {
                                append(stringResource(R.string.profile_my_addresses))
                                if (address.isDefault) append(" • ${stringResource(R.string.common_default)}")
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(address.recipientName, style = MaterialTheme.typography.bodySmall)
                        if (address.phoneNumber.isNotBlank()) {
                            Text(
                                address.phoneNumber,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            address.shortDisplay(),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
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
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SecondaryBlue else Color.LightGray,
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
                if (!subtitle.isNullOrBlank()) {
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

@Composable
private fun CheckoutPaymentOption.localizedTitle(): String {
    if (id == "transfer") {
        return localizedPaymentMethodLabel("BANK_TRANSFER")
    }
    if (id == "wallet") {
        return localizedPaymentMethodLabel("WALLET")
    }
    if (id == "cash_on_delivery") {
        return localizedPaymentMethodLabel("CASH_ON_DELIVERY")
    }

    return sellerMethod?.displayTitle?.ifBlank { null }
        ?: localizedPaymentMethodLabel(paymentMethodCode)
}

private fun CheckoutPaymentOption.localizedSubtitle(): String? {
    return when (type) {
        SellerPaymentMethodType.CASH_ON_DELIVERY -> null
        SellerPaymentMethodType.BANK_TRANSFER -> sellerMethod?.shortSubtitle
        SellerPaymentMethodType.MOMO -> sellerMethod?.shortSubtitle
        SellerPaymentMethodType.WALLET -> null
    }
}

private fun CheckoutPaymentOption.icon(): ImageVector {
    return when (type) {
        SellerPaymentMethodType.CASH_ON_DELIVERY -> Icons.Default.Money
        SellerPaymentMethodType.BANK_TRANSFER -> Icons.Default.AccountBalance
        SellerPaymentMethodType.MOMO -> Icons.Default.PhoneAndroid
        SellerPaymentMethodType.WALLET -> Icons.Default.AccountBalance
    }
}
