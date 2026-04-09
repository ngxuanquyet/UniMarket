package com.example.unimarket.data.repository

import com.example.unimarket.BuildConfig
import com.example.unimarket.data.api.NotificationApiService
import com.example.unimarket.data.api.SepayApiService
import com.example.unimarket.data.api.model.CheckoutAddressDto
import com.example.unimarket.data.api.model.CheckoutPaymentMethodDto
import com.example.unimarket.data.api.model.OrderDto
import com.example.unimarket.data.api.model.OrderPaymentCheckResponseDto
import com.example.unimarket.data.api.model.OrderStatusUpdateRequestDto
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderPaymentCheckResult
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.repository.OrderRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

class FirebaseOrderRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val notificationApiService: NotificationApiService,
    private val sepayApiService: SepayApiService
) : OrderRepository {

    override suspend fun getBuyerOrders(): Result<List<Order>> {
        val now = System.currentTimeMillis()
        return fetchOrdersFromBackend(actorField = ActorField.BUYER) { authorization ->
            notificationApiService.getBuyerOrders(authorization)
        }.map { orders ->
            orders.filter { order ->
                // Filter out orders that are waiting for payment but have already expired
                order.status != OrderStatus.WAITING_PAYMENT || order.paymentExpiresAt > now
            }
        }
    }

    override suspend fun getSellerOrders(): Result<List<Order>> {
        return fetchOrdersFromBackend(actorField = ActorField.SELLER) { authorization ->
            notificationApiService.getSellerOrders(authorization)
        }
    }

    override suspend fun updateOrderStatus(order: Order, status: OrderStatus): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        
        // Allow the update if the user is the seller OR if the user is the buyer moving it to WAITING_CONFIRMATION
        val isSeller = order.sellerId == userId
        val isBuyerConfirmingPayment = order.buyerId == userId && status == OrderStatus.WAITING_CONFIRMATION
        
        if (!isSeller && !isBuyerConfirmingPayment) {
            return Result.failure(Exception("You can only update your own or verified purchases"))
        }

        if (BuildConfig.NOTIFICATION_SERVER_BASE_URL.isBlank()) {
            return Result.failure(Exception("Checkout backend is not configured"))
        }

        return try {
            val idToken = auth.currentUser?.getIdToken(false)?.await()?.token.orEmpty()
            if (idToken.isBlank()) {
                return Result.failure(Exception("Missing Firebase ID token"))
            }

            val response = notificationApiService.updateOrderStatus(
                authorization = "Bearer $idToken",
                orderId = order.id,
                body = OrderStatusUpdateRequestDto(status = status.name)
            )

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        response.errorMessage()
                            ?: "Order update failed with code ${response.code()}"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkTransferPayment(orderId: String): Result<OrderPaymentCheckResult> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No user logged in"))

        if (BuildConfig.NOTIFICATION_SERVER_BASE_URL.isBlank()) {
            return Result.failure(Exception("Checkout backend is not configured"))
        }

        return try {
            val idToken = currentUser.getIdToken(false).await().token.orEmpty()
            if (idToken.isBlank()) {
                return Result.failure(Exception("Missing Firebase ID token"))
            }

            val response = notificationApiService.checkTransferPayment(
                authorization = "Bearer $idToken",
                orderId = orderId
            )

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        response.errorMessage()
                            ?: "Payment check failed with code ${response.code()}"
                    )
                )
            }

            val body = response.body()
                ?: return Result.failure(Exception("Empty response from payment check service"))

            Result.success(body.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private fun mapOrder(document: DocumentSnapshot): Order? {
        return try {
            val productMap = document.get("product").asMapValue()
            val sellerMap = document.get("seller").asMapValue()

            val quantity = firstLong(document, productMap, "quantity", "itemCount")
                ?.toInt()
                ?.coerceAtLeast(1)
                ?: 1

            val unitPrice = firstDouble(
                document,
                productMap,
                "unitPrice",
                "price",
                "productPrice"
            ) ?: 0.0

            val totalAmount = firstDouble(
                document,
                productMap,
                "totalAmount",
                "totalPrice",
                "orderTotal",
                "total",
                "amount"
            ) ?: (unitPrice * quantity)

            val rawStatus = firstString(document, null, "status", "orderStatus")
            val status = OrderStatus.fromRaw(rawStatus)

            Order(
                id = document.id,
                documentPath = document.reference.path,
                buyerId = firstString(document, null, "buyerId", "buyerUid")
                    ?: document.reference.parent.parent?.id.orEmpty(),
                buyerName = firstString(document, null, "buyerName", "buyerDisplayName").orEmpty(),
                sellerId = firstString(document, sellerMap, "sellerId", "sellerUid", "userId")
                    .orEmpty(),
                storeName = firstString(
                    document,
                    sellerMap,
                    "storeName",
                    "sellerName",
                    "sellerDisplayName"
                ).orEmpty().ifBlank { "Campus Seller" },
                productId = firstString(document, productMap, "productId", "id").orEmpty(),
                productName = firstString(document, productMap, "productName", "name")
                    .orEmpty()
                    .ifBlank { "Purchased Item" },
                productDetail = firstString(
                    document,
                    productMap,
                    "productDetail",
                    "productSubtitle",
                    "productDescription",
                    "description",
                    "condition"
                ).orEmpty(),
                productImageUrl = firstImageUrl(document, productMap),
                quantity = quantity,
                unitPrice = unitPrice,
                totalAmount = totalAmount,
                deliveryMethod = firstString(document, null, "deliveryMethod").orEmpty(),
                paymentMethod = firstString(document, null, "paymentMethod").orEmpty(),
                paymentMethodDetails = mapPaymentMethod(document.get("paymentMethodDetails")),
                meetingPoint = firstString(document, null, "meetingPoint").orEmpty(),
                buyerAddress = mapAddress(document.get("buyerAddress")),
                sellerAddress = mapAddress(document.get("sellerAddress")),
                transferContent = firstString(document, null, "transferContent").orEmpty(),
                paymentExpiresAt = firstTimestampMillis(document, null, "paymentExpiresAt") ?: 0L,
                paymentConfirmedAt = firstTimestampMillis(document, null, "paymentConfirmedAt") ?: 0L,
                status = status,
                statusLabel = rawStatus.toStatusLabel(status),
                createdAt = firstTimestampMillis(document, productMap, "createdAt", "orderedAt") ?: 0L,
                updatedAt = firstTimestampMillis(
                    document,
                    productMap,
                    "updatedAt",
                    "lastUpdatedAt",
                    "statusUpdatedAt"
                ) ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun mapAddress(value: Any?): UserAddress? {
        val map = value.asMapValue() ?: return null
        val addressLine = map["addressLine"].toStringValue().orEmpty()
        val recipientName = map["recipientName"].toStringValue().orEmpty()
        val phoneNumber = map["phoneNumber"].toStringValue().orEmpty()
        val id = map["id"].toStringValue().orEmpty()
        val isDefault = map["isDefault"] as? Boolean ?: false

        if (addressLine.isBlank() && recipientName.isBlank() && phoneNumber.isBlank()) {
            return null
        }

        return UserAddress(
            id = id,
            recipientName = recipientName,
            phoneNumber = phoneNumber,
            addressLine = addressLine,
            isDefault = isDefault
        )
    }

    private fun mapPaymentMethod(value: Any?): SellerPaymentMethod? {
        val map = value.asMapValue() ?: return null
        return SellerPaymentMethod(
            id = map["id"].toStringValue().orEmpty(),
            type = SellerPaymentMethodType.fromRaw(map["type"].toStringValue()),
            label = map["label"].toStringValue().orEmpty(),
            accountName = map["accountName"].toStringValue().orEmpty(),
            accountNumber = map["accountNumber"].toStringValue().orEmpty(),
            bankCode = map["bankCode"].toStringValue().orEmpty(),
            bankName = map["bankName"].toStringValue().orEmpty(),
            phoneNumber = map["phoneNumber"].toStringValue().orEmpty(),
            note = map["note"].toStringValue().orEmpty(),
            isDefault = map["isDefault"] as? Boolean ?: false
        )
    }

    private fun firstString(
        document: DocumentSnapshot,
        nested: Map<String, Any?>?,
        vararg keys: String
    ): String? {
        keys.forEach { key ->
            document.getString(key)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
            nested?.get(key).toStringValue()?.let { return it }
        }
        return null
    }

    private fun firstLong(
        document: DocumentSnapshot,
        nested: Map<String, Any?>?,
        vararg keys: String
    ): Long? {
        keys.forEach { key ->
            document.get(key).toLongValue()?.let { return it }
            nested?.get(key).toLongValue()?.let { return it }
        }
        return null
    }

    private fun firstDouble(
        document: DocumentSnapshot,
        nested: Map<String, Any?>?,
        vararg keys: String
    ): Double? {
        keys.forEach { key ->
            document.get(key).toDoubleValue()?.let { return it }
            nested?.get(key).toDoubleValue()?.let { return it }
        }
        return null
    }

    private fun firstTimestampMillis(
        document: DocumentSnapshot,
        nested: Map<String, Any?>?,
        vararg keys: String
    ): Long? {
        keys.forEach { key ->
            document.get(key).toTimestampMillis()?.let { return it }
            nested?.get(key).toTimestampMillis()?.let { return it }
        }
        return null
    }

    private fun firstImageUrl(
        document: DocumentSnapshot,
        nested: Map<String, Any?>?
    ): String {
        val directString = firstString(
            document,
            nested,
            "productImageUrl",
            "productImage",
            "imageUrl",
            "thumbnailUrl"
        )
        if (!directString.isNullOrBlank()) return directString

        val directList = (document.get("productImageUrls") as? List<*>)?.firstOrNull().toStringValue()
            ?: (document.get("imageUrls") as? List<*>)?.firstOrNull().toStringValue()
            ?: (nested?.get("productImageUrls") as? List<*>)?.firstOrNull().toStringValue()
            ?: (nested?.get("imageUrls") as? List<*>)?.firstOrNull().toStringValue()

        return directList.orEmpty()
    }

    private fun Any?.asMapValue(): Map<String, Any?>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Map<String, Any?>
    }

    private fun Any?.toStringValue(): String? {
        return when (this) {
            null -> null
            is String -> trim().takeIf { it.isNotBlank() }
            else -> toString().trim().takeIf { it.isNotBlank() }
        }
    }

    private fun Any?.toLongValue(): Long? {
        return when (this) {
            is Long -> this
            is Int -> toLong()
            is Double -> toLong()
            is Float -> toLong()
            is String -> toLongOrNull()
            else -> null
        }
    }

    private fun Any?.toDoubleValue(): Double? {
        return when (this) {
            is Double -> this
            is Float -> toDouble()
            is Long -> toDouble()
            is Int -> toDouble()
            is String -> toDoubleOrNull()
            else -> null
        }
    }

    private fun Any?.toTimestampMillis(): Long? {
        return when (this) {
            is Timestamp -> toDate().time
            is Long -> this
            is Int -> toLong()
            is Double -> toLong()
            is String -> toLongOrNull()
            else -> null
        }
    }

    private fun String?.toStatusLabel(fallbackStatus: OrderStatus): String {
        val rawValue = this.orEmpty().trim()
        if (rawValue.isBlank()) return fallbackStatus.label

        return rawValue
            .replace("-", " ")
            .replace("_", " ")
            .trim()
            .uppercase()
    }

    private suspend fun fetchOrdersFromBackend(
        actorField: ActorField,
        request: suspend (String) -> retrofit2.Response<com.example.unimarket.data.api.model.OrdersResponseDto>
    ): Result<List<Order>> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        if (BuildConfig.NOTIFICATION_SERVER_BASE_URL.isBlank()) {
            return Result.failure(Exception("Checkout backend is not configured"))
        }

        return try {
            val idToken = currentUser.getIdToken(false).await().token.orEmpty()
            if (idToken.isBlank()) {
                return Result.failure(Exception("Missing Firebase ID token"))
            }

            val response = request("Bearer $idToken")
            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(
                        response.errorMessage()
                            ?: "Failed to load orders with code ${response.code()}"
                    )
                )
            }

            val body = response.body()
                ?: return Result.failure(Exception("Empty response from order service"))

            Result.success(
                body.orders
                    .map(::mapBackendOrder)
                    .filter { order ->
                        when (actorField) {
                            ActorField.BUYER -> order.buyerId == currentUser.uid
                            ActorField.SELLER -> order.sellerId == currentUser.uid
                        }
                    }
                    .distinctBy { it.id }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapBackendOrder(order: OrderDto): Order {
        val status = OrderStatus.fromRaw(order.status)

        return Order(
            id = order.id,
            documentPath = order.documentPath,
            buyerId = order.buyerId,
            buyerName = order.buyerName,
            sellerId = order.sellerId,
            storeName = order.sellerName.ifBlank { "Campus Seller" },
            productId = order.productId,
            productName = order.productName.ifBlank { "Purchased Item" },
            productDetail = order.productDetail,
            productImageUrl = order.productImageUrl,
            quantity = order.quantity.coerceAtLeast(1),
            unitPrice = order.unitPrice,
            totalAmount = order.totalAmount,
            deliveryMethod = order.deliveryMethod,
            paymentMethod = order.paymentMethod,
            paymentMethodDetails = order.paymentMethodDetails?.toDomain(),
            meetingPoint = order.meetingPoint,
            buyerAddress = order.buyerAddress?.toDomain(),
            sellerAddress = order.sellerAddress?.toDomain(),
            transferContent = order.transferContent,
            paymentExpiresAt = order.paymentExpiresAt,
            paymentConfirmedAt = order.paymentConfirmedAt,
            status = status,
            statusLabel = order.statusLabel.ifBlank { status.label },
            createdAt = order.createdAt,
            updatedAt = order.updatedAt
        )
    }

    private fun CheckoutAddressDto.toDomain(): UserAddress {
        return UserAddress(
            id = id,
            recipientName = recipientName,
            phoneNumber = phoneNumber,
            addressLine = addressLine,
            isDefault = isDefault
        )
    }

    private fun CheckoutPaymentMethodDto.toDomain(): SellerPaymentMethod {
        return SellerPaymentMethod(
            id = id,
            type = SellerPaymentMethodType.fromRaw(type),
            label = label,
            accountName = accountName,
            accountNumber = accountNumber,
            bankCode = bankCode,
            bankName = bankName,
            phoneNumber = phoneNumber,
            note = note,
            isDefault = isDefault
        )
    }

    private fun OrderPaymentCheckResponseDto.toDomain(): OrderPaymentCheckResult {
        val normalizedStatus = OrderStatus.fromRaw(status)
        return OrderPaymentCheckResult(
            orderId = orderId,
            status = normalizedStatus,
            statusLabel = statusLabel.ifBlank { normalizedStatus.label },
            paymentExpiresAt = paymentExpiresAt,
            paymentConfirmedAt = paymentConfirmedAt
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

    private companion object {
        const val USERS_COLLECTION = "users"
        const val ORDERS_COLLECTION = "orders"
    }

    private enum class ActorField {
        BUYER,
        SELLER
    }
}
