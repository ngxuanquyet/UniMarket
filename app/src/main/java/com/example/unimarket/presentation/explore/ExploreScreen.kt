package com.example.unimarket.presentation.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.presentation.theme.PrimaryYellowDark
import com.example.unimarket.presentation.theme.SecondaryBlue

data class ExploreProduct(
    val id: String,
    val name: String,
    val price: Double,
    val condition: String,
    val imageUrl: String,
    val isFavorite: Boolean,
    val tag: String? = null // e.g., "PRICE DROP", "NEW"
)

val exploreCategories = listOf("All", "Books", "Laptops", "Dorm Decor", "Clothing")

val exploreProducts = listOf(
    ExploreProduct(
        id = "1",
        name = "Calculus Early Transcendentals",
        price = 25.00,
        condition = "Excellent condition",
        imageUrl = "https://picsum.photos/seed/calcbook/400/400",
        isFavorite = false,
        tag = "PRICE DROP"
    ),
    ExploreProduct(
        id = "2",
        name = "MacBook Air M1 2020",
        price = 450.00,
        condition = "Minor scratches",
        imageUrl = "https://picsum.photos/seed/macbookM1/400/400",
        isFavorite = false,
        tag = "NEW"
    ),
    ExploreProduct(
        id = "3",
        name = "IKEA Tertial Desk Lamp",
        price = 15.00,
        condition = "Like new",
        imageUrl = "https://picsum.photos/seed/ikealamp/400/400",
        isFavorite = true
    ),
    ExploreProduct(
        id = "4",
        name = "Schwinn Commuter Bike",
        price = 120.00,
        condition = "Needs new tires",
        imageUrl = "https://picsum.photos/seed/bike2/400/400",
        isFavorite = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Explore", fontWeight = FontWeight.Bold, fontSize = 24.sp) 
                },
                actions = {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F0F0))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.DarkGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Find second-hand items", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF4F6F9),
                        focusedContainerColor = Color(0xFFF4F6F9),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Location Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1F5FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFF03A9F4),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Location", color = Color.Gray, fontSize = 12.sp)
                    Text("University Campus North", fontWeight = FontWeight.Medium)
                }
            }

            // Categories Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exploreCategories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SecondaryBlue else Color(0xFFF4F6F9))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Product Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(exploreProducts) { product ->
                    ExploreProductCard(product)
                }
            }
        }
    }
}

@Composable
fun ExploreProductCard(product: ExploreProduct) {
    var isFavorite by remember { mutableStateOf(product.isFavorite) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF0F0F0))
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = product.imageUrl),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Tags (e.g., PRICE DROP, NEW)
            if (product.tag != null) {
                val tagColor = if (product.tag == "PRICE DROP") Color(0xFF00BFA5) else SecondaryBlue
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = product.tag,
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
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE53935) else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$${String.format("%.2f", product.price)}",
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
            text = product.condition,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
