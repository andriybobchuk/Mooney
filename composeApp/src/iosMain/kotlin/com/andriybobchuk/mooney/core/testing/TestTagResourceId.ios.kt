package com.andriybobchuk.mooney.core.testing

import androidx.compose.runtime.Composable

@Composable
actual fun WithTestTagsAsResourceId(content: @Composable () -> Unit) {
    content()
}
