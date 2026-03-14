package com.example.unimarket.presentation.messages

import com.example.unimarket.presentation.theme.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.unimarket.presentation.theme.SecondaryBlue

data class MessagePreview(
    val id: String,
    val userName: String,
    val userAvatarUrl: String,
    val lastMessage: String,
    val timestamp: String,
    val isUnread: Boolean,
    val itemName: String,
    val itemImageUrl: String,
    val itemIsSold: Boolean
)

val sampleMessages = listOf(
    MessagePreview(
        id = "1",
        userName = "Alice Walker",
        userAvatarUrl = "https://picsum.photos/seed/alice/100/100",
        lastMessage = "Are you still selling the textbook?",
        timestamp = "10:30 AM",
        isUnread = true,
        itemName = "Bio 101 Textbook",
        itemImageUrl = "https://picsum.photos/seed/book/100/100",
        itemIsSold = false
    ),
    MessagePreview(
        id = "2",
        userName = "Bob Smith",
        userAvatarUrl = "https://picsum.photos/seed/bob/100/100",
        lastMessage = "I can meet at the library around ...",
        timestamp = "Yesterday",
        isUnread = false,
        itemName = "IKEA Desk Lamp",
        itemImageUrl = "https://picsum.photos/seed/lamp/100/100",
        itemIsSold = false
    ),
    MessagePreview(
        id = "3",
        userName = "Charlie Davis",
        userAvatarUrl = "https://picsum.photos/seed/charlie/100/100",
        lastMessage = "Thanks! The calculator works gr...",
        timestamp = "Mon",
        isUnread = false,
        itemName = "TI-84 Graphing Calc",
        itemImageUrl = "https://picsum.photos/seed/calc/100/100",
        itemIsSold = true
    ),
    MessagePreview(
        id = "4",
        userName = "Diana Prince",
        userAvatarUrl = "https://picsum.photos/seed/diana/100/100",
        lastMessage = "Would you take 15.000 VND for it?",
        timestamp = "Sun",
        isUnread = false,
        itemName = "Mini Fridge",
        itemImageUrl = "https://picsum.photos/seed/fridge/100/100",
        itemIsSold = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen() {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Messages", 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    ) 
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
                    placeholder = { Text("Search conversations...", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
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

            // Messages List
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(sampleMessages) { message ->
                    MessageItem(message)
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: MessagePreview) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread indicator dot
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (message.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SecondaryBlue) // Soft blue dot
                    )
                }
            }

            // User Avatar
            Image(
                painter = rememberAsyncImagePainter(model = message.userAvatarUrl),
                contentDescription = message.userName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(DividerColor) // Fallback bg
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.isUnread) SecondaryBlue else Color.Gray,
                        fontWeight = if (message.isUnread) FontWeight.Medium else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUnread) Color.Black else Color.DarkGray,
                    fontWeight = if (message.isUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.itemName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Item Image / Sold badge
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = message.itemImageUrl),
                    contentDescription = message.itemName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(ProfileAvatarBorder) // placeholder bg
                )

                if (message.itemIsSold) {
                    // Gray overlay for sold items
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                    // "SOLD" badge
                    Box(
                        modifier = Modifier
                            .background(Color.Gray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SOLD",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        HorizontalDivider(
            color = ProfileAvatarBorder,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 82.dp) // align with text content roughly
        )
    }
}
