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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    productId: String,
    quantity: Int,
    onBackClick: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SecondaryBlue)
            }
        } else if (uiState.product == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Product not found", color = Color.Gray)
            }
        } else {
            val product = requireNotNull(uiState.product)
            val deliveryFee = if (uiState.selectedDeliveryMethod == DeliveryMethod.SHIPPING) 30000.0 else 0.0
            val platformFee = 1500.0
            val subtotal = product.price * quantity
            val total = subtotal + platformFee + deliveryFee

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                SectionTitle("ITEM SUMMARY")

                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = product.imageUrls.firstOrNull()),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProfileAvatarBorder)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Condition: ${product.condition}", color = Color.Gray, fontSize = 12.sp)
                            Text("Quantity: $quantity", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatVnd(product.price),
                                color = SecondaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("DELIVERY METHOD")

                if (uiState.availableDeliveryMethods.isEmpty()) {
                    Text(
                        text = "Nguoi ban chua thiet lap phuong thuc giao nhan.",
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    uiState.availableDeliveryMethods.forEach { method ->
                        DeliveryOptionCard(
                            title = method.title,
                            subtitle = method.subtitle,
                            isSelected = uiState.selectedDeliveryMethod == method,
                            icon = method.icon(),
                            onClick = { viewModel.selectDeliveryMethod(method) }
                        ) {
                            if (uiState.selectedDeliveryMethod == method) {
                                DeliveryMethodDetails(
                                    method = method,
                                    buyerAddresses = uiState.buyerAddresses,
                                    sellerAddresses = uiState.sellerAddresses,
                                    selectedBuyerAddressId = uiState.selectedBuyerAddressId,
                                    selectedSellerAddressId = uiState.selectedSellerAddressId,
                                    meetingPoint = uiState.meetingPoint,
                                    onBuyerAddressClick = viewModel::selectBuyerAddress,
                                    onSellerAddressClick = viewModel::selectSellerAddress,
                                    onMeetingPointChange = viewModel::updateMeetingPoint
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("PAYMENT METHOD")

                PaymentOptionCard(
                    title = "Cash on delivery",
                    isSelected = uiState.paymentMethod == "Cash on delivery",
                    icon = Icons.Default.Money,
                    onClick = { viewModel.selectPaymentMethod("Cash on delivery") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentOptionCard(
                    title = "Bank Transfer",
                    isSelected = uiState.paymentMethod == "Bank Transfer",
                    icon = Icons.Default.AccountBalance,
                    onClick = { viewModel.selectPaymentMethod("Bank Transfer") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SummaryRow("Subtotal", formatVnd(subtotal))
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("Platform Fee", formatVnd(platformFee))
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("Delivery", formatVnd(deliveryFee))

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            formatVnd(total),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = SecondaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Purchase", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
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
                label = { Text("Diem gap truc tiep") },
                placeholder = { Text("VD: Cong truong, canteen, san truong...") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = SecondaryBlue
                )
            )
        }

        DeliveryMethod.BUYER_TO_SELLER -> {
            AddressSelector(
                title = "Dia chi nguoi ban",
                addresses = sellerAddresses,
                selectedAddressId = selectedSellerAddressId,
                emptyState = "Nguoi ban chua co dia chi de ban den nhan hang.",
                onAddressClick = onSellerAddressClick
            )
        }

        DeliveryMethod.SELLER_TO_BUYER -> {
            AddressSelector(
                title = "Dia chi nguoi mua",
                addresses = buyerAddresses,
                selectedAddressId = selectedBuyerAddressId,
                emptyState = "Ban chua co dia chi. Hay them dia chi trong Profile.",
                onAddressClick = onBuyerAddressClick
            )
        }

        DeliveryMethod.SHIPPING -> {
            AddressSelector(
                title = "Dia chi nhan hang",
                addresses = buyerAddresses,
                selectedAddressId = selectedBuyerAddressId,
                emptyState = "Ban chua co dia chi giao hang. Hay them dia chi trong Profile.",
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
                                append("Dia chi")
                                if (address.isDefault) append(" • Mac dinh")
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
fun DeliveryOptionCard(
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
            .padding(horizontal = 16.dp)
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
                Icon(icon, contentDescription = null, tint = if (isSelected) SecondaryBlue else Color.Gray, modifier = Modifier.size(24.dp))
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
fun PaymentOptionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
            Icon(icon, contentDescription = null, tint = if (isSelected) SecondaryBlue else Color.Gray, modifier = Modifier.size(24.dp))
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
fun SummaryRow(label: String, value: String) {
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
