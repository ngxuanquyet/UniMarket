package com.example.unimarket.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.usecase.auth.DeleteUserPaymentMethodUseCase
import com.example.unimarket.domain.usecase.auth.GetCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.ObserveCachedUserUseCase
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import com.example.unimarket.domain.usecase.auth.SaveUserPaymentMethodUseCase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class PaymentMethodsUiState(
    val methods: List<SellerPaymentMethod> = emptyList(),
    val bankOptions: List<BankOption> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

data class BankOption(
    val bankCode: String,
    val shortName: String,
    val name: String
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    getCachedUserUseCase: GetCachedUserUseCase,
    observeCachedUserUseCase: ObserveCachedUserUseCase,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase,
    private val saveUserPaymentMethodUseCase: SaveUserPaymentMethodUseCase,
    private val deleteUserPaymentMethodUseCase: DeleteUserPaymentMethodUseCase,
    private val remoteConfig: FirebaseRemoteConfig
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
        loadBankOptions()
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

    private fun loadBankOptions() {
        viewModelScope.launch {
            runCatching { remoteConfig.fetchAndActivate().await() }
            val options = parseBankOptions(remoteConfig.getString(KEY_LIST_BANK_CONFIG))
            _uiState.update { it.copy(bankOptions = options) }
        }
    }

    private companion object {
        const val KEY_LIST_BANK_CONFIG = "LIST_BANK_CONFIG"
    }
}

private fun parseBankOptions(raw: String): List<BankOption> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val root = JSONObject(raw)
        val banks = root.optJSONArray("banks") ?: return@runCatching emptyList()
        buildList {
            for (index in 0 until banks.length()) {
                val item = banks.optJSONObject(index) ?: continue
                val bankCode = item.optString("bankCode").trim()
                val shortName = item.optString("shortName").trim()
                val name = item.optString("name").trim()
                if (bankCode.isNotBlank() && shortName.isNotBlank()) {
                    add(
                        BankOption(
                            bankCode = bankCode,
                            shortName = shortName,
                            name = name
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}
