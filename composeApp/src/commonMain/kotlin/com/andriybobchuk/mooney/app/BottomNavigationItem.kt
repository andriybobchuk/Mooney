package com.andriybobchuk.mooney.app

import androidx.compose.ui.graphics.painter.Painter

data class BottomNavigationItem(
    val title: String,
    val icon: Painter,
    val filledIcon: Painter? = null
)
