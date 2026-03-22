package com.example.unimarket.presentation.util

import kotlin.math.max

fun Long.toRelativeTimeLabel(nowMillis: Long = System.currentTimeMillis()): String {
    if (this <= 0L) return ""

    val diffMillis = max(0L, nowMillis - this)
    val minuteMillis = 60_000L
    val hourMillis = 60 * minuteMillis
    val dayMillis = 24 * hourMillis
    val weekMillis = 7 * dayMillis
    val monthMillis = 30 * dayMillis
    val yearMillis = 365 * dayMillis

    return when {
        diffMillis < minuteMillis -> "just now"
        diffMillis < hourMillis -> {
            val minutes = diffMillis / minuteMillis
            if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        }
        diffMillis < dayMillis -> {
            val hours = diffMillis / hourMillis
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        diffMillis < weekMillis -> {
            val days = diffMillis / dayMillis
            if (days == 1L) "1 day ago" else "$days days ago"
        }
        diffMillis < monthMillis -> {
            val weeks = diffMillis / weekMillis
            if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        }
        diffMillis < yearMillis -> {
            val months = diffMillis / monthMillis
            if (months == 1L) "1 month ago" else "$months months ago"
        }
        else -> {
            val years = diffMillis / yearMillis
            if (years == 1L) "1 year ago" else "$years years ago"
        }
    }
}
