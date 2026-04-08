package com.example.unimarket.presentation.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.util.formatVnd

@Composable
fun PaymentSuccessScreen(
    orderId: String,
    productName: String,
    quantity: Int,
    totalAmount: Double,
    onTrackOrderClick: () -> Unit,
    onBackToMarketplaceClick: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.payment_sucess))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie Animation
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .size(280.dp)
                    .scale(1.4f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Payment Successful!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your transaction was completed securely and your order is being processed.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Order Details Card
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(20.dp))
//                    .background(Color(0xFFF3F2FF))
//                    .padding(20.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                DetailRow(label = "Order Number", value = "#${orderId.takeLast(8).uppercase()}")
//                DetailRow(label = "Product Name", value = productName)
//                DetailRow(label = "Quantity", value = quantity.toString())
//                DetailRow(
//                    label = "Total Amount",
//                    value = formatVnd(totalAmount),
//                    valueColor = SecondaryBlue,
//                    valueWeight = FontWeight.Bold
//                )
//            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = onTrackOrderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
            ) {
                Text(
                    text = "Track My Order",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onBackToMarketplaceClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to Marketplace",
                    color = SecondaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1C1F39),
    valueWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF5C627A),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = valueWeight,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

@Preview
@Composable
fun PaymentSuccessScreenPre() {
    PaymentSuccessScreen(
        orderId = "UM12345678",
        productName = "abcxyz",
        quantity = 2,
        totalAmount = 100000.0,
        onTrackOrderClick = {},
        onBackToMarketplaceClick = {}
    )
}
