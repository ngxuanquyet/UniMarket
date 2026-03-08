package com.example.unimarket.presentation.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListingsUiState(isLoading = true))
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    init {
        loadMyListings()
    }

    private fun loadMyListings() {
        val currentUser = getCurrentUserUseCase() as? FirebaseUser
        val currentUid = currentUser?.uid
        
        viewModelScope.launch {
            getAllProductsUseCase()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load listings"
                    )
                }
                .collect { products ->
                    val myProducts = products.filter { it.userId == currentUid }
                    
                    _uiState.value = _uiState.value.copy(
                        activeListings = myProducts,
                        // Dummy data for other tabs to show UI capabilities
                        soldListings = emptyList(), 
                        draftListings = emptyList(),
                        isLoading = false
                    )
                }
        }
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
    }
}
