package com.example.unimarket.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.usecase.auth.DeleteUserPaymentMethodUseCase
import com.example.unimarket.domain.usecase.auth.GetCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.ObserveCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.auth.SaveUserPaymentMethodUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentMethodsUiState(
    val methods: List<SellerPaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    getCachedUserUseCase: GetCachedUserUseCase,
    observeCachedUserUseCase: ObserveCachedUserUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val saveUserPaymentMethodUseCase: SaveUserPaymentMethodUseCase,
    private val deleteUserPaymentMethodUseCase: DeleteUserPaymentMethodUseCase
) : ViewModel() {

    private val cachedMethods = getCachedUserUseCase()?.paymentMethods.orEmpty()
    private val _uiState = MutableStateFlow(
        PaymentMethodsUiState(
            methods = cachedMethods,
            isLoading = cachedMethods.isEmpty()
        )
    )
    val uiState: StateFlow<PaymentMethodsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCachedUserUseCase().collect { user ->
                _uiState.update {
                    it.copy(
                        methods = user?.paymentMethods.orEmpty(),
                        isLoading = false
                    )
                }
            }
        }
        refresh(showLoading = _uiState.value.methods.isEmpty())
    }

    fun refresh(showLoading: Boolean = false) {
        _uiState.update { current ->
            current.copy(
                isLoading = if (showLoading) true else current.isLoading && current.methods.isEmpty(),
                errorMessage = null
            )
        }
        viewModelScope.launch {
            refreshCurrentUserProfileUseCase()
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(isLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load payment methods"
                        )
                    }
                }
        }
    }

    fun saveMethod(method: SellerPaymentMethod) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            saveUserPaymentMethodUseCase(method)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Payment method saved"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to save payment method"
                        )
                    }
                }
        }
    }

    fun deleteMethod(methodId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            deleteUserPaymentMethodUseCase(methodId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Payment method removed"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to delete payment method"
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
