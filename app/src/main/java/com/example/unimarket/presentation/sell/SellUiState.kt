package com.example.unimarket.presentation.sell

import android.net.Uri

data class SellUiState(
    val selectedImageUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
