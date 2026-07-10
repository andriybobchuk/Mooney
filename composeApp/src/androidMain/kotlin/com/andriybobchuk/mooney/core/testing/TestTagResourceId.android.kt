package com.andriybobchuk.mooney.core.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Surfaces every descendant `Modifier.testTag(...)` as an Android view
 * resource id so Maestro's `id:` selector resolves it. Applied at the
 * composition root in `App()`; also folded into `mooneyTestTag` per-node
 * so ModalBottomSheet + Dialog + Popup content still surfaces.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun WithTestTagsAsResourceId(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        content()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun SemanticsPropertyReceiver.androidTestTagsAsResourceIdIfAvailable() {
    testTagsAsResourceId = true
}
