package com.example.unimarket.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.usecase.auth.GetUserPaymentMethodsUseCase
import com.example.unimarket.domain.usecase.wallet.CreateWithdrawalRequestUseCase
import com.example.unimarket.presentation.util.localizedText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletWithdrawUiState(
    val isLoadingMethods: Boolean = false,
    val methods: List<SellerPaymentMethod> = emptyList(),
    val selectedMethodId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successRequestId: String? = null,
    val successAmount: Long? = null
)

@HiltViewModel
class WalletWithdrawViewModel @Inject constructor(
    private val createWithdrawalRequestUseCase: CreateWithdrawalRequestUseCase,
    private val getUserPaymentMethodsUseCase: GetUserPaymentMethodsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(WalletWithdrawUiState())
    val uiState: StateFlow<WalletWithdrawUiState> = _uiState.asStateFlow()

    init {
        loadPaymentMethods()
    }

    fun loadPaymentMethods() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMethods = true, errorMessage = null) }
            val result = getUserPaymentMethodsUseCase()
            val methods = result.getOrDefault(emptyList())
            val preferredMethodId = methods.firstOrNull { it.isDefault }?.id
                ?: methods.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    isLoadingMethods = false,
                    methods = methods,
                    selectedMethodId = preferredMethodId,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun selectMethod(methodId: String) {
        if (methodId.isBlank()) return
        _uiState.update { it.copy(selectedMethodId = methodId, errorMessage = null) }
    }

    fun submitWithdrawal(amount: Long) {
        if (amount <= 0L) return

        viewModelScope.launch {
            val selectedMethod = currentSelectedMethod()
            if (selectedMethod == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = localizedText(
                            english = "Please select a receiving account.",
                            vietnamese = "Vui lòng chọn tài khoản nhận tiền."
                        )
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    successRequestId = null,
                    successAmount = null
                )
            }
            val result = createWithdrawalRequestUseCase(amount, selectedMethod)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isSubmitting = false,
                        successRequestId = result.getOrNull(),
                        successAmount = amount,
                        errorMessage = null
                    )
                } else {
                    it.copy(
                        isSubmitting = false,
                        successRequestId = null,
                        successAmount = null,
                        errorMessage = result.exceptionOrNull()?.message ?: localizedText(
                            english = "Unable to submit withdrawal request.",
                            vietnamese = "Không thể gửi yêu cầu rút tiền."
                        )
                    )
                }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(successRequestId = null, successAmount = null) }
    }

    private fun currentSelectedMethod(): SellerPaymentMethod? {
        val state = _uiState.value
        return state.methods.firstOrNull { it.id == state.selectedMethodId }
    }
}
