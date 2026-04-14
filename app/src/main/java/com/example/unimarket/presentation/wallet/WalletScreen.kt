package com.example.unimarket.presentation.wallet

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import com.example.unimarket.presentation.util.formatVnd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit,
    topUpAmount: Long = 0L,
    topUpAt: Long = 0L,
    withdrawAmount: Long = 0L,
    withdrawAt: Long = 0L,
    onTopUpClick: (Double) -> Unit = {},
    onWithdrawClick: (Double) -> Unit = {},
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAllTransactions by rememberSaveable { mutableStateOf(false) }
    val displayedTransactions = if (showAllTransactions) {
        uiState.transactions
    } else {
        uiState.transactions.take(20)
    }
    val canExpandTransactions = uiState.transactions.size > 20

    LaunchedEffect(topUpAmount, topUpAt) {
        if (topUpAmount > 0L && topUpAt > 0L) {
            viewModel.onTopUpCompleted(amount = topUpAmount, timestamp = topUpAt)
        }
    }
    LaunchedEffect(withdrawAmount, withdrawAt) {
        if (withdrawAmount > 0L && withdrawAt > 0L) {
            viewModel.onWithdrawalRequested(amount = withdrawAmount, timestamp = withdrawAt)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wallet_title),
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
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh(showLoading = false) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SecondaryBlue)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            WalletBalanceCard(
                                balance = uiState.walletBalance,
                                onTopUpClick = onTopUpClick,
                                onWithdrawClick = onWithdrawClick
                            )
                        }
                        item {
                            WalletInsightsCard(
                                earned = uiState.monthlyEarned,
                                spent = uiState.monthlySpent
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.wallet_recent_transactions),
                                    color = TextDarkBlack,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (showAllTransactions) {
                                        stringResource(R.string.wallet_show_less).uppercase()
                                    } else {
                                        stringResource(R.string.wallet_view_all).uppercase()
                                    },
                                    color = if (canExpandTransactions) SecondaryBlue else TextGray,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable(enabled = canExpandTransactions) {
                                        showAllTransactions = !showAllTransactions
                                    }
                                )
                            }
                        }

                        if (displayedTransactions.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.wallet_no_transactions),
                                    color = TextGray
                                )
                            }
                        } else {
                            items(displayedTransactions, key = { it.id }) { transaction ->
                                WalletTransactionItem(transaction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletBalanceCard(
    balance: Double,
    onTopUpClick: (Double) -> Unit,
    onWithdrawClick: (Double) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.wallet_total_balance).uppercase(),
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatVnd(balance),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onTopUpClick(balance) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = SecondaryBlue
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = stringResource(R.string.wallet_add_money))
                }
                Button(
                    onClick = { onWithdrawClick(balance) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = stringResource(R.string.wallet_withdraw))
                }
            }
        }
    }
}

@Composable
private fun WalletInsightsCard(earned: Double, spent: Double) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.wallet_monthly_insights),
                color = TextDarkBlack,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.wallet_earned).uppercase(),
                        color = Color(0xFF1E8E5A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatVnd(earned),
                        color = Color(0xFF1E8E5A),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.wallet_spent).uppercase(),
                        color = Color(0xFFD93025),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatVnd(spent),
                        color = Color(0xFFD93025),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletTransactionItem(transaction: WalletTransaction) {
    val title = when (transaction.kind) {
        WalletTransactionKind.TOP_UP,
        WalletTransactionKind.WITHDRAW -> transaction.title
        WalletTransactionKind.ORDER_SALE -> stringResource(R.string.wallet_sold_prefix, transaction.title)
        WalletTransactionKind.ORDER_PURCHASE -> stringResource(R.string.wallet_bought_prefix, transaction.title)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (transaction.isIncoming) {
                            Icons.Default.Payments
                        } else {
                            Icons.Default.ArrowCircleUp
                        },
                        contentDescription = null,
                        tint = if (transaction.isIncoming) Color(0xFF1E8E5A) else Color(0xFFD93025)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(
                            text = title,
                            color = TextDarkBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = transaction.timestamp.formatWalletTime(),
                            color = TextGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (transaction.isIncoming) "+ " else "- ") + formatVnd(transaction.amount),
                        color = if (transaction.isIncoming) Color(0xFF1E8E5A) else Color(0xFFD93025),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.statusLabel.ifBlank {
                            if (transaction.isSuccessful) {
                                stringResource(R.string.wallet_status_success)
                            } else {
                                stringResource(R.string.wallet_status_pending)
                            }
                        },
                        color = SecondaryBlue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            HorizontalDivider(
                color = Color(0xFFE7EBF6),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

private fun Long.formatWalletTime(): String {
    if (this <= 0L) return ""
    return runCatching {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(this))
    }.getOrDefault("")
}
