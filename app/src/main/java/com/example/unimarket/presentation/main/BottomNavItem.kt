package com.example.unimarket.presentation.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    @StringRes val labelRes: Int,
    val route: String,
    val icon: ImageVector
)
