package com.example.unimarket.presentation.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val vndFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply {
    currency = Currency.getInstance("VND")
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

fun formatVnd(amount: Double): String = vndFormatter.format(amount)
