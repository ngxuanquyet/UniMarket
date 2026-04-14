package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.AppNotification

interface NotificationRepository {
    suspend fun getNotifications(limit: Int = 50): Result<List<AppNotification>>
}
