package com.example.unimarket.presentation.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int? = null,
    val route: String,
    val icon: ImageVector
)
