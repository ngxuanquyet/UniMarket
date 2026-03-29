package com.example.unimarket.presentation.mypurchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPurchasesViewModel @Inject constructor(
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPurchasesUiState(isLoading = true))
    val uiState: StateFlow<MyPurchasesUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun refresh() {
        loadOrders()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getBuyerOrdersUseCase()
                .onSuccess { orders ->
                    _uiState.update {
                        it.copy(
                            orders = orders,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            orders = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load your orders"
                        )
                    }
                }
        }
    }
}
