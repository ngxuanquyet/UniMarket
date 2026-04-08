package com.example.unimarket.presentation.checkout

import android.net.Uri
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
import androidx.compose.material3.HorizontalDivider
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
import com.example.unimarket.domain.model.SellerPaymentMethodType
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
    onBackClick: () -> Unit,
    onTransferCompleted: (Order) -> Unit,
    viewModel: QrTransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val saveQrComingSoonMessage = stringResource(R.string.checkout_qr_save_coming_soon)
    val qrUnavailableMessage = stringResource(R.string.checkout_qr_save_unavailable)

    LaunchedEffect(orderIds) {
        viewModel.loadOrders(orderIds)
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
            }
        }
    }

    val currentOrder = uiState.currentOrder

    LaunchedEffect(currentOrder?.id, currentOrder?.status) {
        val order = currentOrder ?: return@LaunchedEffect
        if (order.status != OrderStatus.WAITING_PAYMENT) return@LaunchedEffect
        val orderId = order.id

        while (true) {
            delay(5_000)
            val latestOrder = viewModel.uiState.value.currentOrder
            if (latestOrder?.id != orderId || latestOrder.status != OrderStatus.WAITING_PAYMENT) {
                break
            }
            viewModel.checkCurrentOrderPayment(showPendingMessage = false)
        }
    }


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

    if (currentOrder == null) {
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

    val paymentMethod = currentOrder.paymentMethodDetails
    val qrUrl = remember(currentOrder.id, currentOrder.transferContent, currentOrder.totalAmount) {
        currentOrder.qrImageUrl()
    }
    val remainingSeconds by rememberRemainingSeconds(currentOrder.paymentExpiresAt)
    val isPendingPayment = currentOrder.status == OrderStatus.WAITING_PAYMENT

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
                    IconButton(onClick = onBackClick) {
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
                        text = if (paymentMethod?.type == SellerPaymentMethodType.BANK_TRANSFER) {
                            "NGUYEN XUAN QUYET"
                        } else {
                            paymentMethod?.accountName.orEmpty().ifBlank {
                                stringResource(R.string.profile_default_user)
                            }
                        },
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
                        text = "#${currentOrder.id.takeLast(8).uppercase()}",
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.checkout_qr_total_amount_label),
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatVnd(currentOrder.totalAmount),
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
                text = when (paymentMethod?.type ?: SellerPaymentMethodType.fromRaw(currentOrder.paymentMethod)) {
                    SellerPaymentMethodType.BANK_TRANSFER ->
                        stringResource(R.string.checkout_qr_step_scan)

                    SellerPaymentMethodType.MOMO ->
                        stringResource(R.string.checkout_qr_step_momo)

                    SellerPaymentMethodType.CASH_ON_DELIVERY ->
                        stringResource(R.string.checkout_qr_step_cash)
                }
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
                        value = localizedPaymentMethodLabel(currentOrder.paymentMethod)
                    )
                    TransferInfoRow(
                        label = stringResource(R.string.payment_methods_account_name),
                        value = paymentMethod?.accountName.orEmpty()
                    )
                    if (paymentMethod?.type == SellerPaymentMethodType.BANK_TRANSFER) {
                        TransferInfoRow(
                            label = stringResource(R.string.payment_methods_bank_name),
                            value = "MBBank (MB)"
                        )
                        TransferInfoRow(
                            label = stringResource(R.string.payment_methods_account_number),
                            value = "0356433860"
                        )
                    } else if (paymentMethod?.type == SellerPaymentMethodType.MOMO) {
                        TransferInfoRow(
                            label = stringResource(R.string.payment_methods_phone_number),
                            value = paymentMethod.phoneNumber
                        )
                    }
                    TransferInfoRow(
                        label = stringResource(R.string.checkout_transfer_content),
                        value = currentOrder.transferContent.ifBlank { "UM${currentOrder.id}" }
                    )
                    if (!paymentMethod?.note.isNullOrBlank()) {
                        HorizontalDivider(color = ProfileAvatarBorder)
                        Text(
                            text = paymentMethod?.note.orEmpty(),
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isPendingPayment) {
                        viewModel.checkCurrentOrderPayment(showPendingMessage = true)
                    } else {
                        viewModel.moveToNextOrder()
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
                        text = when {
                            isPendingPayment -> stringResource(R.string.checkout_qr_button_done)
                            uiState.currentIndex < uiState.orders.lastIndex -> stringResource(R.string.auth_continue)
                            else -> stringResource(R.string.checkout_qr_button_complete)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

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

private fun Order.qrImageUrl(): String? {
    val method = paymentMethodDetails ?: return null
    val amount = totalAmount.roundToInt().coerceAtLeast(0)
    val addInfo = Uri.encode(transferContent.ifBlank { "UM$id" })
    
    if (method.type == SellerPaymentMethodType.BANK_TRANSFER) {
        val accountName = Uri.encode("NGUYEN XUAN QUYET")
        return "https://img.vietqr.io/image/MB-0356433860-compact2.png?amount=$amount&addInfo=$addInfo&accountName=$accountName"
    }
    
    if (!method.supportsQr()) {
        return null
    }
    
    val accountName = Uri.encode(method.accountName)
    return "https://img.vietqr.io/image/${method.bankCode}-${method.accountNumber}-compact2.png?amount=$amount&addInfo=$addInfo&accountName=$accountName"
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
