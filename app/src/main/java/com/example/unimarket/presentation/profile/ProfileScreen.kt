package com.example.unimarket.presentation.profile

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.presentation.theme.PrimaryYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            ProfileHeader()
            
            Spacer(modifier = Modifier.height(24.dp))
            ProfileStatsRow()
            
            Spacer(modifier = Modifier.height(32.dp))
            ProfileActionsList()
            
            Spacer(modifier = Modifier.height(32.dp))
            LogoutButton()
            
            Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
        }
    }
}

@Composable
fun ProfileHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture with Edit Icon
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = rememberAsyncImagePainter(model = "https://picsum.photos/seed/alex/200/200"),
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFF0F0F0), CircleShape)
            )
            
            // Edit Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF03A9F4)),
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
        
        Text(
            text = "Alex Johnson",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "University of California, Berkeley",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Verified Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE8F5E9)) // Light green background
                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Verified Student",
                color = Color(0xFF4CAF50), // Green text
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(value = "15", label = "Items Sold", modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        StatCard(value = "8", label = "Bought", modifier = Modifier.weight(1f))
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
            .background(Color(0xFFF7F8FA))
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
            iconBgColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF2196F3)
        )
        ActionRowItem(
            title = "My Purchases",
            icon = Icons.Default.ShoppingBag,
            iconBgColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF2196F3)
        )
        ActionRowItem(
            title = "Saved Items",
            icon = Icons.Default.Favorite,
            iconBgColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF2196F3)
        )
        ActionRowItem(
            title = "Payment Methods",
            icon = Icons.Default.CreditCard,
            iconBgColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF2196F3)
        )
        ActionRowItem(
            title = "Account Settings",
            icon = Icons.Default.Settings,
            iconBgColor = Color(0xFFF0F0F0),
            iconColor = Color.DarkGray
        )
    }
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
            .background(Color(0xFFFFF0F0)) // Light red
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Log Out",
                tint = Color(0xFFE53935) // Reddish
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935)
            )
        }
    }
}
