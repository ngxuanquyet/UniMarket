package com.example.unimarket.presentation.profile

import com.example.unimarket.domain.model.UserAddress

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val boughtCount: Int = 0,
    val soldCount: Int = 0,
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val addresses: List<UserAddress> = emptyList(),
    val isUploading: Boolean = false,
    val isRefreshingProfile: Boolean = false,
    val isLoadingAddresses: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
