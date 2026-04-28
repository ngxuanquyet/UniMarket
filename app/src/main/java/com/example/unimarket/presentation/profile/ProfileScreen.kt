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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.unimarket.R
import com.example.unimarket.localization.LanguageManager
import com.example.unimarket.localization.LanguageOption
import com.example.unimarket.presentation.auth.UniversitySelectionDialog
import com.example.unimarket.presentation.navigation.UniversityListViewModel
import com.example.unimarket.presentation.theme.PrimaryYellowDark
import com.example.unimarket.presentation.theme.RatingStarYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLogoutClick: () -> Unit,
    onMyAddressesClick: () -> Unit = {},
    onMyPurchasesClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onPaymentMethodsClick: () -> Unit = {},
    onSellerOrdersClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val universityListViewModel: UniversityListViewModel = hiltViewModel()
    val universityListState by universityListViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditUniversityDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var universityInput by remember { mutableStateOf("") }
    var currentLanguage by remember { mutableStateOf(LanguageManager.getSelectedLanguage(context)) }

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
                title = {
                    Text(
                        stringResource(R.string.profile_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
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
            isRefreshing = uiState.isRefreshingProfile,
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
                    boughtCount = uiState.boughtCount,
                    averageRating = uiState.averageRating,
                    ratingCount = uiState.ratingCount
                )

                Spacer(modifier = Modifier.height(24.dp))
                ProfileActionsList(
                    currentUniversity = uiState.university,
                    currentLanguage = currentLanguage,
                    onMyAddressesClick = onMyAddressesClick,
                    onMyPurchasesClick = onMyPurchasesClick,
                    onWalletClick = onWalletClick,
                    onPaymentMethodsClick = onPaymentMethodsClick,
                    onSellerOrdersClick = onSellerOrdersClick,
                    onStatisticsClick = onStatisticsClick,
                    onChangeUniversityClick = {
                        universityInput = uiState.university
                        showEditUniversityDialog = true
                    },
                    onLanguageClick = { showLanguageDialog = true }
                )

                Spacer(modifier = Modifier.height(32.dp))
                LogoutButton(onClick = onLogoutClick)

                Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
            }

            if (showEditNameDialog) {
                AlertDialog(
                    onDismissRequest = { showEditNameDialog = false },
                    title = { Text(stringResource(R.string.profile_edit_display_name)) },
                    text = {
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { newNameInput = it },
                            label = { Text(stringResource(R.string.profile_new_name)) },
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
                            Text(stringResource(R.string.common_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditNameDialog = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            if (showLanguageDialog) {
                LanguageSelectionDialog(
                    selectedLanguage = currentLanguage,
                    onDismiss = { showLanguageDialog = false },
                    onLanguageSelected = { language ->
                        currentLanguage = language
                        showLanguageDialog = false
                        LanguageManager.updateLanguage(context, language)
                    }
                )
            }

            if (showEditUniversityDialog) {
                UniversitySelectionDialog(
                    title = stringResource(R.string.profile_change_university),
                    value = universityInput,
                    onValueChange = { universityInput = it },
                    options = universityListState.options,
                    enabled = !uiState.isUploading,
                    onDismiss = { showEditUniversityDialog = false },
                    onInvalidSelection = {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.auth_error_select_university_from_list),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onConfirm = { selectedUniversity ->
                        viewModel.updateUniversity(selectedUniversity.name)
                        showEditUniversityDialog = false
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
                    contentDescription = stringResource(R.string.profile_picture),
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
                        contentDescription = stringResource(R.string.profile_edit_picture),
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
                text = uiState.displayName.ifEmpty { stringResource(R.string.profile_default_user) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.profile_edit_name),
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
                text = stringResource(R.string.profile_verified_student),
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
    boughtCount: Int,
    averageRating: Double,
    ratingCount: Int
) {
    val ratingLabel = if (ratingCount > 0) {
        String.format("%.1f", averageRating)
    } else {
        stringResource(R.string.common_new)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(
            value = soldCount.toString(),
            label = stringResource(R.string.profile_items_sold),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        StatCard(
            value = boughtCount.toString(),
            label = stringResource(R.string.profile_bought),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        StatCard(
            value = ratingLabel,
            label = stringResource(R.string.profile_rating),
            modifier = Modifier.weight(1f),
            showStar = ratingCount > 0
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
                    contentDescription = stringResource(R.string.profile_rating),
                    tint = RatingStarYellow,
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
fun ProfileActionsList(
    currentUniversity: String,
    currentLanguage: LanguageOption,
    onMyAddressesClick: () -> Unit = {},
    onMyPurchasesClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onPaymentMethodsClick: () -> Unit = {},
    onSellerOrdersClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onChangeUniversityClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ActionRowItem(
            title = stringResource(R.string.profile_income_expense),
            icon = Icons.Default.BarChart,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onStatisticsClick
        )
        ActionRowItem(
            title = stringResource(R.string.profile_my_purchases),
            icon = Icons.Default.ShoppingBag,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onMyPurchasesClick
        )
        ActionRowItem(
            title = stringResource(R.string.profile_wallet),
            icon = Icons.Default.AccountBalance,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onWalletClick
        )
        ActionRowItem(
            title = stringResource(R.string.profile_seller_orders),
            icon = Icons.Default.Inventory2,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onSellerOrdersClick
        )
        ActionRowItem(
            title = stringResource(R.string.profile_payment_methods),
            icon = Icons.Default.CreditCard,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onPaymentMethodsClick
        )
        ActionRowItem(
            title = stringResource(R.string.profile_my_addresses),
            icon = Icons.Default.LocationOn,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onMyAddressesClick
        )
        ActionRowItem(
            title = stringResource(R.string.profile_change_university),
            subtitle = currentUniversity.ifBlank { stringResource(R.string.auth_university_placeholder) },
            icon = Icons.Default.School,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onChangeUniversityClick
        )
        ActionRowItem(
            title = stringResource(R.string.language_label),
            subtitle = currentLanguage.displayName(),
            icon = Icons.Default.Language,
            iconBgColor = LightBlueReviewBg,
            iconColor = BlueReview,
            onClick = onLanguageClick
        )
    }
}

@Composable
fun ActionRowItem(
    title: String,
    subtitle: String? = null,
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
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = stringResource(R.string.common_go),
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
                contentDescription = stringResource(R.string.profile_log_out),
                tint = RedDanger // Reddish
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.profile_log_out),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = RedDanger
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    selectedLanguage: LanguageOption,
    onDismiss: () -> Unit,
    onLanguageSelected: (LanguageOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.language_dialog_message))
                LanguageOption.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == language,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun LanguageOption.displayName(): String {
    return when (this) {
        LanguageOption.ENGLISH -> stringResource(R.string.language_english)
        LanguageOption.VIETNAMESE -> stringResource(R.string.language_vietnamese)
    }
}
