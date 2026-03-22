package com.example.unimarket.data.api

import com.example.unimarket.data.api.model.ChatNotificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NotificationApiService {

    @POST("notifications/chat")
    suspend fun notifyNewChatMessage(
        @Header("Authorization") authorization: String,
        @Body body: ChatNotificationRequest
    ): Response<Unit>
}
