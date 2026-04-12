package com.example.unimarket.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.unimarket.R
import com.example.unimarket.domain.model.DeliveryMethod
import com.example.unimarket.domain.model.OrderStatus

@Composable
fun localizedCategoryLabel(category: String): String {
    return when (category) {
        "All Items" -> stringResource(R.string.category_all_items)
        "Electronics" -> stringResource(R.string.category_electronics)
        "Textbooks" -> stringResource(R.string.category_textbooks)
        "Furniture" -> stringResource(R.string.category_furniture)
        "Clothing" -> stringResource(R.string.category_clothing)
        "Other" -> stringResource(R.string.category_other)
        "Select a category" -> stringResource(R.string.sell_select_category)
        else -> category
    }
}

@Composable
fun localizedConditionLabel(condition: String): String {
    return when (condition.trim().lowercase()) {
        "new" -> stringResource(R.string.condition_new)
        "like new" -> stringResource(R.string.condition_like_new)
        "good" -> stringResource(R.string.condition_good)
        "fair" -> stringResource(R.string.condition_fair)
        else -> condition
    }
}

@Composable
fun DeliveryMethod.localizedTitle(): String {
    return stringResource(
        when (this) {
            DeliveryMethod.DIRECT_MEET -> R.string.delivery_direct_meet
            DeliveryMethod.BUYER_TO_SELLER -> R.string.delivery_buyer_to_seller
            DeliveryMethod.SELLER_TO_BUYER -> R.string.delivery_seller_to_buyer
            DeliveryMethod.SHIPPING -> R.string.delivery_shipping
        }
    )
}

@Composable
fun DeliveryMethod.localizedSubtitle(): String {
    return stringResource(
        when (this) {
            DeliveryMethod.DIRECT_MEET -> R.string.delivery_direct_meet_subtitle
            DeliveryMethod.BUYER_TO_SELLER -> R.string.delivery_buyer_to_seller_subtitle
            DeliveryMethod.SELLER_TO_BUYER -> R.string.delivery_seller_to_buyer_subtitle
            DeliveryMethod.SHIPPING -> R.string.delivery_shipping_subtitle
        }
    )
}

@Composable
fun localizedDeliveryMethodLabel(rawValue: String): String {
    return when (rawValue.uppercase()) {
        "DIRECT_MEET" -> stringResource(R.string.delivery_direct_meet)
        "BUYER_TO_SELLER" -> stringResource(R.string.delivery_buyer_to_seller)
        "SELLER_TO_BUYER" -> stringResource(R.string.delivery_seller_to_buyer)
        "SHIPPING" -> stringResource(R.string.delivery_shipping)
        else -> rawValue.ifBlank { stringResource(R.string.delivery_unknown) }
    }
}

@Composable
fun localizedPaymentMethodLabel(rawValue: String): String {
    return when (rawValue.trim().uppercase()) {
        "CASH_ON_DELIVERY",
        "Cash on delivery" -> stringResource(R.string.payment_cash_on_delivery)
        "BANK_TRANSFER",
        "Bank Transfer" -> stringResource(R.string.payment_bank_transfer)
        "MOMO" -> stringResource(R.string.payment_momo)
        "WALLET",
        "WALLET_PAYMENT" -> stringResource(R.string.payment_wallet)
        else -> rawValue.ifBlank { stringResource(R.string.payment_unknown) }
    }
}

@Composable
fun OrderStatus.localizedLabel(): String {
    return stringResource(
        when (this) {
            OrderStatus.WAITING_PAYMENT -> R.string.order_status_waiting_payment
            OrderStatus.WAITING_CONFIRMATION -> R.string.order_status_waiting_confirmation
            OrderStatus.WAITING_PICKUP -> R.string.order_status_waiting_pickup
            OrderStatus.SHIPPING -> R.string.order_status_shipping
            OrderStatus.IN_TRANSIT -> R.string.order_status_in_transit
            OrderStatus.OUT_FOR_DELIVERY -> R.string.order_status_out_for_delivery
            OrderStatus.DELIVERED -> R.string.order_status_delivered
            OrderStatus.CANCELLED -> R.string.order_status_cancelled
            OrderStatus.UNKNOWN -> R.string.order_status_unknown
        }
    )
}
