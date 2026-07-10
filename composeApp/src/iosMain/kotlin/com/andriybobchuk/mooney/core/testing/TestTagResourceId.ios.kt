package com.andriybobchuk.mooney.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

@Composable
actual fun WithTestTagsAsResourceId(content: @Composable () -> Unit) {
    content()
}

internal actual fun SemanticsPropertyReceiver.androidTestTagsAsResourceIdIfAvailable() {
    // Compose Multiplatform iOS surfaces semantics via UIAccessibility;
    // `testTagsAsResourceId` is an Android-only concept and has no
    // equivalent to set here.
}
