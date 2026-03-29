package com.example.unimarket.data.api

import com.example.unimarket.data.api.model.ChatNotificationRequest
import com.example.unimarket.data.api.model.BuyNowPurchaseRequestDto
import com.example.unimarket.data.api.model.BuyNowPurchaseResponseDto
import com.example.unimarket.data.api.model.OrdersResponseDto
import com.example.unimarket.data.api.model.OrderStatusUpdateRequestDto
import com.example.unimarket.data.api.model.OrderStatusUpdateResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApiService {

    @POST("notifications/chat")
    suspend fun notifyNewChatMessage(
        @Header("Authorization") authorization: String,
        @Body body: ChatNotificationRequest
    ): Response<Unit>

    @POST("checkout/buy-now")
    suspend fun confirmBuyNowPurchase(
        @Header("Authorization") authorization: String,
        @Body body: BuyNowPurchaseRequestDto
    ): Response<BuyNowPurchaseResponseDto>

    @GET("orders/buyer")
    suspend fun getBuyerOrders(
        @Header("Authorization") authorization: String
    ): Response<OrdersResponseDto>

    @GET("orders/seller")
    suspend fun getSellerOrders(
        @Header("Authorization") authorization: String
    ): Response<OrdersResponseDto>

    @POST("orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String,
        @Body body: OrderStatusUpdateRequestDto
    ): Response<OrderStatusUpdateResponseDto>
}
