package com.example.unimarket.presentation.sell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.widget.Toast
import com.example.unimarket.presentation.theme.PrimaryYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    viewModel: SellViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Mocked selections for now
    val categories = listOf("Electronics", "Textbooks", "Furniture", "Clothing", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Select Category") }
    
    val conditions = listOf("New", "Used - Like New", "Used - Good", "Used - Fair")
    var conditionExpanded by remember { mutableStateOf(false) }
    var condition by remember { mutableStateOf("Select Condition") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val current = uiState.selectedImageUris
        val combined = (current + uris).distinct().take(6)
        viewModel.updateSelectedImages(combined)
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        if (uiState.successMessage != null) {
            Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
            title = ""
            price = ""
            description = ""
            category = "Select Category"
            condition = "Select Condition"
            viewModel.clearMessages()
        }
        if (uiState.errorMessage != null) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

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
                    TextButton(
                        onClick = { viewModel.postListing(title, price, description, category, condition) },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryYellowDark, strokeWidth = 2.dp)
                        } else {
                            Text("Post", fontWeight = FontWeight.Bold, color = PrimaryYellowDark, fontSize = 16.sp)
                        }
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
            // Photo Upload Area
            if (uiState.selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize().padding(1.dp), // add 1dp padding so border isn't covered
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { 
                                    val newList = uiState.selectedImageUris.filter { it != uri }
                                    viewModel.updateSelectedImages(newList)
                                },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                            ) {
                                Box(modifier = Modifier.size(24.dp).background(Color.White, RoundedCornerShape(12.dp))) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Remove Image", tint = Color.Red, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                    if (uiState.selectedImageUris.size < 6) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
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
                                    Text("Add More", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
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
                        Text("Add Photos (Max 6)", color = Color.Gray, fontSize = 14.sp)
                    }
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

            // Category Dropdown (Mocked for single selection simply with clicking right now)
            Text("Category", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Box {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, color = if (category == "Select Category") Color.Gray else Color.Black)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categories.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    category = selection
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Condition Dropdown (Mocked)
            Text("Condition", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { conditionExpanded = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Box {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(condition, color = if (condition == "Select Condition") Color.Gray else Color.Black)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        conditions.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    condition = selection
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
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
                onClick = { viewModel.postListing(title, price, description, category, condition) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellowDark),
                shape = RoundedCornerShape(25.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Post Listing", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom nav bar
        }
    }
}
