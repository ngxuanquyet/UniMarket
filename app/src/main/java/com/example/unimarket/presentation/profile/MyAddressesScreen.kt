package com.example.unimarket.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimarket.R
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.presentation.theme.BackgroundLight
import com.example.unimarket.presentation.theme.BlueReview
import com.example.unimarket.presentation.theme.BorderLightGray
import com.example.unimarket.presentation.theme.PrimaryYellowDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAddressesScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editingAddress by remember { mutableStateOf<UserAddress?>(null) }
    var pendingDeleteConfirmation by remember { mutableStateOf<UserAddress?>(null) }
    var showAddressDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val pendingDeletionKeys = remember { mutableStateListOf<String>() }
    val visibleAddresses = uiState.addresses.filterNot { address ->
        pendingDeletionKeys.contains(address.pendingDeletionKey())
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_my_addresses), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingAddress = null
                            showAddressDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.my_addresses_add_address), tint = BlueReview)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        AddressesContent(
            paddingValues = paddingValues,
            addresses = visibleAddresses,
            isLoading = uiState.isLoadingAddresses,
            hasPendingDeletions = pendingDeletionKeys.isNotEmpty(),
            onAddClick = {
                editingAddress = null
                showAddressDialog = true
            },
            onEditClick = {
                editingAddress = it
                showAddressDialog = true
            },
            onDeleteClick = { address ->
                pendingDeleteConfirmation = address
            },
            onSetDefaultClick = viewModel::setDefaultAddress
        )

        if (showAddressDialog) {
            AddressEditorDialog(
                initialAddress = editingAddress,
                onDismiss = { showAddressDialog = false },
                onSave = { address ->
                    viewModel.saveAddress(address)
                    showAddressDialog = false
                }
            )
        }

        pendingDeleteConfirmation?.let { address ->
            AlertDialog(
                onDismissRequest = { pendingDeleteConfirmation = null },
                title = { Text(stringResource(R.string.my_addresses_delete_title)) },
                text = { Text(stringResource(R.string.my_addresses_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeleteConfirmation = null
                            val addressKey = address.pendingDeletionKey()
                            if (pendingDeletionKeys.contains(addressKey)) return@TextButton

                            pendingDeletionKeys.add(addressKey)
                            coroutineScope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.my_addresses_deleted),
                                    actionLabel = context.getString(R.string.common_undo),
                                    withDismissAction = true
                                )

                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    pendingDeletionKeys.remove(addressKey)
                                    return@launch
                                }

                                val deleteResult = if (address.id.isBlank()) {
                                    Result.failure(IllegalArgumentException("Address id is missing"))
                                } else {
                                    viewModel.deleteAddress(address.id)
                                }

                                pendingDeletionKeys.remove(addressKey)
                                deleteResult.onFailure { error ->
                                    snackbarHostState.showSnackbar(
                                        message = error.message ?: context.getString(R.string.my_addresses_delete_failed)
                                    )
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteConfirmation = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun AddressesContent(
    paddingValues: PaddingValues,
    addresses: List<UserAddress>,
    isLoading: Boolean,
    hasPendingDeletions: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (UserAddress) -> Unit,
    onDeleteClick: (UserAddress) -> Unit,
    onSetDefaultClick: (UserAddress) -> Unit
) {
    when {
        isLoading && addresses.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryYellowDark)
            }
        }

        addresses.isEmpty() && hasPendingDeletions -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }

        addresses.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.my_addresses_empty_title), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.my_addresses_empty_message),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onAddClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.my_addresses_add_address))
                        }
                    }
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryYellowDark
                            )
                        }
                    }
                }

                items(addresses, key = { it.id.ifBlank { "${it.recipientName}-${it.addressLine}" } }) { address ->
                    AddressCard(
                        address = address,
                        onEditClick = { onEditClick(address) },
                        onDeleteClick = { onDeleteClick(address) },
                        onSetDefaultClick = { onSetDefaultClick(address) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressCard(
    address: UserAddress,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSetDefaultClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLightGray, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.my_addresses_address_label),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (address.isDefault) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.common_default)) },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                } else {
                    TextButton(onClick = onSetDefaultClick) {
                        Text(stringResource(R.string.my_addresses_set_default))
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(address.recipientName, fontWeight = FontWeight.Medium)
            if (address.phoneNumber.isNotBlank()) {
                Text(address.phoneNumber, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(address.shortDisplay(), color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEditClick, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.common_edit))
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}

@Composable
private fun AddressEditorDialog(
    initialAddress: UserAddress?,
    onDismiss: () -> Unit,
    onSave: (UserAddress) -> Unit
) {
    var recipientName by remember(initialAddress) { mutableStateOf(initialAddress?.recipientName.orEmpty()) }
    var phoneNumber by remember(initialAddress) { mutableStateOf(initialAddress?.phoneNumber.orEmpty()) }
    var addressLine by remember(initialAddress) { mutableStateOf(initialAddress?.addressLine.orEmpty()) }
    var isDefault by remember(initialAddress) { mutableStateOf(initialAddress?.isDefault ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialAddress == null) {
                    stringResource(R.string.my_addresses_add_title)
                } else {
                    stringResource(R.string.my_addresses_edit_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text(stringResource(R.string.my_addresses_recipient_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(stringResource(R.string.my_addresses_phone_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = { Text(stringResource(R.string.my_addresses_address_input)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.my_addresses_set_as_default))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        UserAddress(
                            id = initialAddress?.id.orEmpty(),
                            recipientName = recipientName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            addressLine = addressLine.trim(),
                            isDefault = isDefault
                        )
                    )
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

private fun UserAddress.pendingDeletionKey(): String {
    return id.ifBlank { "$recipientName|$phoneNumber|$addressLine" }
}
