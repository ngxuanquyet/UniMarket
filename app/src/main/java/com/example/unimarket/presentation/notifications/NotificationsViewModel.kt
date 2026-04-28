package com.example.unimarket.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.AppNotification
import com.example.unimarket.domain.usecase.notification.DeleteNotificationUseCase
import com.example.unimarket.domain.usecase.notification.GetNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = getNotificationsUseCase(limit = 50)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    notifications = result.getOrDefault(emptyList()),
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val currentNotifications = _uiState.value.notifications
            _uiState.update {
                it.copy(
                    notifications = it.notifications.filterNot { notification -> notification.id == notificationId },
                    errorMessage = null
                )
            }

            deleteNotificationUseCase(notificationId).onFailure { error ->
                _uiState.update {
                    it.copy(
                        notifications = currentNotifications,
                        errorMessage = error.message
                    )
                }
            }
        }
    }
}
