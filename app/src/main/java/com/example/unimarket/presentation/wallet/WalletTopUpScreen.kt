package com.example.unimarket.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletTopUpScreen(
    currentBalance: Double = 0.0,
    onBackClick: () -> Unit,
    onTopUpClick: (Long) -> Unit = {}
) {
    val presetAmounts = remember { listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L) }
    var selectedPresetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var customAmountInput by rememberSaveable { mutableStateOf("") }
    var selectedSourceIndex by rememberSaveable { mutableIntStateOf(0) }
    val customAmountValue = customAmountInput.toLongOrNull()
    val hasCustomAmount = customAmountInput.isNotBlank()
    val effectiveAmount = if (hasCustomAmount) {
        customAmountValue
    } else {
        selectedPresetIndex?.let { presetAmounts.getOrNull(it) }
    }
    val isAmountValid = effectiveAmount != null && effectiveAmount in MIN_TOP_UP_AMOUNT..MAX_TOP_UP_AMOUNT
    val showInvalidCustomAmount = hasCustomAmount && !isAmountValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wallet_top_up_title),
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF1FB), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.wallet_top_up_current_balance).uppercase(),
                        color = Color(0xFF7A81A8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatVnd(currentBalance),
                        color = SecondaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.wallet_top_up_amount),
                    color = TextDarkBlack,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.wallet_top_up_promo),
                    color = Color(0xFFB7399B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Color(0xFFF7D6F1), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presetAmounts.chunked(3).forEach { rowValues ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowValues.forEach { value ->
                            val index = presetAmounts.indexOf(value)
                            val selected = selectedPresetIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) SecondaryBlue else SurfaceWhite,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) SecondaryBlue else Color(0xFFE6E8F2),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        customAmountInput = value.toString()
                                        selectedPresetIndex = if (selected) null else index
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatVnd(value.toDouble()),
                                    color = if (selected) Color.White else Color(0xFF66709A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.wallet_top_up_custom_amount),
                color = TextDarkBlack,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = customAmountInput,
                onValueChange = { input ->
                    customAmountInput = input.filter(Char::isDigit).take(9)
                    selectedPresetIndex = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.wallet_top_up_enter_amount),
                        color = Color(0xFFB0B5CE)
                    )
                },
                leadingIcon = {
                    Text(
                        text = "₫",
                        color = Color(0xFF8087AB),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E5F3),
                    focusedBorderColor = SecondaryBlue
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (showInvalidCustomAmount) {
                Text(
                    text = localizedText(
                        english = "Top-up amount must be between 5,000 and 100,000,000 VND.",
                        vietnamese = "Số tiền nạp phải từ 5.000 đến 100.000.000 VNĐ."
                    ),
                    color = Color(0xFFD93025),
                    fontSize = 12.sp
                )
            }

            Text(
                text = stringResource(R.string.wallet_top_up_payment_source),
                color = TextDarkBlack,
                fontWeight = FontWeight.SemiBold
            )

            PaymentSourceCard(
                title = "Chase Bank",
                subtitle = "Checking •••• 8821",
                icon = Icons.Default.AccountBalance,
                selected = selectedSourceIndex == 0,
                onClick = { selectedSourceIndex = 0 }
            )

            PaymentSourceCard(
                title = "Mastercard Gold",
                subtitle = "Credit •••• 4590",
                icon = Icons.Default.CreditCard,
                selected = selectedSourceIndex == 1,
                onClick = { selectedSourceIndex = 1 }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFD7DBED), RoundedCornerShape(12.dp))
                    .clickable { }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SecondaryBlue
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.wallet_top_up_add_method),
                        color = SecondaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.wallet_top_up_security_note),
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Button(
                onClick = {
                    effectiveAmount?.let(onTopUpClick)
                },
                enabled = isAmountValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryBlue,
                    disabledContainerColor = Color(0xFFB5BCDA)
                )
            ) {
                Text(
                    text = stringResource(R.string.wallet_top_up_now),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

private const val MIN_TOP_UP_AMOUNT = 5_000L
private const val MAX_TOP_UP_AMOUNT = 100_000_000L

@Composable
private fun PaymentSourceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite, RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = if (selected) SecondaryBlue else Color(0xFFE2E5F3),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF1F3FB), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF6A7298))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextDarkBlack, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextGray, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(
                        width = 2.dp,
                        color = if (selected) SecondaryBlue else Color(0xFFC7CCE3),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .background(
                        color = if (selected) SecondaryBlue else Color.Transparent,
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}
