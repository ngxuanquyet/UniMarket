package com.example.unimarket.presentation.mylistings

import com.example.unimarket.presentation.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarket.R
import com.example.unimarket.domain.model.Product
import com.example.unimarket.presentation.navigation.Screen
import com.example.unimarket.presentation.util.formatVnd
import kotlinx.coroutines.launch

val SurfaceWhite = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: MyListingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.my_listings_deleted)
    val undoLabel = stringResource(R.string.common_undo)
    
    var itemToDelete by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                itemToDelete = null
            },
            title = { Text(stringResource(R.string.my_listings_confirm_deletion)) },
            text = { Text(stringResource(R.string.my_listings_confirm_deletion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val product = itemToDelete!!
                        showDeleteDialog = false
                        itemToDelete = null
                        
                        // Execute deletion
                        viewModel.deleteListing(product)
                        
                        // Show Undo Snackbar
                        coroutineScope.launch {
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = deletedMessage,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                viewModel.undoDelete()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = RedDanger)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.common_cancel), color = TextDarkBlack)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.profile_my_listings), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDarkBlack)
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextDarkBlack)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO Build Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search), tint = TextDarkBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = SurfaceWhite,
        floatingActionButton = {
            // "Post New Item" FAB
            FloatingActionButton(
                onClick = { onAddClick() },
                containerColor = AppBlue,
                contentColor = SurfaceWhite,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 10.dp) // Maintain padding above bottom nav
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = stringResource(R.string.my_listings_post_new_item))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            val tabs = listOf(
                stringResource(R.string.my_listings_tab_active),
                stringResource(R.string.my_listings_tab_sold),
                stringResource(R.string.my_listings_tab_drafts)
            )
            TabRow(
                selectedTabIndex = uiState.currentTab,
                containerColor = SurfaceWhite,
                contentColor = AppBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab]),
                        color = AppBlue,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = BorderLightGray) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.currentTab == index,
                        onClick = { viewModel.setTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (uiState.currentTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = if (uiState.currentTab == index) AppBlue else TextGray
                            )
                        }
                    )
                }
            }

            // Content Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
            ) {
                if (uiState.isLoading && uiState.displayedListings.isEmpty()) {
                    CircularProgressIndicator(color = AppBlue, modifier = Modifier.align(Alignment.Center))
                } else {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Stat Card
                            item {
                                Surface(
                                    shape = RoundedCornerShape(32.dp),
                                    color = LightBlueSelection, // Light blue tint
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(stringResource(R.string.my_listings_live_items).uppercase(), color = AppBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(stringResource(R.string.my_listings_items_count, uiState.liveItemsCount), color = TextDarkBlack, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(stringResource(R.string.my_listings_est_value).uppercase(), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(formatVnd(uiState.estimatedValue), color = TextDarkBlack, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }

                            // Listing Items
                            items(uiState.displayedListings) { product ->
                                ListingCard(
                                    product = product,
                                    onEditClick = { onEditClick(it) },
                                    onDeleteClick = {
                                        itemToDelete = product
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListingCard(
    product: Product,
    onEditClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, TagBlueBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Product Image
                AsyncImage(
                    model = product.imageUrls.firstOrNull() ?: "https://via.placeholder.com/150",
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BorderLightGray)
                )

                Spacer(modifier = Modifier.width(16.dp))

            // Product Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isUnderReview = product.name.contains("Watch", ignoreCase = true)
                    val isDraft = product.sellerName == "Draft"

                    val badgeText = when {
                        isDraft -> stringResource(R.string.my_listings_status_draft)
                        isUnderReview -> stringResource(R.string.my_listings_status_under_review)
                        else -> stringResource(R.string.my_listings_status_active)
                    }
                    val badgeColor = when {
                        isDraft -> TextGray
                        isUnderReview -> OrangeBadge
                        else -> GreenBadge
                    }
                    val badgeBg = when {
                        isDraft -> BorderLightGray
                        isUnderReview -> OrangeBadgeBg
                        else -> GreenBadgeBg
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        formatVnd(product.price),
                        color = AppBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    product.name,
                    color = TextDarkBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 20.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val views = (10..100).random() // Dummy views count
                Text(
                    stringResource(R.string.my_listings_views_time, product.timeAgo, views),
                    color = TextGray,
                    fontSize = 12.sp
                )

                val isUnderReviewTextFlag = product.name.contains("Watch", ignoreCase = true)
                if (isUnderReviewTextFlag) {
                     Text(
                        stringResource(R.string.my_listings_pending_approval),
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ActionChipBg)
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            val isUnderReview = product.name.contains("Watch", ignoreCase = true)
            val isDraft = product.sellerName == "Draft"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isDraft) {
                    ActionChip(stringResource(R.string.my_listings_edit_draft), Icons.Default.Edit, TextDarkBlack, ActionChipBg, Modifier.weight(1f)) { onEditClick(product.id) }
                    ActionChip(stringResource(R.string.my_listings_delete_draft), Icons.Default.DeleteOutline, RedDanger, RedDangerBg, Modifier.weight(1f)) { onDeleteClick() }
                } else if (isUnderReview) {
                    ActionChip(stringResource(R.string.my_listings_edit_listing), Icons.Default.Edit, TextDarkBlack, ActionChipBg, Modifier.weight(1f)) { onEditClick(product.id) }
                    ActionChip(stringResource(R.string.common_cancel), Icons.Outlined.Cancel, RedDanger, RedDangerBg, Modifier.weight(1f)) { onDeleteClick() }
                } else {
                    ActionChip(stringResource(R.string.common_edit), Icons.Default.Edit, TextDarkBlack, ActionChipBg, Modifier.weight(1f)) { onEditClick(product.id) }
                    ActionChip(stringResource(R.string.my_listings_tab_sold), Icons.Default.CheckCircleOutline, AppBlue, LightBlueSelection, Modifier.weight(1f)) { /* TODO */ }
                    ActionChip(stringResource(R.string.common_delete), Icons.Default.DeleteOutline, RedDanger, RedDangerBg, Modifier.weight(1f)) { onDeleteClick() }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    text: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    bgColor: Color, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(36.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
