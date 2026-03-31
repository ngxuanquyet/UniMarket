package com.example.unimarket.presentation.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.usecase.auth.AddUserAddressUseCase
import com.example.unimarket.domain.usecase.auth.DeleteUserAddressUseCase
import com.example.unimarket.domain.usecase.auth.GetUserAddressesUseCase
import com.example.unimarket.domain.usecase.auth.LogoutUseCase
import com.example.unimarket.domain.usecase.auth.ObserveCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.auth.UpdateProfileUseCase
import com.example.unimarket.domain.usecase.auth.UpdateUserAddressUseCase
import com.example.unimarket.domain.usecase.image.UploadImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeCachedUserUseCase: ObserveCachedUserUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getUserAddressesUseCase: GetUserAddressesUseCase,
    private val addUserAddressUseCase: AddUserAddressUseCase,
    private val updateUserAddressUseCase: UpdateUserAddressUseCase,
    private val deleteUserAddressUseCase: DeleteUserAddressUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCachedUserUseCase().collect { cachedUser ->
                if (cachedUser != null) {
                    _uiState.value = _uiState.value.copy(
                        displayName = cachedUser.displayName,
                        email = cachedUser.email,
                        avatarUrl = cachedUser.avatarUrl,
                        boughtCount = cachedUser.boughtCount,
                        soldCount = cachedUser.soldCount
                    )
                }
            }
        }

        refreshUserProfile(showLoading = false)
        loadAddresses()
    }

    fun refresh() {
        refreshUserProfile(showLoading = true)
        loadAddresses()
    }

    private fun refreshUserProfile(showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(isRefreshingProfile = true, errorMessage = null)
        }

        viewModelScope.launch {
            refreshCurrentUserProfileUseCase()
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to refresh profile"
                    )
                }

            if (showLoading) {
                _uiState.value = _uiState.value.copy(isRefreshingProfile = false)
            }
        }
    }

    fun loadAddresses() {
        _uiState.value = _uiState.value.copy(isLoadingAddresses = true, errorMessage = null)
        viewModelScope.launch {
            getUserAddressesUseCase().onSuccess { addresses ->
                _uiState.value = _uiState.value.copy(
                    addresses = addresses,
                    isLoadingAddresses = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingAddresses = false,
                    errorMessage = error.message ?: "Failed to load addresses"
                )
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)
        viewModelScope.launch {
            val result = uploadImageUseCase(uri)
            result.onSuccess { remoteUrl ->
                val updateResult = updateProfileUseCase(name = null, avatarUrl = remoteUrl)
                updateResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        successMessage = "Avatar updated successfully"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        errorMessage = error.message ?: "Failed to update profile"
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    errorMessage = error.message ?: "Failed to upload image"
                )
            }
            Log.d("ProfileViewModel", "err: ${_uiState.value}")
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Display name cannot be empty")
            return
        }
        
        _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)
        viewModelScope.launch {
            val updateResult = updateProfileUseCase(name = newName, avatarUrl = null)
            updateResult.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    successMessage = "Display name updated successfully"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    errorMessage = error.message ?: "Failed to update display name"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    fun saveAddress(address: UserAddress) {
        if (address.recipientName.isBlank() || address.phoneNumber.isBlank() || address.addressLine.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui long nhap day du thong tin dia chi")
            return
        }

        _uiState.value = _uiState.value.copy(isLoadingAddresses = true, errorMessage = null)
        viewModelScope.launch {
            val result = if (address.id.isBlank()) {
                addUserAddressUseCase(address)
            } else {
                updateUserAddressUseCase(address)
            }

            result.onSuccess {
                loadAddresses()
                _uiState.value = _uiState.value.copy(successMessage = "Da luu dia chi")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingAddresses = false,
                    errorMessage = error.message ?: "Failed to save address"
                )
            }
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        _uiState.value = _uiState.value.copy(isLoadingAddresses = true, errorMessage = null)
        val result = deleteUserAddressUseCase(addressId)
        result.onSuccess {
            _uiState.value = _uiState.value.copy(
                addresses = _uiState.value.addresses.filterNot { it.id == addressId },
                isLoadingAddresses = false,
                successMessage = "Da xoa dia chi"
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoadingAddresses = false,
                errorMessage = error.message ?: "Failed to delete address"
            )
        }
        return result
    }

    fun setDefaultAddress(address: UserAddress) {
        saveAddress(address.copy(isDefault = true))
    }

    fun logout() {
        logoutUseCase()
    }
}

