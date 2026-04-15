package com.example.unimarket.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.presentation.auth.UniversityOption
import com.example.unimarket.presentation.auth.parseUniversityOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UniversityListUiState(
    val options: List<UniversityOption> = emptyList()
)

@HiltViewModel
class UniversityListViewModel @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniversityListUiState())
    val uiState: StateFlow<UniversityListUiState> = _uiState.asStateFlow()

    init {
        loadUniversityList()
    }

    private fun loadUniversityList() {
        viewModelScope.launch {
            runCatching { remoteConfig.fetchAndActivate().await() }
            val options = parseUniversityOptions(remoteConfig.getString(KEY_UNIVERSITY_LIST))
            _uiState.update { it.copy(options = options) }
        }
    }

    private companion object {
        const val KEY_UNIVERSITY_LIST = "university_list"
    }
}
