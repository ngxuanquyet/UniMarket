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
import com.example.unimarket.domain.usecase.auth.UpdateUniversityUseCase
import com.example.unimarket.domain.usecase.auth.UpdateUserAddressUseCase
import com.example.unimarket.domain.usecase.image.UploadImageUseCase
import com.example.unimarket.presentation.util.localizedText
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
    private val updateUniversityUseCase: UpdateUniversityUseCase,
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
                        university = cachedUser.university,
                        boughtCount = cachedUser.boughtCount,
                        soldCount = cachedUser.soldCount,
                        averageRating = cachedUser.averageRating,
                        ratingCount = cachedUser.ratingCount,
                        walletBalance = cachedUser.walletBalance
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
                        errorMessage = error.message ?: localizedText(
                            english = "Failed to refresh profile",
                            vietnamese = "Không thể làm mới hồ sơ"
                        )
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
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to load addresses",
                        vietnamese = "Không thể tải danh sách địa chỉ"
                    )
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
                        successMessage = localizedText(
                            english = "Avatar updated successfully",
                            vietnamese = "Cập nhật ảnh đại diện thành công"
                        )
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        errorMessage = error.message ?: localizedText(
                            english = "Failed to update profile",
                            vietnamese = "Không thể cập nhật hồ sơ"
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to upload image",
                        vietnamese = "Không thể tải ảnh lên"
                    )
                )
            }
            Log.d("ProfileViewModel", "err: ${_uiState.value}")
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = localizedText(
                    english = "Display name cannot be empty",
                    vietnamese = "Tên hiển thị không được để trống"
                )
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)
        viewModelScope.launch {
            val updateResult = updateProfileUseCase(name = newName, avatarUrl = null)
            updateResult.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    successMessage = localizedText(
                        english = "Display name updated successfully",
                        vietnamese = "Cập nhật tên hiển thị thành công"
                    )
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to update display name",
                        vietnamese = "Không thể cập nhật tên hiển thị"
                    )
                )
            }
        }
    }

    fun updateUniversity(university: String) {
        val trimmedUniversity = university.trim()
        if (trimmedUniversity.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = localizedText(
                    english = "University cannot be empty",
                    vietnamese = "Trường đại học không được để trống"
                )
            )
            return
        }

        _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)
        viewModelScope.launch {
            updateUniversityUseCase(trimmedUniversity)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        successMessage = localizedText(
                            english = "University updated successfully",
                            vietnamese = "Cập nhật trường đại học thành công"
                        )
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        errorMessage = error.message ?: localizedText(
                            english = "Failed to update university",
                            vietnamese = "Không thể cập nhật trường đại học"
                        )
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    fun saveAddress(address: UserAddress) {
        if (address.recipientName.isBlank() || address.phoneNumber.isBlank() || address.addressLine.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = localizedText(
                    english = "Please fill in all required address fields",
                    vietnamese = "Vui lòng nhập đầy đủ thông tin địa chỉ"
                )
            )
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
                _uiState.value = _uiState.value.copy(
                    successMessage = localizedText(
                        english = "Address saved",
                        vietnamese = "Đã lưu địa chỉ"
                    )
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingAddresses = false,
                    errorMessage = error.message ?: localizedText(
                        english = "Failed to save address",
                        vietnamese = "Không thể lưu địa chỉ"
                    )
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
                successMessage = localizedText(
                    english = "Address deleted",
                    vietnamese = "Đã xóa địa chỉ"
                )
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoadingAddresses = false,
                errorMessage = error.message ?: localizedText(
                    english = "Failed to delete address",
                    vietnamese = "Không thể xóa địa chỉ"
                )
            )
        }
        return result
    }

    fun setDefaultAddress(address: UserAddress) {
        saveAddress(address.copy(isDefault = true))
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
