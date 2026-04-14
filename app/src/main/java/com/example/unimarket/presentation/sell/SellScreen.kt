package com.example.unimarket.presentation.sell

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.unimarket.R
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.UserAddress
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
import com.example.unimarket.presentation.util.localizedCategoryLabel
import com.example.unimarket.presentation.util.localizedConditionLabel
import com.example.unimarket.presentation.util.localizedTitle
import androidx.compose.ui.text.input.KeyboardType
import java.io.File

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
    var pickupAddressSource by remember(initialProduct?.id) {
        mutableStateOf(PickupAddressSource.MY_ADDRESSES)
    }
    var selectedPickupAddressId by remember(initialProduct?.id) { mutableStateOf("") }
    var customPickupRecipientName by remember(initialProduct?.id) { mutableStateOf("") }
    var customPickupPhoneNumber by remember(initialProduct?.id) { mutableStateOf("") }
    var customPickupAddressLine by remember(initialProduct?.id) { mutableStateOf("") }

    var pickingIndex by remember { mutableIntStateOf(-1) }
    var showImageSourceSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = pendingCameraUri
        if (success && capturedUri != null && pickingIndex != -1) {
            viewModel.updateImageAtIndex(capturedUri, pickingIndex)
        }
        pendingCameraUri = null
    }
    val openImageSourcePicker = { index: Int ->
        pickingIndex = index
        showImageSourceSheet = true
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val successMessage = uiState.successMessage
        val errorMessage = uiState.errorMessage

        if (successMessage != null) {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            if (!successMessage.contains("AI updated", ignoreCase = true)) {
                title = ""
                price = ""
                description = ""
                quantity = ""
                category = "Select a category"
                condition = "New"
                specCounter = 0
                specifications.clear()
                selectedDeliveryMethods.clear()
                pickupAddressSource = PickupAddressSource.MY_ADDRESSES
                selectedPickupAddressId = uiState.myAddresses.firstOrNull { it.isDefault }?.id
                    ?: uiState.myAddresses.firstOrNull()?.id
                    ?: ""
                customPickupRecipientName = ""
                customPickupPhoneNumber = ""
                customPickupAddressLine = ""
            }
            viewModel.clearMessages()
        }
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.aiSuggestion) {
        uiState.aiSuggestion?.let { suggestion ->
            if (suggestion.title.isNotBlank()) {
                title = suggestion.title
            }
            if (suggestion.description.isNotBlank()) {
                description = suggestion.description
            }
            if (suggestion.specifications.isNotEmpty()) {
                specifications.clear()
                suggestion.specifications.forEach { (key, value) ->
                    specifications.add(SpecItem(specCounter++, key, value))
                }
            }
            viewModel.consumeAiSuggestion()
        }
    }

    LaunchedEffect(uiState.aiImageSuggestion) {
        uiState.aiImageSuggestion?.let { suggestion ->
            if (suggestion.title.isNotBlank()) {
                title = suggestion.title
            }
            if (suggestion.description.isNotBlank()) {
                description = suggestion.description
            }
            mapSuggestedCategoryToSellCategory(suggestion.category)?.let { mappedCategory ->
                category = mappedCategory
            }
            if (suggestion.specifications.isNotEmpty()) {
                specifications.clear()
                suggestion.specifications.forEach { (key, value) ->
                    specifications.add(SpecItem(specCounter++, key, value))
                }
            }
            viewModel.consumeAiImageSuggestion()
        }
    }

    LaunchedEffect(initialProduct?.id, uiState.myAddresses) {
        val initialPickupAddress = initialProduct?.sellerPickupAddress
        val matchedProfileAddressId = initialPickupAddress?.id
            ?.takeIf { addressId -> uiState.myAddresses.any { it.id == addressId } }
            .orEmpty()

        pickupAddressSource = when {
            initialPickupAddress == null -> PickupAddressSource.MY_ADDRESSES
            matchedProfileAddressId.isNotBlank() -> PickupAddressSource.MY_ADDRESSES
            else -> PickupAddressSource.OTHER_ADDRESS
        }

        selectedPickupAddressId = matchedProfileAddressId.ifBlank {
            uiState.myAddresses.firstOrNull { it.isDefault }?.id
                ?: uiState.myAddresses.firstOrNull()?.id
                ?: ""
        }

        customPickupRecipientName = if (matchedProfileAddressId.isBlank()) {
            initialPickupAddress?.recipientName.orEmpty()
        } else {
            ""
        }
        customPickupPhoneNumber = if (matchedProfileAddressId.isBlank()) {
            initialPickupAddress?.phoneNumber.orEmpty()
        } else {
            ""
        }
        customPickupAddressLine = if (matchedProfileAddressId.isBlank()) {
            initialPickupAddress?.addressLine.orEmpty()
        } else {
            ""
        }
    }

    val selectedMyPickupAddress = uiState.myAddresses.firstOrNull { it.id == selectedPickupAddressId }
    val resolvedSellerPickupAddress = when {
        !selectedDeliveryMethods.contains(DeliveryMethod.BUYER_TO_SELLER) -> null
        pickupAddressSource == PickupAddressSource.MY_ADDRESSES -> selectedMyPickupAddress
        customPickupRecipientName.isBlank() || customPickupPhoneNumber.isBlank() || customPickupAddressLine.isBlank() -> null
        else -> UserAddress(
            recipientName = customPickupRecipientName.trim(),
            phoneNumber = customPickupPhoneNumber.trim(),
            addressLine = customPickupAddressLine.trim(),
            isDefault = false
        )
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
        selectedDeliveryMethods.toList() != initialProduct.deliveryMethodsAvailable ||
        resolvedSellerPickupAddress != initialProduct.sellerPickupAddress ||
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
    val submitListing = {
        viewModel.postListing(
            title,
            price,
            description,
            category,
            condition,
            quantity,
            false,
            specifications.filter { it.key.isNotBlank() }.associate { it.key to it.value },
            selectedDeliveryMethods.toList(),
            resolvedSellerPickupAddress
        )
    }

    BackHandler {
        handleBackClick()
    }

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = {
                Text(
                    stringResource(R.string.sell_save_draft_title),
                    fontWeight = FontWeight.Bold,
                    color = TextDarkBlack
                )
            },
            text = {
                Text(
                    if (initialProduct != null) {
                        stringResource(R.string.sell_save_draft_message_existing)
                    } else {
                        stringResource(R.string.sell_save_draft_message_new)
                    },
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
                        isNegotiable = false,
                        specifications = specifications.filter { it.key.isNotBlank() }.associate { it.key to it.value },
                        deliveryMethodsAvailable = selectedDeliveryMethods.toList(),
                        sellerPickupAddress = resolvedSellerPickupAddress,
                        onDraftSaved = { onBackClick() }
                    )
                }) {
                    Text(stringResource(R.string.common_save), color = AppBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDraftDialog = false
                    onBackClick()
                }) {
                    Text(stringResource(R.string.common_discard), color = Color.Red)
                }
            }
        )
    }

    if (showImageSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.sell_add_photo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkBlack
                )
                Spacer(modifier = Modifier.height(16.dp))
                SourceOptionRow(
                    title = stringResource(R.string.sell_choose_from_gallery),
                    onClick = {
                        showImageSourceSheet = false
                        singleImagePickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
                SourceOptionRow(
                    title = stringResource(R.string.sell_take_photo),
                    onClick = {
                        showImageSourceSheet = false
                        val uri = createSellPhotoUri(context)
                        if (uri == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.sell_unable_open_camera),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (productId != null) {
                            stringResource(R.string.sell_edit_item)
                        } else {
                            stringResource(R.string.sell_sell_item)
                        },
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
                            contentDescription = stringResource(R.string.common_back),
                            tint = TextDarkBlack
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = submitListing,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppBlue,
                            contentColor = Color.White,
                            disabledContainerColor = AppBlue.copy(alpha = 0.7f),
                            disabledContentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerticalAlignTop,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (productId != null) {
                                        stringResource(R.string.common_update)
                                    } else {
                                        stringResource(R.string.common_post)
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SellTopBarBg)
            )
        },
        containerColor = SellTopBarBg
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
                        .clickable { openImageSourcePicker(0) }
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
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(80.dp))
                            .background(if (firstImage == null) SurfaceLightBlue else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (firstImage != null) {
                            AsyncImage(
                                model = firstImage,
                                contentDescription = stringResource(R.string.sell_main_photo),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.sell_main_photo),
                                tint = SlateGrey.copy(alpha = 0.75f),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    if (firstImage != null) {
                        ImageRemoveButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp),
                            onClick = { viewModel.removeImageAtIndex(0) }
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
                            onClick = { openImageSourcePicker(1) },
                            onRemove = { viewModel.removeImageAtIndex(1) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(2),
                            onClick = { openImageSourcePicker(2) },
                            onRemove = { viewModel.removeImageAtIndex(2) },
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
                            onClick = { openImageSourcePicker(3) },
                            onRemove = { viewModel.removeImageAtIndex(3) },
                            isAdd = uiState.selectedImageUris.size <= 3,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SmallImageBox(
                            uri = uiState.selectedImageUris.getOrNull(4),
                            onClick = { openImageSourcePicker(4) },
                            onRemove = { viewModel.removeImageAtIndex(4) },
                            isAdd = uiState.selectedImageUris.size <= 4,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.generateListingSuggestionFromImage(
                        title = title,
                        description = description,
                        category = category,
                        condition = condition,
                        specifications = specifications
                            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
                            .associate { it.key to it.value }
                    )
                },
                enabled = uiState.selectedImageUris.isNotEmpty() && !uiState.isGeneratingWithAiFromImage && !uiState.isLoading,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isGeneratingWithAiFromImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.sell_ai_autofill_image),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sell_ai_autofill_image),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderLightBlue, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // Title Input
                    Text(
                        stringResource(R.string.sell_item_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = TextDarkBlack
                    )
                    CustomTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = stringResource(R.string.sell_item_title_placeholder)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category Dropdown
                    Text(
                        stringResource(R.string.sell_category),
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
                                    localizedCategoryLabel(category),
                                    color = if (category == "Select a category") Color.Gray else TextDarkBlack,
                                    fontSize = 16.sp
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.sell_open_category_dropdown),
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
                                    text = { Text(localizedCategoryLabel(selection)) },
                                    onClick = {
                                        category = selection
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderLightBlue, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // Price
                    Text(
                        stringResource(R.string.sell_price),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = TextDarkBlack
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CustomTextField(
                            value = price,
                            onValueChange = { input -> price = input.filter(Char::isDigit) },
                            placeholder = stringResource(R.string.sell_price_placeholder),
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        stringResource(R.string.sell_quantity),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = TextDarkBlack
                    )
                    CustomTextField(
                        value = quantity,
                        onValueChange = { input -> quantity = input.filter(Char::isDigit) },
                        placeholder = stringResource(R.string.sell_quantity_placeholder),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderLightBlue, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // Item Condition
                    Text(
                        stringResource(R.string.sell_item_condition),
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
                                color = if (isSelected) AppBlue else SurfaceLightBlue
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = localizedConditionLabel(cond),
                                        color = if (isSelected) Color.White else TextGray,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        stringResource(R.string.sell_delivery_methods),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = TextDarkBlack
                    )
                    Text(
                        stringResource(R.string.sell_delivery_methods_hint),
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
                                    val isSelected = selectedDeliveryMethods.contains(method)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (isSelected) {
                                                    selectedDeliveryMethods.remove(method)
                                                } else {
                                                    selectedDeliveryMethods.add(method)
                                                }
                                            },
                                        color = if (isSelected) AppBlue else SurfaceLightBlue
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = method.localizedTitle(),
                                                color = if (isSelected) Color.White else TextGray,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (selectedDeliveryMethods.contains(DeliveryMethod.BUYER_TO_SELLER)) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            stringResource(R.string.sell_pickup_address),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = TextDarkBlack
                        )
                        Text(
                            stringResource(R.string.sell_pickup_address_hint),
                            color = TextGray,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PickupAddressOptionChip(
                                title = stringResource(R.string.sell_pickup_source_my_addresses),
                                selected = pickupAddressSource == PickupAddressSource.MY_ADDRESSES,
                                onClick = { pickupAddressSource = PickupAddressSource.MY_ADDRESSES },
                                modifier = Modifier.weight(1f)
                            )
                            PickupAddressOptionChip(
                                title = stringResource(R.string.sell_pickup_source_other_address),
                                selected = pickupAddressSource == PickupAddressSource.OTHER_ADDRESS,
                                onClick = { pickupAddressSource = PickupAddressSource.OTHER_ADDRESS },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (pickupAddressSource == PickupAddressSource.MY_ADDRESSES) {
                            if (uiState.myAddresses.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.sell_no_saved_addresses),
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.myAddresses.forEach { address ->
                                        PickupAddressSelectionCard(
                                            address = address,
                                            isSelected = selectedPickupAddressId == address.id,
                                            onClick = { selectedPickupAddressId = address.id }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CustomTextField(
                                    value = customPickupRecipientName,
                                    onValueChange = { customPickupRecipientName = it },
                                    placeholder = stringResource(R.string.sell_recipient_name_placeholder)
                                )
                                CustomTextField(
                                    value = customPickupPhoneNumber,
                                    onValueChange = { customPickupPhoneNumber = it },
                                    placeholder = stringResource(R.string.sell_phone_number_placeholder),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                )
                                OutlinedTextField(
                                    value = customPickupAddressLine,
                                    onValueChange = { customPickupAddressLine = it },
                                    placeholder = { Text(stringResource(R.string.sell_pickup_address_placeholder)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = BorderLightBlue,
                                        focusedBorderColor = AppBlue,
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White
                                    ),
                                    minLines = 3
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderLightBlue, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // Description Input
                    Text(
                        stringResource(R.string.sell_description),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = TextDarkBlack
                    )
                    Button(
                        onClick = {
                            viewModel.generateListingSuggestion(
                                title = title,
                                description = description,
                                category = category,
                                condition = condition,
                                priceStr = price,
                                quantityStr = quantity,
                                isNegotiable = false,
                                specifications = specifications
                                    .filter { it.key.isNotBlank() && it.value.isNotBlank() }
                                    .associate { it.key to it.value },
                                deliveryMethodsAvailable = selectedDeliveryMethods.toList()
                            )
                        },
                        enabled = !uiState.isGeneratingWithAi && !uiState.isLoading,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightBlueAction)
                    ) {
                        if (uiState.isGeneratingWithAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = stringResource(R.string.sell_ai_write),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.sell_ai_write),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.sell_description_placeholder),
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Specifications
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.sell_specifications),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDarkBlack
                        )
                        TextButton(onClick = { specifications.add(SpecItem(specCounter++, "", "")) }) {
                            Text(stringResource(R.string.sell_add_spec), color = AppBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        specifications.forEachIndexed { index, spec ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceLightBlue)
                                    .border(1.dp, BorderLightBlue, RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CustomTextField(
                                        value = spec.key,
                                        onValueChange = { specifications[index] = spec.copy(key = it) },
                                        placeholder = stringResource(R.string.sell_spec_key_placeholder),
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomTextField(
                                        value = spec.value,
                                        onValueChange = { specifications[index] = spec.copy(value = it) },
                                        placeholder = stringResource(R.string.sell_spec_value_placeholder),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { specifications.removeAt(index) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.common_remove),
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
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
private fun SourceOptionRow(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLightBlue)
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = title,
                color = TextDarkBlack,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun createSellPhotoUri(context: android.content.Context): Uri? {
    return runCatching {
        val imageDir = File(context.cacheDir, "sell_images").apply { mkdirs() }
        val imageFile = File.createTempFile("sell_capture_", ".jpg", imageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }.getOrNull()
}

private fun mapSuggestedCategoryToSellCategory(rawCategory: String): String? {
    val normalized = rawCategory.trim().lowercase()
    if (normalized.isBlank()) return null
    return when {
        normalized.contains("electronic") || normalized.contains("smartphone") || normalized.contains("phone") ||
            normalized.contains("laptop") || normalized.contains("tablet") || normalized.contains("headphone") ||
            normalized.contains("camera") -> "Electronics"
        normalized.contains("textbook") || normalized.contains("book") || normalized.contains("notebook") -> "Textbooks"
        normalized.contains("furniture") || normalized.contains("chair") || normalized.contains("desk") ||
            normalized.contains("table") || normalized.contains("sofa") -> "Furniture"
        normalized.contains("clothing") || normalized.contains("shirt") || normalized.contains("pants") ||
            normalized.contains("jacket") || normalized.contains("shoe") || normalized.contains("fashion") -> "Clothing"
        normalized.contains("other") -> "Other"
        else -> null
    }
}

private enum class PickupAddressSource {
    MY_ADDRESSES,
    OTHER_ADDRESS
}

@Composable
private fun PickupAddressOptionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) AppBlue else SurfaceLightBlue
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (selected) Color.White else TextGray,
                fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PickupAddressSelectionCard(
    address: UserAddress,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isSelected) AppBlue else BorderLightBlue,
                RoundedCornerShape(16.dp)
            )
            .background(if (isSelected) LightBlueSelection else Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = address.recipientName.ifBlank { stringResource(R.string.sell_my_address_fallback) },
                    fontWeight = FontWeight.SemiBold,
                    color = TextDarkBlack
                )
                if (address.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.common_default),
                        color = AppBlue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (address.phoneNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.phoneNumber,
                    color = TextGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = address.addressLine,
                color = TextGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SmallImageBox(
    uri: Uri?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isAdd: Boolean = false,
    modifier: Modifier
) {
    val smallCorner = 40.dp
    Box(
        modifier = modifier
            .clickable { onClick() }
            .drawBehind {
                if (uri == null) {
                    drawRoundRect(
                        color = DashColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(15f, 15f),
                                0f
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            smallCorner.toPx(),
                            smallCorner.toPx()
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(smallCorner))
                .background(if (uri == null) SurfaceLightBlue else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.sell_small_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector =Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = SlateGrey,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (uri != null) {
            ImageRemoveButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun ImageRemoveButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Red)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.common_remove_image),
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

data class SpecItem(val id: Int, val key: String, val value: String)
