package com.example.unimarket.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.unimarket.presentation.theme.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.presentation.theme.PrimaryYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLogoutClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var editingAddress by remember { mutableStateOf<UserAddress?>(null) }
    var showAddressDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.updateAvatar(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Empty spacer to balance the back icon for true centering
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoadingAddresses || uiState.isUploading || uiState.isRefreshingProfile,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                ProfileHeader(
                    uiState = uiState,
                    onEditClick = { launcher.launch("image/*") },
                    onEditNameClick = {
                        newNameInput = uiState.displayName
                        showEditNameDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
                ProfileStatsRow(
                    soldCount = uiState.soldCount,
                    boughtCount = uiState.boughtCount
                )

                Spacer(modifier = Modifier.height(24.dp))
                AddressSection(
                    addresses = uiState.addresses,
                    isLoading = uiState.isLoadingAddresses,
                    onAddClick = {
                        editingAddress = null
                        showAddressDialog = true
                    },
                    onEditClick = {
                        editingAddress = it
                        showAddressDialog = true
                    },
                    onDeleteClick = viewModel::deleteAddress,
                    onSetDefaultClick = viewModel::setDefaultAddress
                )

                Spacer(modifier = Modifier.height(32.dp))
                ProfileActionsList()

                Spacer(modifier = Modifier.height(32.dp))
                LogoutButton(onClick = onLogoutClick)

                Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
            }

            if (showEditNameDialog) {
                AlertDialog(
                    onDismissRequest = { showEditNameDialog = false },
                    title = { Text("Edit Display Name") },
                    text = {
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { newNameInput = it },
                            label = { Text("New Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newNameInput.isNotBlank()) {
                                viewModel.updateDisplayName(newNameInput)
                            }
                            showEditNameDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditNameDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

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
        }
    }
}

@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
    onEditNameClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture with Edit Icon
        Box(contentAlignment = Alignment.BottomEnd) {
            val imageUrl = uiState.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=${uiState.displayName.replace(" ", "+")}&background=random" }
            
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            )
            
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painter,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, ProfileAvatarBorder, CircleShape)
                        .clickable(onClick = onEditClick)
                )

                if (painter.state is AsyncImagePainter.State.Loading || uiState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = PrimaryYellowDark,
                        strokeWidth = 3.dp
                    )
                }
            }
            
            // Edit Badge (Click to open gallery)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clickable(onClick = onEditClick)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(LightBlueAction),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.displayName.ifEmpty { "User" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Name",
                tint = PrimaryYellowDark,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onEditNameClick)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = uiState.email,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Verified Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GreenBadgeBg) // Light green background
                .border(1.dp, GreenBadgeBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = GreenBadge,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Verified Student",
                color = GreenBadge, // Green text
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileStatsRow(
    soldCount: Int,
    boughtCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(value = soldCount.toString(), label = "Items Sold", modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        StatCard(value = boughtCount.toString(), label = "Bought", modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        StatCard(
            value = "4.8", 
            label = "Rating", 
            modifier = Modifier.weight(1f),
            showStar = true
        )
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, showStar: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundLight)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (showStar) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = PrimaryYellowDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ProfileActionsList() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ActionRowItem(
            title = "My Listings",
            icon = Icons.Default.LocalOffer,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview
        )
        ActionRowItem(
            title = "My Purchases",
            icon = Icons.Default.ShoppingBag,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview
        )
        ActionRowItem(
            title = "Saved Items",
            icon = Icons.Default.Favorite,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview
        )
        ActionRowItem(
            title = "Payment Methods",
            icon = Icons.Default.CreditCard,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview
        )
        ActionRowItem(
            title = "Account Settings",
            icon = Icons.Default.Settings,
            iconBgColor = ProfileAvatarBorder,
            iconColor = Color.DarkGray
        )
    }
}

@Composable
fun AddressSection(
    addresses: List<UserAddress>,
    isLoading: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (UserAddress) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSetDefaultClick: (UserAddress) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Addresses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryYellowDark
                )
            }
        } else if (addresses.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Chua co dia chi nao", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Them dia chi de nguoi mua hoac nguoi ban co the chon khi giao nhan.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            addresses.forEach { address ->
                AddressCard(
                    address = address,
                    onEditClick = { onEditClick(address) },
                    onDeleteClick = { onDeleteClick(address.id) },
                    onSetDefaultClick = { onSetDefaultClick(address) }
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    text = "Dia chi",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (address.isDefault) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Mac dinh") },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                } else {
                    TextButton(onClick = onSetDefaultClick) {
                        Text("Dat mac dinh")
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
                    Text("Sua")
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xoa")
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
        title = { Text(if (initialAddress == null) "Them dia chi" else "Sua dia chi") },
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
                    label = { Text("Ho ten nguoi nhan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("So dien thoai") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dat lam dia chi mac dinh")
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
                Text("Luu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Huy")
            }
        }
    )
}

@Composable
fun ActionRowItem(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LightRedReviewBg) // Light red
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Log Out",
                tint = RedDanger // Reddish
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = RedDanger
            )
        }
    }
}
