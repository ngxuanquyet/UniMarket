package com.example.unimarket.presentation.sell

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.product.AddProductUseCase
import com.example.unimarket.domain.usecase.image.UploadImageUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class SellViewModel @Inject constructor(
    private val addProductUseCase: AddProductUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
): ViewModel() {
    
    private val _uiState = MutableStateFlow(SellUiState())
    val uiState: StateFlow<SellUiState> = _uiState.asStateFlow()

    fun updateSelectedImages(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedImageUris = uris, errorMessage = null)
    }

    fun postListing(
        title: String,
        priceStr: String,
        description: String,
        category: String,
        condition: String,
        isNegotiable: Boolean
    ) {
        val uris = _uiState.value.selectedImageUris
        if (uris.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select at least one image")
            return
        }

        if (title.isBlank() || priceStr.isBlank() || category == "Select Category" || condition == "Select Condition") {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all required fields")
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid price format")
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
                val uploadResult = uploadImageUseCase(uri)
                if (uploadResult.isSuccess) {
                    uploadedUrls.add(uploadResult.getOrNull()!!)
                } else {
                    uploadError = uploadResult.exceptionOrNull()?.message
                    break
                }
            }

            if (uploadError != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to upload image: $uploadError"
                )
                return@launch
            }
            
            // 2. Save product to Firestore
            val product = Product(
                id = UUID.randomUUID().toString(), // Generate a unique ID if needed, though Firestore does this automatically on add(). Using UUID for the model placeholder. 
                name = title,
                price = price,
                imageUrls = uploadedUrls,
                categoryId = category, // In a real app, map category name back to ID. Using name as string for ease.
                condition = condition,
                sellerName = sellerName,
                rating = 0.0,
                location = "Unknown", // Add location picking feature later
                timeAgo = "Just now",
                isFavorite = false,
                isNegotiable = isNegotiable,
                userId = userId
            )

            val saveResult = addProductUseCase(product)
            saveResult.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Product listed successfully!",
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

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}