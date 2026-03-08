package com.example.unimarket.presentation.profile

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isUploading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
