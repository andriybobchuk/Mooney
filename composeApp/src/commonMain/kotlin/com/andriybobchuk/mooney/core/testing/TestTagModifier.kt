package com.andriybobchuk.mooney.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

/**
 * Attach a Maestro-visible testTag. Both properties (testTag AND
 * testTagsAsResourceId) are set on the same semantics node, so Maestro's
 * `id:` selector resolves regardless of whether this element lives inside
 * a `ModalBottomSheet` or Dialog — those render in a separate Window with
 * their own semantics tree root, and a `testTagsAsResourceId = true` set
 * on the App composition root does NOT propagate into them.
 *
 * `mergeDescendants = true` merges child semantics so a tap on any child
 * resolves to this element's tag.
 */
@Composable
fun Modifier.mooneyTestTag(tag: String): Modifier =
    this.semantics(mergeDescendants = true) {
        testTag = tag
        androidTestTagsAsResourceIdIfAvailable()
    }

/**
 * Enables `testTag → resource-id` surfacing at the composition-root level.
 * Kept as a passthrough on iOS; on Android it applies
 * `Modifier.semantics { testTagsAsResourceId = true }` to a full-size Box.
 *
 * Even with this wrapper at the root, ModalBottomSheet + Dialog + Popup
 * open their own Window/semantics-root — so [mooneyTestTag] additionally
 * sets `testTagsAsResourceId = true` on every tagged node.
 */
@Composable
expect fun WithTestTagsAsResourceId(content: @Composable () -> Unit)

/**
 * Android sets `testTagsAsResourceId = true` on the current semantics node.
 * iOS is a no-op (Compose Multiplatform's iOS surfacing uses UIAccessibility,
 * not resource IDs, so the flag has nothing to surface into).
 */
internal expect fun androidx.compose.ui.semantics.SemanticsPropertyReceiver.androidTestTagsAsResourceIdIfAvailable()
