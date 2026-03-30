package com.example.unimarket.presentation.sell

import android.net.Uri
import com.example.unimarket.domain.model.AiListingSuggestion

data class SellUiState(
    val selectedImageUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingWithAi: Boolean = false,
    val aiSuggestion: AiListingSuggestion? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
