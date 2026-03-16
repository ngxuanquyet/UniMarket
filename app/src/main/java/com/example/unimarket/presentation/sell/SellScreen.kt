package com.example.unimarket.presentation.sell

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.presentation.theme.AppBlue
import com.example.unimarket.presentation.theme.BorderLightBlue
import com.example.unimarket.presentation.theme.DashColor
import com.example.unimarket.presentation.theme.LightBlueAction
import com.example.unimarket.presentation.theme.LightBlueSelection
import com.example.unimarket.presentation.theme.SellTopBarBg
import com.example.unimarket.presentation.theme.SlateGrey
import com.example.unimarket.presentation.theme.SurfaceLightBlue
import com.example.unimarket.presentation.theme.TextDarkBlack
import com.example.unimarket.presentation.theme.TextGray
import androidx.compose.ui.text.input.KeyboardType

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
    var price by remember(initialProduct) {
        mutableStateOf(
            initialProduct?.price?.toString() ?: ""
        )
    }
    var description by remember(initialProduct) { mutableStateOf(initialProduct?.description ?: "") }
    var quantity by remember(initialProduct) {
        mutableStateOf(initialProduct?.quantityAvailable?.toString() ?: "")
    }
    var isNegotiable by remember(initialProduct) {
        mutableStateOf(
            initialProduct?.isNegotiable ?: false
        )
    }

    var specCounter by remember { mutableIntStateOf(0) }
    val specifications = remember(initialProduct) {
        androidx.compose.runtime.mutableStateListOf<SpecItem>().apply {
            initialProduct?.specifications?.forEach { (k, v) ->
                add(SpecItem(specCounter++, k, v))
            }
        }
    }

    val categories = listOf("Electronics", "Textbooks", "Furniture", "Clothing", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }
    var category by remember(initialProduct) {
        mutableStateOf(
            initialProduct?.categoryId?.takeIf { it.isNotBlank() } ?: "Select a category"
        )
    }

    val conditions = listOf("New", "Like New", "Good", "Fair")
    var condition by remember(initialProduct) { mutableStateOf(initialProduct?.condition ?: "") }
    val selectedDeliveryMethods = remember(initialProduct) {
        androidx.compose.runtime.mutableStateListOf<DeliveryMethod>().apply {
            addAll(initialProduct?.deliveryMethodsAvailable ?: emptyList())
        }
    }

    var pickingIndex by remember { mutableIntStateOf(-1) }

//    val imagePickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
//    ) { uris ->
//        if (uris.isNotEmpty()) {
//            viewModel.updateSelectedImages(uris.take(6))
//        }
//    }

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
            quantity = ""
            category = "Select a category"
            condition = "New"
            isNegotiable = false
            specCounter = 0
            specifications.clear()
            selectedDeliveryMethods.clear()
            viewModel.clearMessages()
        }
        if (uiState.errorMessage != null) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    var showDraftDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = if (initialProduct != null && viewModel.isEditingDraft) {
        val initialUris = initialProduct.imageUrls
        val currentUris = uiState.selectedImageUris.map { it.toString() }
        
        title != initialProduct.name ||
        price != initialProduct.price.toString() ||
        description != initialProduct.description ||
        quantity != initialProduct.quantityAvailable.toString() ||
        category != (initialProduct.categoryId.takeIf { it.isNotBlank() } ?: "Select a category") ||
        condition != initialProduct.condition ||
        isNegotiable != initialProduct.isNegotiable ||
        selectedDeliveryMethods.toList() != initialProduct.deliveryMethodsAvailable ||
        currentUris != initialUris ||
        specifications.associate { it.key to it.value } != initialProduct.specifications
    } else if (initialProduct == null) {
        title.isNotBlank() || price.isNotBlank() || description.isNotBlank() || quantity.isNotBlank() || uiState.selectedImageUris.isNotEmpty() || specifications.isNotEmpty() || selectedDeliveryMethods.isNotEmpty()
    } else {
        false
    }

    val handleBackClick = {
        if (hasUnsavedChanges && (initialProduct == null || viewModel.isEditingDraft)) {
            showDraftDialog = true
        } else {
            onBackClick()
        }
    }

    BackHandler {
        handleBackClick()
    }

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("Save Draft?", fontWeight = FontWeight.Bold, color = TextDarkBlack) },
            text = { 
                Text(
                    if (initialProduct != null) "You have unsaved changes. Do you want to save the changes to this draft?" 
                    else "You have unsaved changes. Do you want to save this listing as a draft?", 
                    color = TextGray
                ) 
            },
            confirmButton = {
                TextButton(onClick = {
                    showDraftDialog = false
                    viewModel.saveAsDraft(
                        title = title,
                        priceStr = price,
                        description = description,
                        categoryId = category,
                        condition = condition,
                        quantityStr = quantity,
                        isNegotiable = isNegotiable,
                        specifications = specifications.filter { it.key.isNotBlank() }.associate { it.key to it.value },
                        deliveryMethodsAvailable = selectedDeliveryMethods.toList(),
                        onDraftSaved = { onBackClick() }
                    )
                }) {
                    Text("Save", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDraftDialog = false
                    onBackClick()
                }) {
                    Text("Discard", color = Color.Red)
                }
            }
        )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackClick() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDarkBlack
                        )
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
                    onClick = {
                        viewModel.postListing(
                            title,
                            price,
                            description,
                            category,
                            condition,
                            quantity,
                            isNegotiable,
                            specifications.filter { it.key.isNotBlank() }.associate { it.key to it.value },
                            selectedDeliveryMethods.toList()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerticalAlignTop,
                                contentDescription = "Upload",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (productId != null) "Update Item" else "Post Item",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Huge Image (Pill shaped)
                val firstImage = uiState.selectedImageUris.getOrNull(0)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            pickingIndex = 0
                            singleImagePickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                        .drawBehind { // Draw dashed border for pill
                            if (firstImage == null) {
                                drawRoundRect(
                                    color = DashColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(
                                                15f,
                                                15f
                                            ), 0f
                                        )
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
                        AsyncImage(
                            model = firstImage,
                            contentDescription = "Main Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Right 2x2 grid
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(1),
                            onClick = {
                                pickingIndex = 1
                                singleImagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(2),
                            onClick = {
                                pickingIndex = 2
                                singleImagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            isAdd = uiState.selectedImageUris.size <= 2,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    // Bottom Row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(3),
                            onClick = {
                                pickingIndex = 3
                                singleImagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            isAdd = uiState.selectedImageUris.size <= 3,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(4),
                            onClick = {
                                pickingIndex = 4
                                singleImagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            isAdd = uiState.selectedImageUris.size <= 4,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title Input
            Text(
                "Item Title",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
            CustomTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "What are you selling?"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Dropdown
            Text(
                "Category",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { categoryExpanded = true },
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            category,
                            color = if (category == "Select a category") Color.Gray else TextDarkBlack,
                            fontSize = 16.sp
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.Gray
                        )
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
            Text(
                "Price",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomTextField(
                    value = price,
                    onValueChange = { price = it },
                    placeholder = "0 VND",
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
                Text(
                    "Negotiable",
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Quantity",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
            CustomTextField(
                value = quantity,
                onValueChange = { input -> quantity = input.filter(Char::isDigit) },
                placeholder = "Enter available quantity",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Item Condition
            Text(
                "Item Condition",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AppBlue else BorderLightBlue
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = cond,
                                color = if (isSelected) LightBlueAction else TextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Delivery Methods Available",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
            Text(
                "Chon mot hoac nhieu phuong thuc ma ban co the ho tro.",
                color = TextGray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryMethod.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { method ->
                            FilterChip(
                                selected = selectedDeliveryMethods.contains(method),
                                onClick = {
                                    if (selectedDeliveryMethods.contains(method)) {
                                        selectedDeliveryMethods.remove(method)
                                    } else {
                                        selectedDeliveryMethods.add(method)
                                    }
                                },
                                label = { Text(method.title) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description Input
            Text(
                "Description",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = TextDarkBlack
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text(
                        "Describe your item, including any flaws,\ndimensions, or specific details buyers\nshould know...",
                        color = Color.Gray.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderLightBlue,
                    focusedBorderColor = AppBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Specifications
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Specifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDarkBlack
                )
                TextButton(onClick = { specifications.add(SpecItem(specCounter++, "", "")) }) {
                    Text("+ Add Spec", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                specifications.forEachIndexed { index, spec ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomTextField(
                            value = spec.key,
                            onValueChange = { specifications[index] = spec.copy(key = it) },
                            placeholder = "e.g. Brand",
                            modifier = Modifier.weight(1f)
                        )
                        CustomTextField(
                            value = spec.value,
                            onValueChange = { specifications[index] = spec.copy(value = it) },
                            placeholder = "e.g. Apple",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { specifications.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = BorderLightBlue,
            focusedBorderColor = AppBlue,
        ),
        keyboardOptions = keyboardOptions,
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
            AsyncImage(
                model = uri,
                contentDescription = "Small Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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

data class SpecItem(val id: Int, val key: String, val value: String)
