package com.example.unimarket.presentation.explore

import com.example.unimarket.presentation.theme.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.example.unimarket.R
import com.example.unimarket.domain.model.DeliveryMethod
    import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.presentation.theme.PrimaryYellowDark
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedCategoryLabel
import com.example.unimarket.presentation.util.localizedConditionLabel
import com.example.unimarket.presentation.util.localizedTitle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onProductClick: (String) -> Unit = {},
    onSellerClick: (String) -> Unit = {},
    onCartClick: () -> Unit = {},
    viewModel: ExploreViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasActiveFilters = uiState.selectedPriceFilter != ExplorePriceFilter.ALL ||
        uiState.selectedPriceSort != ExplorePriceSort.RECOMMENDED

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.resetExploreState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(R.string.explore_title), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                },
                actions = {
                    IconButton(
                        onClick = { onCartClick() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(ProfileAvatarBorder)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = stringResource(R.string.explore_cart),
                            tint = Color.DarkGray
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(ProfileAvatarBorder)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.explore_notifications),
                            tint = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val displayCategories = if (uiState.categories.isEmpty()) {
                listOf("All Items")
            } else {
                uiState.categories.map { it.name }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text(stringResource(R.string.explore_search_placeholder), color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.common_search),
                                tint = Color.Gray
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showFilterSheet = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (hasActiveFilters) {
                                            SecondaryBlue.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.explore_filter_products),
                                    tint = if (hasActiveFilters) SecondaryBlue else Color.Gray
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MessageBg,
                            focusedContainerColor = MessageBg,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayCategories) { category ->
                            val isSelected = category == uiState.selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) SecondaryBlue else MessageBg)
                                    .clickable { viewModel.updateSelectedCategory(category) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = localizedCategoryLabel(category),
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (uiState.matchedSellers.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.explore_seller_matches),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            uiState.matchedSellers.forEach { seller ->
                                ExploreSellerCard(
                                    seller = seller,
                                    onSellerClick = { onSellerClick(seller.sellerId) },
                                    onProductClick = onProductClick
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading && uiState.products.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SecondaryBlue)
                        }
                    }
                } else if (uiState.filteredProducts.isEmpty() && uiState.matchedSellers.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.explore_no_items_found),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(uiState.filteredProducts) { product ->
                        ExploreProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color.White
        ) {
            ExploreFilterSheet(
                uiState = uiState,
                onPriceFilterSelected = viewModel::updateSelectedPriceFilter,
                onPriceSortSelected = viewModel::updateSelectedPriceSort,
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun ExploreSellerCard(
    seller: ExploreSellerPreview,
    onSellerClick: () -> Unit,
    onProductClick: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = seller.avatarUrl),
                        contentDescription = seller.sellerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ProfileAvatarBorder)
                            .clickable { onSellerClick() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = seller.sellerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.explore_items_on_sale, seller.totalListings),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                TextButton(onClick = onSellerClick) {
                    Text(stringResource(R.string.explore_view_profile), color = SecondaryBlue)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(seller.previewProducts, key = { it.id }) { product ->
                    Column(
                        modifier = Modifier
                            .width(92.dp)
                            .clickable { onProductClick(product.id) }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = product.imageUrls.firstOrNull() ?: "https://via.placeholder.com/400"
                            ),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ProfileAvatarBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreFilterSheet(
    uiState: ExploreUiState,
    onPriceFilterSelected: (ExplorePriceFilter) -> Unit,
    onPriceSortSelected: (ExplorePriceSort) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.explore_filter_products),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.explore_price_range),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExplorePriceFilter.entries.toList()) { priceFilter ->
                    val isSelected = priceFilter == uiState.selectedPriceFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryYellowDark else MessageBg)
                            .clickable { onPriceFilterSelected(priceFilter) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priceFilter.displayLabel(),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.explore_sort_by_price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ExplorePriceSort.entries.forEach { sortOption ->
                val isSelected = sortOption == uiState.selectedPriceSort
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPriceSortSelected(sortOption) },
                    color = if (isSelected) SecondaryBlue.copy(alpha = 0.12f) else MessageBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = sortOption.displayLabel(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) SecondaryBlue else Color.DarkGray,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.common_done), color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ExploreProductCard(
    product: com.example.unimarket.domain.model.Product,
    onClick: () -> Unit = {}
) {
    var isFavorite by remember { mutableStateOf(product.isFavorite) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(ProfileAvatarBorder)
                .clickable { onClick() }
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = product.imageUrls.firstOrNull() ?: "https://via.placeholder.com/400"
                ),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Tags (e.g., PRICE DROP, NEW) -> Simplified logic based on timeAgo or condition
            val tag = if (product.condition == "New") stringResource(R.string.condition_new_badge) else null
            if (tag != null) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SecondaryBlue)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Favorite Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable { isFavorite = !isFavorite },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.explore_favorite),
                    tint = if (isFavorite) RedDanger else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = formatVnd(product.price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = localizedConditionLabel(product.condition),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (product.deliveryMethodsAvailable.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            DeliveryMethodSummaryChips(product.deliveryMethodsAvailable)
        }
    }
}

@Composable
private fun DeliveryMethodSummaryChips(methods: List<DeliveryMethod>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        methods.take(2).chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { method ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                        .background(MessageBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = method.localizedTitle(),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorePriceFilter.displayLabel(): String {
    return stringResource(
        when (this) {
            ExplorePriceFilter.ALL -> R.string.explore_filter_all_prices
            ExplorePriceFilter.UP_TO_200K -> R.string.explore_filter_up_to_200k
            ExplorePriceFilter.FROM_200K_TO_500K -> R.string.explore_filter_200k_500k
            ExplorePriceFilter.FROM_500K_TO_1M -> R.string.explore_filter_500k_1m
            ExplorePriceFilter.FROM_1M -> R.string.explore_filter_from_1m
        }
    )
}

@Composable
private fun ExplorePriceSort.displayLabel(): String {
    return stringResource(
        when (this) {
            ExplorePriceSort.RECOMMENDED -> R.string.explore_sort_recommended
            ExplorePriceSort.PRICE_LOW_TO_HIGH -> R.string.explore_sort_low_to_high
            ExplorePriceSort.PRICE_HIGH_TO_LOW -> R.string.explore_sort_high_to_low
        }
    )
}
