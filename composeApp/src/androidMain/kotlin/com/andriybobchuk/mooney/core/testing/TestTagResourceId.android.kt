package com.andriybobchuk.mooney.core.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Surfaces every descendant `Modifier.testTag(...)` as an Android view
 * resource id so Maestro's `id:` selector resolves it. Applied once at
 * the composition root in `App()`.
 *
 * The extension lives in `androidx.compose.ui.semantics` (not `platform`,
 * despite older docs) — this bit us in the Phase-1 build.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun WithTestTagsAsResourceId(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        content()
    }
}
