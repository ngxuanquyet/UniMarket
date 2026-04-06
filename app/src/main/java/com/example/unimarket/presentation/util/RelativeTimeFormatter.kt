package com.example.unimarket.presentation.util

import java.util.Locale
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
    val isVietnamese = Locale.getDefault().language == "vi"

    return when {
        diffMillis < minuteMillis -> if (isVietnamese) "vừa xong" else "just now"
        diffMillis < hourMillis -> {
            val minutes = diffMillis / minuteMillis
            if (isVietnamese) {
                "$minutes phút trước"
            } else if (minutes == 1L) {
                "1 minute ago"
            } else {
                "$minutes minutes ago"
            }
        }
        diffMillis < dayMillis -> {
            val hours = diffMillis / hourMillis
            if (isVietnamese) {
                "$hours giờ trước"
            } else if (hours == 1L) {
                "1 hour ago"
            } else {
                "$hours hours ago"
            }
        }
        diffMillis < weekMillis -> {
            val days = diffMillis / dayMillis
            if (isVietnamese) {
                "$days ngày trước"
            } else if (days == 1L) {
                "1 day ago"
            } else {
                "$days days ago"
            }
        }
        diffMillis < monthMillis -> {
            val weeks = diffMillis / weekMillis
            if (isVietnamese) {
                "$weeks tuần trước"
            } else if (weeks == 1L) {
                "1 week ago"
            } else {
                "$weeks weeks ago"
            }
        }
        diffMillis < yearMillis -> {
            val months = diffMillis / monthMillis
            if (isVietnamese) {
                "$months tháng trước"
            } else if (months == 1L) {
                "1 month ago"
            } else {
                "$months months ago"
            }
        }
        else -> {
            val years = diffMillis / yearMillis
            if (isVietnamese) {
                "$years năm trước"
            } else if (years == 1L) {
                "1 year ago"
            } else {
                "$years years ago"
            }
        }
    }
}
