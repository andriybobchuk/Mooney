package com.andriybobchuk.mooney.core.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun platformBottomSafeInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)

@Composable
actual fun BottomBarBottomSpacer() {
    Spacer(modifier = Modifier.height(20.dp))
}
