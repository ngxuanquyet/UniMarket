package com.example.unimarket.presentation.profile

import com.example.unimarket.domain.model.UserAddress

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val addresses: List<UserAddress> = emptyList(),
    val isUploading: Boolean = false,
    val isLoadingAddresses: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
