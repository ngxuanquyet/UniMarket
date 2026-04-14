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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimarket.R
import com.example.unimarket.domain.model.SellerPaymentMethodType
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
    mode: WalletTransactionMode = WalletTransactionMode.TOP_UP,
    onBackClick: () -> Unit,
    onTopUpClick: (Long) -> Unit = {},
    onWithdrawSubmitted: (Long) -> Unit = {},
    onOpenPaymentMethods: () -> Unit = {},
    withdrawViewModel: WalletWithdrawViewModel = hiltViewModel()
) {
    val presetAmounts = remember { listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L) }
    var selectedPresetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var customAmountInput by rememberSaveable { mutableStateOf("") }
    val customAmountValue = customAmountInput.toLongOrNull()
    val hasCustomAmount = customAmountInput.isNotBlank()
    val effectiveAmount = if (hasCustomAmount) {
        customAmountValue
    } else {
        selectedPresetIndex?.let { presetAmounts.getOrNull(it) }
    }
    val withdrawUiState by withdrawViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isAmountValid = effectiveAmount != null && effectiveAmount in MIN_TRANSACTION_AMOUNT..MAX_TRANSACTION_AMOUNT
    val hasEnoughBalance = mode != WalletTransactionMode.WITHDRAW || (effectiveAmount ?: 0L) <= currentBalance.toLong()
    val hasSelectedReceiverMethod =
        mode != WalletTransactionMode.WITHDRAW || !withdrawUiState.selectedMethodId.isNullOrBlank()
    val showInvalidCustomAmount = hasCustomAmount && !isAmountValid
    val showInsufficientBalance = mode == WalletTransactionMode.WITHDRAW && effectiveAmount != null && !hasEnoughBalance
    val titleText = if (mode == WalletTransactionMode.WITHDRAW) {
        stringResource(R.string.wallet_withdraw_title)
    } else {
        stringResource(R.string.wallet_top_up_title)
    }
    val amountLabelText = if (mode == WalletTransactionMode.WITHDRAW) {
        stringResource(R.string.wallet_withdraw_amount)
    } else {
        stringResource(R.string.wallet_top_up_amount)
    }
    val actionButtonText = if (mode == WalletTransactionMode.WITHDRAW) {
        stringResource(R.string.wallet_withdraw_now)
    } else {
        stringResource(R.string.wallet_top_up_now)
    }
    val submittingText = stringResource(R.string.common_processing)
    val withdrawRequestSentText = stringResource(R.string.wallet_withdraw_request_sent)
    var showWithdrawSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var latestSuccessAmount by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(withdrawUiState.successRequestId, mode) {
        if (mode == WalletTransactionMode.WITHDRAW && !withdrawUiState.successRequestId.isNullOrBlank()) {
            latestSuccessAmount = withdrawUiState.successAmount ?: 0L
            withdrawViewModel.consumeSuccess()
            showWithdrawSuccessDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
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
                    text = amountLabelText,
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
                        english = "Amount must be between 5,000 and 100,000,000 VND.",
                        vietnamese = "Số tiền phải từ 5.000 đến 100.000.000 VNĐ."
                    ),
                    color = Color(0xFFD93025),
                    fontSize = 12.sp
                )
            }
            if (showInsufficientBalance) {
                Text(
                    text = stringResource(R.string.wallet_withdraw_insufficient_balance),
                    color = Color(0xFFD93025),
                    fontSize = 12.sp
                )
            }

            if (mode == WalletTransactionMode.WITHDRAW) {
                Text(
                    text = stringResource(R.string.wallet_top_up_payment_source),
                    color = TextDarkBlack,
                    fontWeight = FontWeight.SemiBold
                )
                if (withdrawUiState.isLoadingMethods) {
                    Text(
                        text = localizedText(
                            english = "Loading payment methods...",
                            vietnamese = "Đang tải tài khoản nhận tiền..."
                        ),
                        color = TextGray,
                        fontSize = 12.sp
                    )
                } else {
                    val withdrawMethods = withdrawUiState.methods.filter {
                        it.type == SellerPaymentMethodType.BANK_TRANSFER || it.type == SellerPaymentMethodType.MOMO
                    }
                    if (withdrawMethods.isEmpty()) {
                        Text(
                            text = localizedText(
                                english = "No receiving account found. Please add one.",
                                vietnamese = "Chưa có tài khoản nhận tiền. Vui lòng thêm phương thức thanh toán."
                            ),
                            color = Color(0xFFD93025),
                            fontSize = 12.sp
                        )
                    } else {
                        withdrawMethods.forEach { method ->
                            PaymentSourceCard(
                                title = method.displayTitle,
                                subtitle = method.shortSubtitle,
                                icon = when (method.type) {
                                    SellerPaymentMethodType.BANK_TRANSFER -> Icons.Default.AccountBalance
                                    SellerPaymentMethodType.MOMO -> Icons.Default.PhoneAndroid
                                    SellerPaymentMethodType.CASH_ON_DELIVERY -> Icons.Default.CreditCard
                                    SellerPaymentMethodType.WALLET -> Icons.Default.AccountBalance
                                },
                                selected = withdrawUiState.selectedMethodId == method.id,
                                onClick = { withdrawViewModel.selectMethod(method.id) }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD7DBED), RoundedCornerShape(12.dp))
                        .clickable { onOpenPaymentMethods() }
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
                    effectiveAmount?.let { amount ->
                        if (mode == WalletTransactionMode.WITHDRAW) {
                            withdrawViewModel.submitWithdrawal(amount)
                        } else {
                            onTopUpClick(amount)
                        }
                    }
                },
                enabled = isAmountValid && hasEnoughBalance && hasSelectedReceiverMethod && !withdrawUiState.isSubmitting,
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
                    text = if (mode == WalletTransactionMode.WITHDRAW && withdrawUiState.isSubmitting) {
                        submittingText
                    } else {
                        actionButtonText
                    },
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
            if (mode == WalletTransactionMode.WITHDRAW && !withdrawUiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = withdrawUiState.errorMessage.orEmpty(),
                    color = Color(0xFFD93025),
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showWithdrawSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = localizedText(
                        english = "Request submitted",
                        vietnamese = "Gửi yêu cầu thành công"
                    )
                )
            },
            text = {
                Text(withdrawRequestSentText)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWithdrawSuccessDialog = false
                        onWithdrawSubmitted(latestSuccessAmount)
                    }
                ) {
                    Text(text = stringResource(R.string.auth_continue))
                }
            }
        )
    }
}

enum class WalletTransactionMode(val routeValue: String) {
    TOP_UP("top_up"),
    WITHDRAW("withdraw");

    companion object {
        fun fromRoute(raw: String?): WalletTransactionMode {
            return entries.firstOrNull { it.routeValue.equals(raw, ignoreCase = true) } ?: TOP_UP
        }
    }
}

private const val MIN_TRANSACTION_AMOUNT = 5_000L
private const val MAX_TRANSACTION_AMOUNT = 100_000_000L

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
