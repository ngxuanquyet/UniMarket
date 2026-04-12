package com.example.unimarket.presentation.checkout

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.R
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.presentation.theme.ProfileAvatarBorder
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedPaymentMethodLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrTransferScreen(
    orderIds: List<String>,
    topUpAmount: Long = 0L,
    onBackClick: () -> Unit,
    onTransferCompleted: (Order) -> Unit,
    onTopUpCompleted: () -> Unit = {},
    viewModel: QrTransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val saveQrComingSoonMessage = stringResource(R.string.checkout_qr_save_coming_soon)
    val qrUnavailableMessage = stringResource(R.string.checkout_qr_save_unavailable)
    var showCancelDialog by remember { mutableStateOf(false) }
    var showExpiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderIds, topUpAmount) {
        if (topUpAmount > 0L && orderIds.isEmpty()) {
            viewModel.startTopUpFlow(topUpAmount)
        } else {
            viewModel.loadOrders(orderIds)
        }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is QrTransferViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is QrTransferViewModel.UiEvent.PaymentConfirmed -> {
                    viewModel.onPaymentSuccessHandled(event.orderId)
                }

                is QrTransferViewModel.UiEvent.AllTransfersCompleted -> {
                    onTransferCompleted(event.order)
                }

                QrTransferViewModel.UiEvent.ExitAfterCancel -> {
                    onBackClick()
                }

                QrTransferViewModel.UiEvent.TopUpCompleted -> {
                    onTopUpCompleted()
                }
            }
        }
    }

    val currentOrder = uiState.currentOrder
    val isTopUpMode = uiState.isTopUpMode
    val resolvedAmount = if (isTopUpMode) uiState.topUpAmount.toDouble() else (currentOrder?.totalAmount ?: 0.0)
    val resolvedTransferContent = if (isTopUpMode) uiState.topUpTransferContent else currentOrder?.transferContent.orEmpty()
    val isPendingPayment = if (isTopUpMode) !uiState.isTopUpCompleted else currentOrder?.status == OrderStatus.WAITING_PAYMENT

    if (uiState.isLoading) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.checkout_qr_transfer_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SecondaryBlue)
            }
        }
        return
    }

    if (!isTopUpMode && currentOrder == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.checkout_qr_transfer_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.checkout_qr_transfer_empty),
                    color = Color.Gray
                )
            }
        }
        return
    }

    val qrUrl = remember(currentOrder?.id, resolvedTransferContent, resolvedAmount) {
        qrImageUrl(
            amount = resolvedAmount,
            transferContent = resolvedTransferContent,
            fallbackOrderId = currentOrder?.id.orEmpty()
        )
    }
    val remainingSeconds by rememberRemainingSeconds(currentOrder?.paymentExpiresAt ?: 0L)
    val shouldConfirmCancel = isPendingPayment

    LaunchedEffect(currentOrder?.id) {
        showExpiredDialog = false
    }

    LaunchedEffect(isTopUpMode, isPendingPayment, remainingSeconds, currentOrder?.id) {
        if (!isTopUpMode && isPendingPayment && remainingSeconds <= 0) {
            showCancelDialog = false
            showExpiredDialog = true
        }
    }

    BackHandler(enabled = showExpiredDialog) {}

    BackHandler(enabled = true) {
        if (shouldConfirmCancel) {
            showCancelDialog = true
        } else {
            onBackClick()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.checkout_qr_transfer_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (shouldConfirmCancel) {
                                if (showExpiredDialog) return@IconButton
                                showCancelDialog = true
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F5FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.checkout_qr_recipient_label),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = APP_TRANSFER_ACCOUNT_NAME,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color(0xFF1C1F39)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.checkout_qr_verified_seller),
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.checkout_qr_order_label),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTopUpMode) {
                            "#${uiState.topUpTransferContent.takeLast(8).uppercase()}"
                        } else {
                            "#${currentOrder?.id?.takeLast(8)?.uppercase().orEmpty()}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = SecondaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.checkout_qr_total_amount_label),
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatVnd(resolvedAmount),
                        color = SecondaryBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, ProfileAvatarBorder, RoundedCornerShape(20.dp))
                            .background(Color(0xFFFDFDFF))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = qrUrl),
                                contentDescription = stringResource(R.string.checkout_payment_qr_content_description),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE07B7B),
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.checkout_payment_qr_unavailable),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (qrUrl != null) {
                                        saveQrComingSoonMessage
                                    } else {
                                        qrUnavailableMessage
                                    }
                                )
                            }
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = SecondaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.checkout_qr_button_save),
                            color = SecondaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isPendingPayment && remainingSeconds > 0) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFEEF0))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE55B68))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.checkout_qr_expires_in,
                                    remainingSeconds.asClockText()
                                ),
                                color = Color(0xFFE55B68),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            InstructionStep(
                number = 1,
                text = stringResource(R.string.checkout_qr_step_scan)
            )
            InstructionStep(
                number = 2,
                text = if (isPendingPayment) {
                    stringResource(R.string.checkout_qr_step_auto_check)
                } else {
                    stringResource(R.string.checkout_qr_step_escrow)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TransferInfoRow(
                        label = stringResource(R.string.checkout_payment_method),
                        value = if (isTopUpMode) {
                            localizedPaymentMethodLabel("BANK_TRANSFER")
                        } else {
                            localizedPaymentMethodLabel(currentOrder?.paymentMethod.orEmpty())
                        }
                    )
                    TransferInfoRow(
                        label = stringResource(R.string.payment_methods_account_name),
                        value = APP_TRANSFER_ACCOUNT_NAME
                    )
                    TransferInfoRow(
                        label = stringResource(R.string.payment_methods_bank_name),
                        value = APP_TRANSFER_BANK_NAME
                    )
                    TransferInfoRow(
                        label = stringResource(R.string.payment_methods_account_number),
                        value = APP_TRANSFER_ACCOUNT_NUMBER
                    )
                    TransferInfoRow(
                        label = stringResource(R.string.checkout_transfer_content),
                        value = resolvedTransferContent.ifBlank {
                            if (isTopUpMode) {
                                uiState.topUpTransferContent
                            } else {
                                "UM${currentOrder?.id.orEmpty()}"
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isPendingPayment) {
                Button(
                    onClick = {
                        if (isTopUpMode) {
                            viewModel.checkTopUpPayment(showPendingMessage = true)
                        } else {
                            viewModel.checkCurrentOrderPayment(showPendingMessage = true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    enabled = !uiState.isCheckingPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                ) {
                    if (uiState.isCheckingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.checkout_payment_checking),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.checkout_qr_button_done),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Button(
                    onClick = { viewModel.moveToNextOrder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    enabled = !uiState.isCheckingPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                ) {
                    if (uiState.isCheckingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.checkout_payment_checking),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (uiState.currentIndex < uiState.orders.lastIndex) {
                                stringResource(R.string.auth_continue)
                            } else {
                                stringResource(R.string.checkout_qr_button_complete)
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = if (isPendingPayment) {
                    stringResource(R.string.checkout_qr_support_payment_waiting)
                } else {
                    stringResource(R.string.checkout_qr_support)
                },
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isCancellingPayment) {
                    showCancelDialog = false
                }
            },
            title = { Text(stringResource(R.string.mypurchases_cancel_payment_title)) },
            text = { Text(stringResource(R.string.mypurchases_cancel_payment_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelPaymentAndExit()
                    },
                    enabled = !uiState.isCancellingPayment
                ) {
                    if (uiState.isCancellingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(stringResource(R.string.mypurchases_cancel_payment_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    enabled = !uiState.isCancellingPayment
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showExpiredDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.checkout_qr_expired_title)) },
            text = { Text(stringResource(R.string.checkout_qr_expired_message)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelPaymentAndExit() },
                    enabled = !uiState.isCancellingPayment
                ) {
                    if (uiState.isCancellingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(stringResource(R.string.checkout_qr_expired_confirm))
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFE9EEFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = SecondaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color(0xFF5C627A),
            lineHeight = 20.sp
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun TransferInfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color(0xFF1C1F39),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun rememberRemainingSeconds(paymentExpiresAt: Long): androidx.compose.runtime.State<Int> {
    val remainingState = remember(paymentExpiresAt) { mutableStateOf(paymentExpiresAt.remainingSeconds()) }

    LaunchedEffect(paymentExpiresAt) {
        while (remainingState.value > 0) {
            delay(1_000)
            remainingState.value = paymentExpiresAt.remainingSeconds()
        }
    }

    return remainingState
}

private fun qrImageUrl(
    amount: Double,
    transferContent: String,
    fallbackOrderId: String
): String? {
    val resolvedAmount = amount.roundToInt().coerceAtLeast(0)
    val addInfo = Uri.encode(transferContent.ifBlank { "UM$fallbackOrderId" })
    val accountName = Uri.encode(APP_TRANSFER_ACCOUNT_NAME)
    return "https://img.vietqr.io/image/$APP_TRANSFER_BANK_CODE-$APP_TRANSFER_ACCOUNT_NUMBER-compact2.png?amount=$resolvedAmount&addInfo=$addInfo&accountName=$accountName"
}

private fun Long.remainingSeconds(): Int {
    if (this <= 0L) return 0
    return ((this - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L).toInt()
}

private fun Int.asClockText(): String {
    val safe = coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%02d:%02d".format(minutes, seconds)
}

private const val APP_TRANSFER_BANK_CODE = "MB"
private const val APP_TRANSFER_BANK_NAME = "MBBank"
private const val APP_TRANSFER_ACCOUNT_NUMBER = "0356433860"
private const val APP_TRANSFER_ACCOUNT_NAME = "NGUYEN XUAN QUYET"
