package com.example.unimarket.presentation.home

import com.example.unimarket.presentation.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import com.example.unimarket.presentation.components.ProductCard
import com.example.unimarket.presentation.navigation.Screen
import com.example.unimarket.presentation.theme.PrimaryYellowDark
import com.example.unimarket.presentation.theme.SecondaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCartClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            HomeTopBar(
                onCartClick = onCartClick
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Banner Section
            HomeBanner(modifier = Modifier.padding(horizontal = 16.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Categories Section
            CategoriesSection(
                categories = uiState.categories,
                selectedCategoryId = "1",
                onCategorySelected = { /* Handle category selection */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recommended Section
            Text(
                text = "Recommended for You",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            RecommendedProductsGrid(
                products = uiState.recommendedProducts,
                onProductClick = { /* Navigate to detail */ }
            )
            
            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for FAB and Nav
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onCartClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "UniMarket",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(onClick = { /* Handle Notifications */ }) {
            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Color.DarkGray)
        }
        
        IconButton(onClick = onCartClick) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart", tint = Color.DarkGray)
        }
    }
    
    // Search Bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Search textbooks, electronics...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ProfileAvatarBorder,
                unfocusedContainerColor = ProfileAvatarBorder,
                disabledContainerColor = ProfileAvatarBorder,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun HomeBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(TagBlue, SecondaryBlue)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.6f)
        ) {
            Box(
                modifier = Modifier
                    .background(PrimaryYellowDark, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Back to School",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Up to 50% off\nUsed Textbooks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        // Pager indicators (static for now)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(PrimaryYellowDark, RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(6.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(6.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
fun CategoriesSection(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "See all",
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryYellowDark,
            modifier = Modifier.clickable { }
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedCategoryId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) PrimaryYellowDark else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else DividerColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onCategorySelected(category.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = category.name,
                    color = if (isSelected) Color.Black else Color.DarkGray,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun RecommendedProductsGrid(
    products: List<Product>,
    onProductClick: (String) -> Unit
) {
    // Basic implementation of a 2-column grid using rows for scrollability context
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        for (i in products.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProductCard(
                        product = products[i],
                        onClick = { onProductClick(products[i].id) }
                    )
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    if (i + 1 < products.size) {
                        ProductCard(
                            product = products[i + 1],
                            onClick = { onProductClick(products[i + 1].id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
