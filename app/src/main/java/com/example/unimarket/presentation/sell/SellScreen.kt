package com.example.unimarket.presentation.sell

import com.example.unimarket.presentation.theme.*

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    productId: String? = null,
    onBackClick: () -> Unit,
    viewModel: SellViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(productId) {
        viewModel.setEditProductId(productId)
    }

    val initialProduct = viewModel.initialProduct
    var title by remember(initialProduct) { mutableStateOf(initialProduct?.name ?: "") }
    var price by remember(initialProduct) { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var description by remember(initialProduct) { mutableStateOf("") } // Doesn't exist on Product model right now, mock it or leave blank
    var isNegotiable by remember(initialProduct) { mutableStateOf(initialProduct?.isNegotiable ?: false) }
    
    val categories = listOf("Electronics", "Textbooks", "Furniture", "Clothing", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }
    var category by remember(initialProduct) { mutableStateOf(initialProduct?.categoryId ?: "Select a category") }
    
    val conditions = listOf("New", "Like New", "Good", "Fair")
    var condition by remember(initialProduct) { mutableStateOf(initialProduct?.condition ?: "New") }

    var pickingIndex by remember { mutableStateOf(-1) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.updateSelectedImages(uris.take(6))
        }
    }

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pickingIndex != -1) {
            viewModel.updateImageAtIndex(uri, pickingIndex)
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        if (uiState.successMessage != null) {
            Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
            title = ""
            price = ""
            description = ""
            category = "Select a category"
            condition = "New"
            isNegotiable = false
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
                        if (productId != null) "Edit Item" else "Sell an Item", 
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextDarkBlack,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDarkBlack)
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SellTopBarBg)
            )
        },
        containerColor = SellTopBarBg,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { viewModel.postListing(title, price, description, category, condition, isNegotiable) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerticalAlignTop, contentDescription = "Upload", tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (productId != null) "Update Item" else "Post Item", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            
            // Image Picker Custom Layout
            Row(modifier = Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Left Huge Image (Pill shaped)
                val firstImage = uiState.selectedImageUris.getOrNull(0)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { 
                            pickingIndex = 0
                            singleImagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                        }
                        .drawBehind { // Draw dashed border for pill
                            if (firstImage == null) {
                                drawRoundRect(
                                    color = DashColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(80.dp.toPx())
                                )
                            }
                        }
                        .clip(RoundedCornerShape(80.dp))
                        .background(if (firstImage == null) SurfaceLightBlue else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (firstImage != null) {
                        AsyncImage(model = firstImage, contentDescription = "Main Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }

                // Right 2x2 grid
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(1), 
                            onClick = { 
                                pickingIndex = 1
                                singleImagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }, 
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(2), 
                            onClick = { 
                                pickingIndex = 2
                                singleImagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }, 
                            isAdd = uiState.selectedImageUris.size <= 2, 
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    // Bottom Row
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(3), 
                            onClick = { 
                                pickingIndex = 3
                                singleImagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }, 
                            isAdd = uiState.selectedImageUris.size <= 3,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(4), 
                            onClick = { 
                                pickingIndex = 4
                                singleImagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }, 
                            isAdd = uiState.selectedImageUris.size <= 4,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title Input
            Text("Item Title", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp), color = TextDarkBlack)
            CustomTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "What are you selling?"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Dropdown
            Text("Category", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp), color = TextDarkBlack)
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clickable { categoryExpanded = true },
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, color = if (category == "Select a category") Color.Gray else TextDarkBlack, fontSize = 16.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                    }
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

            Spacer(modifier = Modifier.height(24.dp))

            // Price & Negotiable Row
            Text("Price", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp), color = TextDarkBlack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomTextField(
                    value = price,
                    onValueChange = { price = it },
                    placeholder = "$ 0.00",
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Switch(
                    checked = isNegotiable,
                    onCheckedChange = { isNegotiable = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Negotiable", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Item Condition
            Text("Item Condition", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp), color = TextDarkBlack)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                conditions.forEach { cond ->
                    val isSelected = condition == cond
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { condition = cond },
                        color = if (isSelected) LightBlueSelection else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AppBlue else BorderLightBlue)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(text = cond, color = if (isSelected) LightBlueAction else TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description Input
            Text("Description", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp), color = TextDarkBlack)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { 
                    Text(
                        "Describe your item, including any flaws,\ndimensions, or specific details buyers\nshould know...", 
                        color = Color.Gray.copy(alpha=0.7f),
                        lineHeight = 22.sp
                    ) 
                },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderLightBlue,
                    focusedBorderColor = AppBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray.copy(alpha=0.7f), fontSize = 16.sp) },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = BorderLightBlue,
            focusedBorderColor = AppBlue,
        ),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = TextDarkBlack)
    )
}

@Composable
fun SmallImageBox(uri: Uri?, onClick: () -> Unit, isAdd: Boolean = false, modifier: Modifier) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .drawBehind {
                if (uri == null) {
                    drawCircle(
                        color = DashColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        ),
                        radius = size.minDimension / 2
                    )
                }
            }
            .clip(CircleShape)
            .background(if (uri == null) SurfaceLightBlue else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = "Small Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            // Placeholder Icon
            Icon(
                imageVector = if (isAdd) Icons.Default.AddPhotoAlternate else Icons.Default.Image,
                contentDescription = null,
                tint = SlateGrey, // Slate grey
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
