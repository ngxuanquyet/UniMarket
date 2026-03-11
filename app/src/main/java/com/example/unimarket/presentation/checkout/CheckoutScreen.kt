package com.example.unimarket.presentation.checkout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    productId: String,
    onBackClick: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                Text("Product not found", color = Color.Gray)
            }
        } else {
            val product = uiState.product!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                // Item Summary
                Text(
                    text = "ITEM SUMMARY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$${String.format("%.2f", product.price)}",
                                color = SecondaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delivery Method
                Text(
                    text = "DELIVERY METHOD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                var deliveryMethod by remember { mutableStateOf("Meet in person") }

                DeliveryOptionCard(
                    title = "Meet in person",
                    subtitle = "Free - Coordinate a spot on campus",
                    isSelected = deliveryMethod == "Meet in person",
                    icon = Icons.Default.Groups,
                    onClick = { deliveryMethod = "Meet in person" }
                ) {
                    if (deliveryMethod == "Meet in person") {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text("PICK A MEETING POINT", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "Main Library Entrance",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = SecondaryBlue
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                DeliveryOptionCard(
                    title = "Campus Delivery",
                    subtitle = "+$2.00 - Delivered to your dorm",
                    isSelected = deliveryMethod == "Campus Delivery",
                    icon = Icons.Default.LocalShipping,
                    onClick = { deliveryMethod = "Campus Delivery" }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Payment Method
                Text(
                    text = "PAYMENT METHOD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                var paymentMethod by remember { mutableStateOf("Student Wallet") }

                PaymentOptionCard(
                    title = "Student Wallet",
                    subtitle = "Balance: $152.50",
                    isSelected = paymentMethod == "Student Wallet",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = { paymentMethod = "Student Wallet" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentOptionCard(
                    title = "Cash on delivery",
                    isSelected = paymentMethod == "Cash on delivery",
                    icon = Icons.Default.Money,
                    onClick = { paymentMethod = "Cash on delivery" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentOptionCard(
                    title = "Bank Transfer",
                    isSelected = paymentMethod == "Bank Transfer",
                    icon = Icons.Default.AccountBalance,
                    onClick = { paymentMethod = "Bank Transfer" }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Summary
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    val subtotal = product.price
                    val platformFee = 1.50
                    val deliveryFee = if (deliveryMethod == "Campus Delivery") 2.00 else 0.00
                    val total = subtotal + platformFee + deliveryFee

                    SummaryRow("Subtotal", "$${String.format("%.2f", subtotal)}")
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("Platform Fee", "$${String.format("%.2f", platformFee)}")
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("Delivery", "$${String.format("%.2f", deliveryFee)}")

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "$${String.format("%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = SecondaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Confirm Button
                Button(
                    onClick = { /* Handle actual purchase */ },
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
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp, bottom = 32.dp)
                )
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
