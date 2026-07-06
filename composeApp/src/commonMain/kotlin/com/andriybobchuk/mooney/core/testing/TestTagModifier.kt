package com.andriybobchuk.mooney.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

/**
 * Attach a Maestro-visible testTag. Merges descendants so a tap on any child
 * inside a tagged Row/Column still resolves to this element's ID.
 *
 * Android additionally needs [WithTestTagsAsResourceId] wrapped once around
 * the composition root so Maestro's `id:` selector resolves testTags as
 * Android resource identifiers.
 */
@Composable
fun Modifier.mooneyTestTag(tag: String): Modifier =
    this.semantics(mergeDescendants = true) { testTag = tag }

/**
 * Enables `testTag → resource-id` surfacing so Maestro's `id:` selector
 * works on Android. iOS is a passthrough — Compose Multiplatform's iOS
 * accessibility bridge handles testTags separately.
 */
@Composable
expect fun WithTestTagsAsResourceId(content: @Composable () -> Unit)
