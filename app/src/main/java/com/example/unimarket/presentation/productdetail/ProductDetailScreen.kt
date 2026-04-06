package com.example.unimarket.presentation.productdetail

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.unimarket.R
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.presentation.theme.*
import com.example.unimarket.presentation.util.formatVnd
import com.example.unimarket.presentation.util.localizedConditionLabel
import com.example.unimarket.presentation.util.localizedTitle
import com.example.unimarket.presentation.util.toRelativeTimeLabel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String?,
    onBackClick: () -> Unit,
    onConversationOpen: (String) -> Unit = {},
    onSellerClick: (String, String) -> Unit = { _, _ -> },
    onBuyNowClick: (String, Int) -> Unit = { _, _ -> },
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val product = uiState.product
    val isInitialLoading = uiState.isLoading && product == null

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
        }
    }

    if (isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SecondaryBlue)
        }
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProductDetailViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ProductDetailViewModel.UiEvent.OpenConversation -> {
                    onConversationOpen(event.conversationId)
                }
            }
        }
    }

    val pagerState = rememberPagerState(
        pageCount = { product?.imageUrls?.takeIf { it.isNotEmpty() }?.size ?: 1 }
    )
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isFavorite by remember(product?.id) { mutableStateOf(product?.isFavorite ?: false) }
    var purchaseAction by remember { mutableStateOf<PurchaseAction?>(null) }
    var selectedQuantity by remember(product?.id) { mutableIntStateOf(1) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* Share */ },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.product_share), tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.explore_favorite),
                            tint = if (isFavorite) RedDanger else Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (product != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (auth.uid != product.userId) {
                                    viewModel.startConversation(product)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.product_cannot_chat_self),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .height(50.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Message,
                                    contentDescription = stringResource(R.string.product_chat),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.product_chat_now),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (auth.uid != product.userId) {
                                    selectedQuantity = 1
                                    purchaseAction = PurchaseAction.AddToCart
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.product_cannot_add_own),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .height(50.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = stringResource(R.string.explore_cart),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.product_add_to_cart),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (auth.uid != product.userId) {
                                    selectedQuantity = 1
                                    purchaseAction = PurchaseAction.BuyNow
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.product_cannot_buy_own),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .height(50.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellowDark)
                        ) {
                            Text(
                                stringResource(R.string.product_buy_now),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            isRefreshing = uiState.isLoading,
            onRefresh = {
                productId?.let(viewModel::loadProduct)
            }
        ) {
            if (product == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.product_not_found),
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color.LightGray)
                        ) {
                            if (product.imageUrls.isNotEmpty()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    Image(
                                        painter = rememberAsyncImagePainter(model = product.imageUrls[page]),
                                        contentDescription = stringResource(R.string.product_image_page, product.name, page + 1),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(model = "https://via.placeholder.com/400"),
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = localizedConditionLabel(product.condition),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (product.imageUrls.size > 1) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(count = product.imageUrls.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) SecondaryBlue else Color.LightGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = product.imageUrls[index]),
                                            contentDescription = stringResource(R.string.product_thumbnail, index + 1),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = product.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatVnd(product.price),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SecondaryBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = formatVnd(product.price * 1.1),
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (product.isNegotiable) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.sell_negotiable),
                                        color = Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.product_available, product.quantityAvailable),
                            fontSize = 14.sp,
                            color = if (product.quantityAvailable > 0) Color.Gray else RedDanger,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.product_location),
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.location.ifBlank { stringResource(R.string.product_default_location) },
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(
                                    R.string.product_posted_time,
                                    product.postedAt.toRelativeTimeLabel()
                                        .ifBlank { product.timeAgo.ifBlank { stringResource(R.string.product_recently_listed) } }
                                ),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        if (product.deliveryMethodsAvailable.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.sell_delivery_methods),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            DeliveryMethodChips(product.deliveryMethodsAvailable)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSellerClick(product.userId, product.id) }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sellerAvatarPainter = rememberAsyncImagePainter(
                            model = uiState.sellerAvatarUrl.ifBlank {
                                "https://ui-avatars.com/api/?name=${product.sellerName.replace(" ", "+")}&background=random"
                            }
                        )
                        val sellerInitial = product.sellerName
                            .trim()
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifBlank { "U" }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ProfileAvatarBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sellerAvatarPainter.state !is AsyncImagePainter.State.Success) {
                                Text(
                                    text = sellerInitial,
                                    color = SecondaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (sellerAvatarPainter.state !is AsyncImagePainter.State.Error) {
                                Image(
                                    painter = sellerAvatarPainter,
                                    contentDescription = stringResource(R.string.product_seller_avatar),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Log.d("check_avatar", "sellerAvatarUrl=${uiState.sellerAvatarUrl}")

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.sellerName.ifBlank { stringResource(R.string.product_anonymous_seller) },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (uiState.isSellerVerifiedStudent) {
                                    stringResource(R.string.profile_verified_student)
                                } else {
                                    stringResource(R.string.seller_profile_campus_seller)
                                },
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = stringResource(R.string.profile_rating),
                                    tint = PrimaryYellowDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.sellerRatingCount > 0) {
                                        String.format("%.1f", uiState.sellerAverageRating)
                                    } else {
                                        stringResource(R.string.common_new)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = stringResource(R.string.product_sold_count, uiState.sellerSoldCount),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.sell_description),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = product.description.ifBlank {
                                stringResource(R.string.product_no_description)
                            },
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            lineHeight = 22.sp
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    if (product.specifications.isNotEmpty()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.sell_specifications),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            product.specifications.forEach { (key, value) ->
                                SpecRow(key, value)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (purchaseAction != null && product != null) {
        ModalBottomSheet(
            onDismissRequest = { purchaseAction = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            QuantityPickerSheet(
                productName = product.name,
                availableQuantity = product.quantityAvailable,
                selectedQuantity = selectedQuantity,
                onDecrease = {
                    selectedQuantity = (selectedQuantity - 1).coerceAtLeast(1)
                },
                onIncrease = {
                    selectedQuantity =
                        (selectedQuantity + 1).coerceAtMost(product.quantityAvailable)
                },
                onConfirm = {
                    when (purchaseAction) {
                        PurchaseAction.AddToCart -> viewModel.addToCart(product, selectedQuantity)
                        PurchaseAction.BuyNow -> onBuyNowClick(product.id, selectedQuantity)
                        null -> Unit
                    }
                    purchaseAction = null
                }
            )
        }
    }
}

private enum class PurchaseAction {
    AddToCart,
    BuyNow
}

@Composable
private fun QuantityPickerSheet(
    productName: String,
    availableQuantity: Int,
    selectedQuantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.product_choose_quantity),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = productName,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.product_available_stock, availableQuantity), fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecrease,
                    enabled = selectedQuantity > 1
                ) {
                    Text("-")
                }
                Text(
                    text = selectedQuantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onIncrease,
                    enabled = selectedQuantity < availableQuantity
                ) {
                    Text("+")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            enabled = availableQuantity > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellowDark)
        ) {
            Text(stringResource(R.string.product_confirm), color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun DeliveryMethodChips(methods: List<DeliveryMethod>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { method ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = method.localizedTitle(),
                            color = SecondaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
