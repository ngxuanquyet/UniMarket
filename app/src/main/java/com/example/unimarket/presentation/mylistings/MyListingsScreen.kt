package com.example.unimarket.presentation.mylistings

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarket.domain.model.Product
import com.example.unimarket.presentation.navigation.Screen

val AppBlue = Color(0xFF29B6F6)
val TextDarkBlack = Color(0xFF1E293B)
val TextGray = Color(0xFF64748B)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceWhite = Color.White
val GreenBadge = Color(0xFF4CAF50)
val GreenBadgeBg = Color(0xFFE8F5E9)
val OrangeBadge = Color(0xFFFF9800)
val OrangeBadgeBg = Color(0xFFFFF3E0)
val RedDangerText = Color(0xFFE53935)
val RedDangerBg = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    navController: NavController,
    viewModel: MyListingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("My Listings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDarkBlack) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDarkBlack)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO Build Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextDarkBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = SurfaceWhite,
        floatingActionButton = {
            // "Post New Item" FAB overlapping bottom navigation or scrollable list
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.Sell.route) },
                icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Post New Item", tint = SurfaceWhite) },
                text = { Text("Post New Item", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                containerColor = AppBlue,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(bottom = 80.dp).fillMaxWidth(0.85f).height(56.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            val tabs = listOf("Active", "Sold", "Drafts")
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
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
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
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = AppBlue, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stat Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = Color(0xFFE1F5FE), // Light blue tint
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("LIVE ITEMS", color = AppBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${uiState.liveItemsCount} Items", color = TextDarkBlack, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EST. VALUE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$${String.format("%.2f", uiState.estimatedValue)}", color = TextDarkBlack, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }

                        // Listing Items
                        items(uiState.displayedListings) { product ->
                            ListingCard(product = product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListingCard(product: Product) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEDF2F7))
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
                        .background(Color(0xFFE2E8F0))
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
            // Product Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge (Mock Logic for 'Under Review' vs 'Active')
                    val isUnderReview = product.name.contains("Watch", ignoreCase = true)
                    val badgeText = if (isUnderReview) "UNDER REVIEW" else "ACTIVE"
                    val badgeColor = if (isUnderReview) OrangeBadge else GreenBadge
                    val badgeBg = if (isUnderReview) OrangeBadgeBg else GreenBadgeBg
                    
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Text(
                        "$${String.format("%.2f", product.price)}",
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
                    "${product.timeAgo} • $views views",
                    color = TextGray,
                    fontSize = 12.sp
                )
                
                val isUnderReviewTextFlag = product.name.contains("Watch", ignoreCase = true)
                if (isUnderReviewTextFlag) {
                     Text(
                        "Pending approval from mods",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            val isUnderReview = product.name.contains("Watch", ignoreCase = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isUnderReview) {
                    ActionChip("Edit Listing", Icons.Default.Edit, TextDarkBlack, Color(0xFFF1F5F9), Modifier.weight(1f))
                    ActionChip("Cancel", Icons.Outlined.Cancel, RedDangerText, RedDangerBg, Modifier.weight(1f))
                } else {
                    ActionChip("Edit", Icons.Default.Edit, TextDarkBlack, Color(0xFFF1F5F9), Modifier.weight(1f))
                    ActionChip("Sold", Icons.Default.CheckCircleOutline, AppBlue, Color(0xFFE1F5FE), Modifier.weight(1f))
                    ActionChip("Delete", Icons.Default.DeleteOutline, RedDangerText, RedDangerBg, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ActionChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, bgColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(36.dp).clickable { /* TODO Action */ }
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
