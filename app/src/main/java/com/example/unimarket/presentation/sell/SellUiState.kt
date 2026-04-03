package com.example.unimarket.presentation.sell

import android.net.Uri
import com.example.unimarket.domain.model.AiImageListingSuggestion
import com.example.unimarket.domain.model.AiListingSuggestion
import com.example.unimarket.domain.model.UserAddress

data class SellUiState(
    val selectedImageUris: List<Uri> = emptyList(),
    val myAddresses: List<UserAddress> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingWithAi: Boolean = false,
    val isGeneratingWithAiFromImage: Boolean = false,
    val aiSuggestion: AiListingSuggestion? = null,
    val aiImageSuggestion: AiImageListingSuggestion? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
