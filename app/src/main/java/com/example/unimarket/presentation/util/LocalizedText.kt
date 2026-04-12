package com.example.unimarket.presentation.util

import java.util.Locale

fun localizedText(english: String, vietnamese: String): String {
    return if (Locale.getDefault().language.equals("vi", ignoreCase = true)) {
        vietnamese
    } else {
        english
    }
}

