package com.example.unimarket.presentation.sell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.presentation.theme.PrimaryYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen() {
    val scrollState = rememberScrollState()
    
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Select Category") }
    var condition by remember { mutableStateOf("Select Condition") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "List an Item", 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* Close action */ }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(onClick = { /* Post action */ }) {
                        Text("Post", fontWeight = FontWeight.Bold, color = PrimaryYellowDark, fontSize = 16.sp)
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            
            // Photo Upload Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add Photos",
                        tint = PrimaryYellowDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add up to 10 photos", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title Input
            Text("Title", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What are you selling?", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = PrimaryYellowDark
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Price Input
            Text("Price", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                placeholder = { Text("$ 0.00", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = PrimaryYellowDark
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Dropdown (Mocked)
            Text("Category", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, color = if (category == "Select Category") Color.Gray else Color.Black)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Condition Dropdown (Mocked)
            Text("Condition", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(condition, color = if (condition == "Select Condition") Color.Gray else Color.Black)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description Input
            Text("Description", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Describe your item (brand, size, flaws, etc.)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = PrimaryYellowDark
                ),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { /* Handle Post */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellowDark),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Post Listing", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom nav bar
        }
    }
}
