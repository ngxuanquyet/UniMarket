package com.example.unimarket.domain.usecase.notification

import com.example.unimarket.domain.repository.NotificationRepository
import javax.inject.Inject

class DeleteNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        return repository.deleteNotification(notificationId)
    }
}
