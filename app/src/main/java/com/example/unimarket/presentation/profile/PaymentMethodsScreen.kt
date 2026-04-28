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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimarket.R
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.BorderLightGray
import com.example.unimarket.presentation.theme.LightBlueReviewBg
import com.example.unimarket.presentation.theme.RedDanger
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: PaymentMethodsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
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
        containerColor = Color(0xFFF8FAFD),
        floatingActionButton = {
            Button(
                onClick = {
                    onAddClick()
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
                        onAddClick = onAddClick
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 10.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.payment_methods_hint),
                                color = TextGray,
                                lineHeight = 24.sp
                            )
                        }
                        items(uiState.methods, key = { it.id.ifBlank { it.displayTitle } }) { method ->
                            PaymentMethodCard(
                                method = method,
                                onEdit = {
                                    onEditClick(method.id)
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
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (method.isDefault) {
                    Box(
                        modifier = Modifier
                            .background(SecondaryBlue, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.common_default),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(28.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = Color(0xFF9AA3B2),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_edit)) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SecondaryBlue)
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.common_delete),
                                    color = RedDanger
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RedDanger)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            when (method.type) {
                SellerPaymentMethodType.BANK_TRANSFER -> {
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_account_name),
                        value = method.accountName
                    )
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_bank_name),
                        value = method.bankName
                    )
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_account_number),
                        value = method.accountNumber,
                        showDivider = false
                    )
                }

                SellerPaymentMethodType.MOMO -> {
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_account_name),
                        value = method.accountName
                    )
                    PaymentDetailRow(
                        label = stringResource(R.string.payment_methods_phone_number),
                        value = method.phoneNumber,
                        showDivider = false
                    )
                }

                SellerPaymentMethodType.CASH_ON_DELIVERY -> Unit
                SellerPaymentMethodType.WALLET -> Unit
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    if (value.isBlank()) return
    Spacer(modifier = Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TextGray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = TextDarkBlack,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
    if (showDivider) {
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderLightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodEditorScreen(
    methodId: String?,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: PaymentMethodsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialValue = methodId?.let { id ->
        uiState.methods.firstOrNull { it.id == id }
    }
    val bankOptions = uiState.bankOptions
    var type by remember(initialValue) {
        mutableStateOf(SellerPaymentMethodType.BANK_TRANSFER)
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
    val bankSelectRequiredMessage = stringResource(R.string.payment_methods_error_bank_select)
    val accountNumberRequiredMessage = stringResource(R.string.payment_methods_error_account_number)
    val phoneNumberRequiredMessage = stringResource(R.string.payment_methods_error_phone_number)
    val scrollState = rememberScrollState()

    fun saveDraft() {
        val matchedBank = bankOptions.find { option ->
            option.bankCode == bankCode ||
                option.shortName.equals(bankName, ignoreCase = true) ||
                option.name.equals(bankName, ignoreCase = true)
        }
        val draft = SellerPaymentMethod(
            id = initialValue?.id.orEmpty(),
            type = type,
            label = label,
            accountName = accountName,
            accountNumber = accountNumber,
            bankCode = matchedBank?.bankCode ?: bankCode,
            bankName = matchedBank?.shortName ?: bankName,
            phoneNumber = phoneNumber,
            note = note,
            isDefault = isDefault
        )

        errorMessage = when {
            draft.accountName.isBlank() -> accountNameRequiredMessage
            draft.type == SellerPaymentMethodType.BANK_TRANSFER && draft.bankName.isBlank() ->
                bankNameRequiredMessage

            draft.type == SellerPaymentMethodType.BANK_TRANSFER && draft.bankCode.isBlank() ->
                bankSelectRequiredMessage

            draft.type == SellerPaymentMethodType.BANK_TRANSFER && draft.accountNumber.isBlank() ->
                accountNumberRequiredMessage

            draft.type == SellerPaymentMethodType.MOMO && draft.phoneNumber.isBlank() ->
                phoneNumberRequiredMessage

            else -> null
        }
        if (errorMessage == null) {
            viewModel.saveMethod(draft)
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialValue == null) {
                            stringResource(R.string.payment_methods_add_title)
                        } else {
                            stringResource(R.string.payment_methods_edit_title)
                        },
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = TextDarkBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                PaymentTypeButton(
                    text = stringResource(R.string.payment_methods_select_bank),
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { type = SellerPaymentMethodType.BANK_TRANSFER }
                )

                Spacer(modifier = Modifier.height(24.dp))

                PaymentEditorField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = stringResource(R.string.payment_methods_label),
                    leadingIcon = Icons.Default.CreditCard,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                PaymentEditorField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    placeholder = stringResource(R.string.payment_methods_account_name),
                    leadingIcon = Icons.Default.Person,
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == SellerPaymentMethodType.BANK_TRANSFER) {
                    Spacer(modifier = Modifier.height(14.dp))
                    BankSuggestionField(
                        value = bankName,
                        bankOptions = bankOptions,
                        onValueChange = {
                            bankName = it
                            bankCode = bankOptions.firstOrNull { option ->
                                option.shortName.equals(it, ignoreCase = true) ||
                                    option.name.equals(it, ignoreCase = true)
                            }?.bankCode.orEmpty()
                        },
                        onBankSelected = { option ->
                            bankName = option.shortName
                            bankCode = option.bankCode
                        },
                        placeholder = stringResource(R.string.payment_methods_bank_name),
                        leadingIcon = Icons.Default.AccountBalance
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PaymentEditorField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        placeholder = stringResource(R.string.payment_methods_account_number),
                        leadingIcon = Icons.Default.CreditCard,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    PaymentEditorField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        placeholder = stringResource(R.string.payment_methods_phone_number),
                        leadingIcon = Icons.Default.PhoneAndroid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                PaymentEditorField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = stringResource(R.string.payment_methods_note),
                    leadingIcon = Icons.Default.Description,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFF5F7FC), RoundedCornerShape(10.dp))
                        .clickable { isDefault = !isDefault }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.payment_methods_set_default),
                        modifier = Modifier.weight(1f),
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = it, color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryBlue)
                    ) {
                        Text(stringResource(R.string.common_cancel), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { saveDraft() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B72F0))
                    ) {
                        Text(stringResource(R.string.common_save), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
    }
}

@Composable
private fun PaymentTypeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF0B72F0) else BorderLightGray
    val backgroundColor = if (selected) Color.White else Color(0xFFF8FAFD)
    val contentColor = if (selected) Color(0xFF0B72F0) else Color(0xFF8B95A7)

    Row(
        modifier = modifier
            .height(58.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun BankSuggestionField(
    value: String,
    bankOptions: List<BankOption>,
    onValueChange: (String) -> Unit,
    onBankSelected: (BankOption) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val query = value.trim()
    val suggestions = remember(query, bankOptions) {
        if (query.length < MIN_BANK_QUERY_LENGTH) {
            emptyList()
        } else {
            bankOptions.filter { option ->
                option.shortName.contains(query, ignoreCase = true) ||
                    option.name.contains(query, ignoreCase = true) ||
                    option.bankCode.contains(query, ignoreCase = true)
            }
        }.take(12)
    }
    val showSuggestions = expanded && suggestions.isNotEmpty()

    LaunchedEffect(showSuggestions, suggestions.size) {
        if (showSuggestions) {
            delay(100)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        PaymentEditorField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.trim().length >= MIN_BANK_QUERY_LENGTH
            },
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = Icons.Default.KeyboardArrowDown,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) expanded = false
                }
        )

        if (showSuggestions) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column {
                    suggestions.take(5).forEachIndexed { index, option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBankSelected(option)
                                    expanded = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = option.shortName,
                                color = TextDarkBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (option.name.isNotBlank()) {
                                Text(
                                    text = option.name,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        if (index < suggestions.take(5).lastIndex) {
                            HorizontalDivider(color = BorderLightGray)
                        }
                    }
                }
            }
        }
    }
}

private const val MIN_BANK_QUERY_LENGTH = 2

@Composable
private fun PaymentEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF98A1B2),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFF98A1B2),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF98A1B2),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF0B72F0),
            unfocusedBorderColor = BorderLightGray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = Color(0xFF0B72F0)
        )
    )
}
