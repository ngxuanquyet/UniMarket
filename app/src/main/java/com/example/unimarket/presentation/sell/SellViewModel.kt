package com.example.unimarket.presentation.sell

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.AiListingInput
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.data.local.DraftProduct
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.GetUserAddressesUseCase
import com.example.unimarket.domain.usecase.ai.GenerateListingSuggestionUseCase
import com.example.unimarket.domain.usecase.draft.DeleteDraftUseCase
import com.example.unimarket.domain.usecase.draft.GetDraftByIdUseCase
import com.example.unimarket.domain.usecase.draft.SaveDraftUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.image.UploadImageUseCase
import com.example.unimarket.domain.usecase.product.AddProductUseCase
import com.example.unimarket.domain.usecase.product.UpdateProductUseCase
import com.example.unimarket.presentation.util.toRelativeTimeLabel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class SellViewModel @Inject constructor(
    private val addProductUseCase: AddProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserAddressesUseCase: GetUserAddressesUseCase,
    private val generateListingSuggestionUseCase: GenerateListingSuggestionUseCase,
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val getDraftByIdUseCase: GetDraftByIdUseCase,
    private val deleteDraftUseCase: DeleteDraftUseCase
): ViewModel() {
    
    private val _uiState = MutableStateFlow(SellUiState())
    val uiState: StateFlow<SellUiState> = _uiState.asStateFlow()

    private var editingProductId: String? = null
    var isEditingDraft: Boolean = false
        private set
    var initialProduct: Product? = null
        private set

    init {
        loadMyAddresses()
    }

    private fun loadMyAddresses() {
        viewModelScope.launch {
            getUserAddressesUseCase()
                .onSuccess { addresses ->
                    _uiState.value = _uiState.value.copy(myAddresses = addresses)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to load addresses"
                    )
                }
        }
    }

    fun setEditProductId(id: String?) {
        if (id == null || editingProductId == id) return
        editingProductId = id
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // 1. Fetch from active products first
            val products = getAllProductsUseCase().firstOrNull() ?: emptyList()
            var product = products.find { it.id == id }

            if (product != null) {
                isEditingDraft = false
                initialProduct = product
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedImageUris = product.imageUrls.map { Uri.parse(it) }
                )
            } else {
                // 2. Not found in active products, check drafts
                val draft = getDraftByIdUseCase(id)
                if (draft != null) {
                    isEditingDraft = true
                    // Map DraftProduct to Product to display in UI
                    initialProduct = Product(
                        id = draft.id,
                        name = draft.name,
                        price = draft.price ?: 0.0,
                        description = draft.description,
                        imageUrls = draft.imageUrls,
                        categoryId = draft.categoryId,
                        condition = draft.condition,
                        sellerName = "Draft",
                        rating = 0.0,
                        location = "",
                        timeAgo = "Draft",
                        postedAt = 0L,
                        isFavorite = false,
                        isNegotiable = draft.isNegotiable,
                        quantityAvailable = draft.quantityAvailable ?: 1,
                        userId = draft.userId,
                        specifications = draft.specifications,
                        deliveryMethodsAvailable = draft.deliveryMethodsAvailable,
                        sellerPickupAddress = draft.sellerPickupAddress
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedImageUris = draft.imageUrls.mapNotNull { 
                            try { Uri.parse(it) } catch (e: Exception) { null } 
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Product or Draft not found"
                    )
                }
            }
        }
    }

    fun updateSelectedImages(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedImageUris = uris, errorMessage = null)
    }

    fun updateImageAtIndex(uri: Uri, index: Int) {
        val currentUris = _uiState.value.selectedImageUris.toMutableList()
        if (index < currentUris.size) {
            currentUris[index] = uri
        } else {
            // If we're adding it at the end or at an index beyond current size
            currentUris.add(uri)
        }
        _uiState.value = _uiState.value.copy(selectedImageUris = currentUris.take(5), errorMessage = null)
    }

    fun removeImageAtIndex(index: Int) {
        val currentUris = _uiState.value.selectedImageUris.toMutableList()
        if (index !in currentUris.indices) return
        currentUris.removeAt(index)
        _uiState.value = _uiState.value.copy(selectedImageUris = currentUris, errorMessage = null)
    }

    fun generateListingSuggestion(
        title: String,
        description: String,
        category: String,
        condition: String,
        priceStr: String,
        quantityStr: String,
        isNegotiable: Boolean,
        specifications: Map<String, String>,
        deliveryMethodsAvailable: List<DeliveryMethod>
    ) {
        val hasEnoughContext = title.isNotBlank() ||
            description.isNotBlank() ||
            category != "Select a category" ||
            specifications.isNotEmpty()

        if (!hasEnoughContext) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Add at least a title, description, category, or specifications before using AI"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingWithAi = true,
                errorMessage = null
            )

            generateListingSuggestionUseCase(
                AiListingInput(
                    title = title.trim(),
                    description = description.trim(),
                    category = category.takeUnless { it == "Select a category" }.orEmpty(),
                    condition = condition.trim(),
                    price = priceStr.trim(),
                    quantity = quantityStr.trim(),
                    isNegotiable = isNegotiable,
                    specifications = specifications,
                    deliveryMethodsAvailable = deliveryMethodsAvailable
                )
            ).onSuccess { suggestion ->
                _uiState.value = _uiState.value.copy(
                    isGeneratingWithAi = false,
                    aiSuggestion = suggestion,
                    successMessage = "AI updated title, description, and specifications"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGeneratingWithAi = false,
                    errorMessage = error.message ?: "Failed to generate listing with AI"
                )
            }
        }
    }

    fun postListing(
        title: String,
        priceStr: String,
        description: String,
        category: String,
        condition: String,
        quantityStr: String,
        isNegotiable: Boolean,
        specifications: Map<String, String>,
        deliveryMethodsAvailable: List<DeliveryMethod>,
        sellerPickupAddress: UserAddress?
    ) {
        val uris = _uiState.value.selectedImageUris
        if (uris.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select at least one image")
            return
        }

        if (title.isBlank() || priceStr.isBlank() || description.isBlank() || category == "Select a category" || condition == "" || quantityStr.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all required fields")
            return
        }

        if (deliveryMethodsAvailable.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select at least one delivery method")
            return
        }

        if (deliveryMethodsAvailable.contains(DeliveryMethod.BUYER_TO_SELLER) && sellerPickupAddress == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please choose or enter a pickup address for buyer pickup"
            )
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid price format")
            return
        }

        val quantityAvailable = quantityStr.toIntOrNull()
        if (quantityAvailable == null || quantityAvailable <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Quantity must be a number greater than 0")
            return
        }

        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        val sellerName = currentUser?.displayName ?: "Anonymous"
        val userId = currentUser?.uid ?: ""

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            // 1. Upload images
            val uploadedUrls = mutableListOf<String>()
            var uploadError: String? = null

            for (uri in uris) {
                val uriStr = uri.toString()
                if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                    // This is an existing image URL from Firestore, no need to upload
                    uploadedUrls.add(uriStr)
                } else {
                    // This is a local file URI (e.g. content://...), upload it
                    val uploadResult = uploadImageUseCase(uri)
                    if (uploadResult.isSuccess) {
                        uploadedUrls.add(uploadResult.getOrNull()!!)
                    } else {
                        uploadError = uploadResult.exceptionOrNull()?.message
                        break
                    }
                }
            }

            if (uploadError != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to upload image: $uploadError"
                )
                Log.d("ccc", "${_uiState.value.errorMessage}")
                return@launch
            }
            
            // 2. Determine ID
            val productId = editingProductId ?: UUID.randomUUID().toString()
            val postedAt = initialProduct?.postedAt?.takeIf { it > 0L } ?: System.currentTimeMillis()
            val timeAgo = postedAt.toRelativeTimeLabel()

            // 3. Save product to Firestore
            val product = Product(
                id = productId,
                name = title,
                price = price,
                description = description,
                imageUrls = uploadedUrls,
                categoryId = category, // In a real app, map category name back to ID. Using name as string for ease.
                condition = condition,
                sellerName = sellerName,
                rating = initialProduct?.rating ?: 0.0,
                location = initialProduct?.location ?: "Unknown",
                timeAgo = timeAgo,
                postedAt = postedAt,
                isFavorite = initialProduct?.isFavorite ?: false,
                isNegotiable = isNegotiable,
                quantityAvailable = quantityAvailable,
                userId = userId,
                specifications = specifications,
                deliveryMethodsAvailable = deliveryMethodsAvailable,
                sellerPickupAddress = sellerPickupAddress
            )

            val saveResult = if (editingProductId != null && !isEditingDraft) {
                updateProductUseCase(product)
            } else {
                // If it is a draft, we post it as a new product, since drafts don't exist remotely
                addProductUseCase(product)
            }

            saveResult.onSuccess {
                // If we successfully posted a draft, delete the local draft database entry
                if (isEditingDraft && editingProductId != null) {
                    deleteDraftUseCase(editingProductId!!)
                    isEditingDraft = false
                    editingProductId = null
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (editingProductId != null && !isEditingDraft) "Product updated successfully!" else "Product listed successfully!",
                    selectedImageUris = emptyList()
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save product: ${error.message}"
                )
            }
        }
    }

    fun saveAsDraft(
        title: String,
        priceStr: String,
        description: String,
        categoryId: String,
        condition: String,
        quantityStr: String,
        isNegotiable: Boolean,
        specifications: Map<String, String>,
        deliveryMethodsAvailable: List<DeliveryMethod>,
        sellerPickupAddress: UserAddress?,
        onDraftSaved: () -> Unit
    ) {
        val currentUser = getCurrentUserUseCase() as? FirebaseUser ?: return
        val userId = currentUser.uid

        val uris = _uiState.value.selectedImageUris
        if (title.isBlank() && description.isBlank() && uris.isEmpty()) {
            // Nothing to save
            onDraftSaved()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // For local drafts, we just save the Uris directly as strings. The Repository handles persisting content Uris to internal storage.
            val imageUrls = uris.map { it.toString() }
            val price = priceStr.toDoubleOrNull()
            val quantityAvailable = quantityStr.toIntOrNull()

            val draftId = editingProductId ?: UUID.randomUUID().toString()

            val draftProduct = DraftProduct(
                id = draftId,
                userId = userId,
                name = title,
                price = price, // it's nullable in the model
                imageUrls = imageUrls,
                description = description,
                categoryId = if (categoryId == "Select a category") "" else categoryId,
                condition = condition,
                quantityAvailable = quantityAvailable,
                isNegotiable = isNegotiable,
                specifications = specifications,
                deliveryMethodsAvailable = deliveryMethodsAvailable,
                sellerPickupAddress = sellerPickupAddress
            )

            try {
                saveDraftUseCase(draftProduct)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Draft saved successfully"
                )
                onDraftSaved()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save draft: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    fun consumeAiSuggestion() {
        _uiState.value = _uiState.value.copy(aiSuggestion = null)
    }
}
