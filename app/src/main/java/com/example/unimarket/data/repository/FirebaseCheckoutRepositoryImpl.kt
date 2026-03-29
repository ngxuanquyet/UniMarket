package com.example.unimarket.data.repository

import com.example.unimarket.BuildConfig
import com.example.unimarket.data.api.NotificationApiService
import com.example.unimarket.data.api.model.BuyNowPurchaseRequestDto
import com.example.unimarket.data.api.model.CheckoutAddressDto
import com.example.unimarket.domain.model.PurchaseConfirmation
import com.example.unimarket.domain.model.PurchaseRequest
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.repository.CheckoutRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

class FirebaseCheckoutRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val notificationApiService: NotificationApiService
) : CheckoutRepository {

    override suspend fun confirmBuyNowPurchase(
        request: PurchaseRequest
    ): Result<PurchaseConfirmation> {
        val buyer = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        if (BuildConfig.NOTIFICATION_SERVER_BASE_URL.isBlank()) {
            return Result.failure(Exception("Checkout backend is not configured"))
        }

        return try {
            val idToken = buyer.getIdToken(false).await().token.orEmpty()
            if (idToken.isBlank()) {
                return Result.failure(Exception("Missing Firebase ID token"))
            }

            val response = notificationApiService.confirmBuyNowPurchase(
                authorization = "Bearer $idToken",
                body = request.toDto()
            )

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        response.errorMessage()
                            ?: "Checkout service failed with code ${response.code()}"
                    )
                )
            }

            val body = response.body()
                ?: return Result.failure(Exception("Empty response from checkout service"))

            Result.success(
                PurchaseConfirmation(
                    orderId = body.orderId,
                    remainingQuantity = body.remainingQuantity
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun PurchaseRequest.toDto(): BuyNowPurchaseRequestDto {
        return BuyNowPurchaseRequestDto(
            productId = productId,
            quantity = quantity,
            deliveryMethod = deliveryMethod.name,
            paymentMethod = paymentMethod,
            meetingPoint = meetingPoint,
            buyerAddress = buyerAddress?.toDto(),
            sellerAddress = sellerAddress?.toDto()
        )
    }

    private fun UserAddress.toDto(): CheckoutAddressDto {
        return CheckoutAddressDto(
            id = id,
            recipientName = recipientName,
            phoneNumber = phoneNumber,
            addressLine = addressLine,
            isDefault = isDefault
        )
    }

    private fun retrofit2.Response<*>.errorMessage(): String? {
        val rawBody = errorBody()?.string().orEmpty()
        if (rawBody.isBlank()) return message().takeIf { it.isNotBlank() }

        return runCatching {
            JSONObject(rawBody).optString("error").ifBlank { message() }
        }.getOrElse {
            message().takeIf { it.isNotBlank() } ?: rawBody
        }
    }
}
