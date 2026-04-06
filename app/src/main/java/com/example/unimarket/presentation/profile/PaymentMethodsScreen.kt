package com.example.unimarket.presentation.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimarket.R
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.BorderLightGray
import com.example.unimarket.presentation.theme.LightBlueReviewBg
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.TextDarkBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBackClick: () -> Unit,
    viewModel: PaymentMethodsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingMethod by remember { mutableStateOf<SellerPaymentMethod?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    if (showEditor) {
        PaymentMethodEditorDialog(
            initialValue = editingMethod,
            onDismiss = {
                showEditor = false
                editingMethod = null
            },
            onSave = { method ->
                viewModel.saveMethod(method)
                showEditor = false
                editingMethod = null
            }
        )
    }

    pendingDeleteId?.let { methodId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.payment_methods_delete_title)) },
            text = { Text(stringResource(R.string.payment_methods_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMethod(methodId)
                        pendingDeleteId = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_payment_methods), fontWeight = FontWeight.Bold) },
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
        containerColor = Color.White,
        floatingActionButton = {
            Button(
                onClick = {
                    editingMethod = null
                    showEditor = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.payment_methods_add), color = Color.White)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.methods.isEmpty() -> {
                    CircularProgressIndicator(
                        color = SecondaryBlue,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.methods.isEmpty() -> {
                    EmptyPaymentMethodsState(
                        onAddClick = {
                            editingMethod = null
                            showEditor = true
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.payment_methods_hint),
                                color = Color.Gray
                            )
                        }
                        items(uiState.methods, key = { it.id.ifBlank { it.displayTitle } }) { method ->
                            PaymentMethodCard(
                                method = method,
                                onEdit = {
                                    editingMethod = method
                                    showEditor = true
                                },
                                onDelete = {
                                    pendingDeleteId = method.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPaymentMethodsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(LightBlueReviewBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = SecondaryBlue,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.payment_methods_empty_title),
            fontWeight = FontWeight.Bold,
            color = TextDarkBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.payment_methods_empty_message),
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
        ) {
            Text(stringResource(R.string.payment_methods_add), color = Color.White)
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: SellerPaymentMethod,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLightGray, RoundedCornerShape(24.dp))
            .background(BackgroundLight, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (method.type) {
                            SellerPaymentMethodType.BANK_TRANSFER -> Icons.Default.AccountBalance
                            SellerPaymentMethodType.MOMO -> Icons.Default.PhoneAndroid
                            SellerPaymentMethodType.CASH_ON_DELIVERY -> Icons.Default.CreditCard
                        },
                        contentDescription = null,
                        tint = SecondaryBlue
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(method.displayTitle, fontWeight = FontWeight.Bold, color = TextDarkBlack)
                    if (method.shortSubtitle.isNotBlank()) {
                        Text(method.shortSubtitle, color = Color.Gray)
                    }
                }
                if (method.isDefault) {
                    Text(
                        text = stringResource(R.string.common_default),
                        color = SecondaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderLightGray)
            Spacer(modifier = Modifier.height(12.dp))

            when (method.type) {
                SellerPaymentMethodType.BANK_TRANSFER -> {
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_bank_name),
                        value = method.bankName
                    )
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_account_name),
                        value = method.accountName
                    )
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_account_number),
                        value = method.accountNumber
                    )
                    if (method.bankCode.isNotBlank()) {
                        PaymentDetailRow(
                            label = stringResource(R.string.payment_methods_bank_code),
                            value = method.bankCode
                        )
                    }
                }

                SellerPaymentMethodType.MOMO -> {
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_account_name),
                        value = method.accountName
                    )
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_phone_number),
                        value = method.phoneNumber
                    )
                }

                SellerPaymentMethodType.CASH_ON_DELIVERY -> Unit
            }

            if (method.note.isNotBlank()) {
                PaymentDetailRow(
                    label = stringResource(R.string.payment_methods_note),
                    value = method.note
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_edit))
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = label, color = Color.Gray)
    Text(text = value, fontWeight = FontWeight.SemiBold, color = TextDarkBlack)
}

@Composable
private fun PaymentMethodEditorDialog(
    initialValue: SellerPaymentMethod?,
    onDismiss: () -> Unit,
    onSave: (SellerPaymentMethod) -> Unit
) {
    var type by remember(initialValue) {
        mutableStateOf(initialValue?.type ?: SellerPaymentMethodType.BANK_TRANSFER)
    }
    var label by remember(initialValue) { mutableStateOf(initialValue?.label.orEmpty()) }
    var accountName by remember(initialValue) { mutableStateOf(initialValue?.accountName.orEmpty()) }
    var accountNumber by remember(initialValue) { mutableStateOf(initialValue?.accountNumber.orEmpty()) }
    var bankCode by remember(initialValue) { mutableStateOf(initialValue?.bankCode.orEmpty()) }
    var bankName by remember(initialValue) { mutableStateOf(initialValue?.bankName.orEmpty()) }
    var phoneNumber by remember(initialValue) { mutableStateOf(initialValue?.phoneNumber.orEmpty()) }
    var note by remember(initialValue) { mutableStateOf(initialValue?.note.orEmpty()) }
    var isDefault by remember(initialValue) { mutableStateOf(initialValue?.isDefault ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val accountNameRequiredMessage = stringResource(R.string.payment_methods_error_account_name)
    val bankNameRequiredMessage = stringResource(R.string.payment_methods_error_bank_name)
    val accountNumberRequiredMessage = stringResource(R.string.payment_methods_error_account_number)
    val phoneNumberRequiredMessage = stringResource(R.string.payment_methods_error_phone_number)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialValue == null) {
                    stringResource(R.string.payment_methods_add_title)
                } else {
                    stringResource(R.string.payment_methods_edit_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == SellerPaymentMethodType.BANK_TRANSFER,
                        onClick = { type = SellerPaymentMethodType.BANK_TRANSFER },
                        label = { Text(stringResource(R.string.payment_bank_transfer)) }
                    )
                    FilterChip(
                        selected = type == SellerPaymentMethodType.MOMO,
                        onClick = { type = SellerPaymentMethodType.MOMO },
                        label = { Text("MoMo") }
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.payment_methods_label)) },
                    placeholder = { Text(stringResource(R.string.payment_methods_label_placeholder)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.payment_methods_account_name)) },
                    singleLine = true
                )

                if (type == SellerPaymentMethodType.BANK_TRANSFER) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.payment_methods_bank_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.payment_methods_account_number)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bankCode,
                        onValueChange = { bankCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.payment_methods_bank_code)) },
                        placeholder = { Text(stringResource(R.string.payment_methods_bank_code_placeholder)) },
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.payment_methods_phone_number)) },
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.payment_methods_note)) }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.payment_methods_set_default),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                }

                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val draft = SellerPaymentMethod(
                        id = initialValue?.id.orEmpty(),
                        type = type,
                        label = label,
                        accountName = accountName,
                        accountNumber = accountNumber,
                        bankCode = bankCode,
                        bankName = bankName,
                        phoneNumber = phoneNumber,
                        note = note,
                        isDefault = isDefault
                    )

                    errorMessage = when {
                        draft.accountName.isBlank() -> accountNameRequiredMessage
                        draft.type == SellerPaymentMethodType.BANK_TRANSFER && draft.bankName.isBlank() ->
                            bankNameRequiredMessage

                        draft.type == SellerPaymentMethodType.BANK_TRANSFER && draft.accountNumber.isBlank() ->
                            accountNumberRequiredMessage

                        draft.type == SellerPaymentMethodType.MOMO && draft.phoneNumber.isBlank() ->
                            phoneNumberRequiredMessage

                        else -> null
                    }
                    if (errorMessage == null) {
                        onSave(draft)
                    }
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
