package com.example.unimarket.domain.usecase.notification

import com.example.unimarket.domain.model.AppNotification
import com.example.unimarket.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(limit: Int = 50): Result<List<AppNotification>> {
        return repository.getNotifications(limit)
    }
}
