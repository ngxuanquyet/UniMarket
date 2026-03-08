package com.example.unimarket.presentation.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.auth.UpdateProfileUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.LogoutUseCase
import com.example.unimarket.domain.usecase.image.UploadImageUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(
                displayName = currentUser.displayName ?: "User",
                email = currentUser.email ?: "",
                photoUrl = currentUser.photoUrl?.toString() ?: ""
            )
        }
    }

    fun updateAvatar(uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)
        viewModelScope.launch {
            val result = uploadImageUseCase(uri)
            result.onSuccess { remoteUrl ->
                // Update Firebase Auth profile
                val updateResult = updateProfileUseCase(name = null, photoUrl = remoteUrl)
                updateResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        photoUrl = remoteUrl,
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
            val updateResult = updateProfileUseCase(name = newName, photoUrl = null)
            updateResult.onSuccess {
                _uiState.value = _uiState.value.copy(
                    displayName = newName,
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

    fun logout() {
        logoutUseCase()
    }
}

