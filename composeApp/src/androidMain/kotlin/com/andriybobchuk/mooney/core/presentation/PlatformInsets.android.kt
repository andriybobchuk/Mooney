package com.andriybobchuk.mooney.core.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun platformBottomSafeInsets(): WindowInsets = WindowInsets.navigationBars

@Composable
actual fun BottomBarBottomSpacer() {
    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}
