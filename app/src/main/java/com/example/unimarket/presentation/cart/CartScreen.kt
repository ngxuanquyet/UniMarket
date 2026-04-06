package com.example.unimarket.presentation.cart

import com.example.unimarket.presentation.theme.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.unimarket.domain.model.CartItem
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onCheckoutClick: (List<String>) -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var itemPendingRemoval by remember { mutableStateOf<CartItem?>(null) }
    val allSelected = uiState.cartItems.isNotEmpty() &&
        uiState.selectedCartItemIds.size == uiState.cartItems.size

    itemPendingRemoval?.let { pendingItem ->
        AlertDialog(
            onDismissRequest = { itemPendingRemoval = null },
            title = { Text(stringResource(R.string.cart_remove_item_title)) },
            text = { Text(stringResource(R.string.cart_remove_item_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeItem(pendingItem.id)
                        itemPendingRemoval = null
                    }
                ) {
                    Text(stringResource(R.string.common_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingRemoval = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cart_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        bottomBar = {
            if (uiState.cartItems.isNotEmpty()) {
                CartBottomBar(
                    total = uiState.selectedSubtotal,
                    selectedCount = uiState.selectedCartItemIds.size,
                    enabled = uiState.selectedCartItemIds.isNotEmpty(),
                    onPlaceOrderClick = {
                        onCheckoutClick(uiState.selectedCartItemIds.toList())
                    }
                )
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Icon(
//                        imageVector = Icons.Default.RemoveShoppingCart,
//                        contentDescription = null,
//                        tint = Color.Gray,
//                        modifier = Modifier.size(48.dp)
//                    )
                    LoadingAminEmptyCart()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.cart_empty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cart_products),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { viewModel.selectAll(it) }
                        )
                        Text(stringResource(R.string.cart_select_all))
                    }
                }

                uiState.cartItems.forEach { item ->
                    CartItemRow(
                        item = item,
                        isSelected = item.id in uiState.selectedCartItemIds,
                        onSelectionChange = { viewModel.toggleSelection(item.id, it) },
                        onIncrease = { viewModel.updateQuantity(item.id, item.quantity + 1) },
                        onDecrease = {
                            if (item.quantity == 1) {
                                itemPendingRemoval = item
                            } else {
                                viewModel.updateQuantity(item.id, item.quantity - 1)
                            }
                        },
                        onRemove = { viewModel.removeItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.cart_selection_summary),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SummaryRow(
                            stringResource(R.string.cart_selected_items_count, uiState.selectedCartItemIds.size),
                            formatVnd(uiState.selectedSubtotal)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DividerColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.cart_selected_total),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                formatVnd(uiState.selectedSubtotal),
                                fontWeight = FontWeight.Bold,
                                color = SecondaryBlue,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )

            Spacer(modifier = Modifier.width(8.dp))

            Image(
                painter = rememberAsyncImagePainter(model = item.product.imageUrls.firstOrNull()),
                contentDescription = item.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ProfileAvatarBorder)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_remove),
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatVnd(item.product.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = SecondaryBlue,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(
                        R.string.cart_item_total,
                        formatVnd(item.product.price * item.quantity)
                    ),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, DividerColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.common_decrease),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDecrease() }
                    )
                    Text(
                        text = item.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.common_increase),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onIncrease() }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(
            value,
            color = valueColor,
            fontWeight = if (valueColor != Color.Black) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun CartBottomBar(
    total: Double,
    selectedCount: Int,
    enabled: Boolean,
    onPlaceOrderClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.cart_total_payment),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = formatVnd(total),
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryYellowDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onPlaceOrderClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.cart_checkout_count, selectedCount),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingAminEmptyCart() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.cart_empty)
    )

    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = {progress}
    )

}
