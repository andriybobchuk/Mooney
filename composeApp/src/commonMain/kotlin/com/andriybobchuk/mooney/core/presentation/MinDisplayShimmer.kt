package com.andriybobchuk.mooney.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import kotlinx.coroutines.delay

/**
 * Holds a shimmer-visible flag at `true` for at least [minDisplayMs] once it
 * has been shown, even if [isLoading] flips back to `false` immediately after.
 *
 * Without this, the AppDataCache emits its first snapshot fast enough on a
 * cold start that a Room-backed UI can race past the shimmer in well under
 * one frame — the user sees an empty placeholder instead of the loading
 * skeleton, and assumes the screen never loaded. This composable enforces a
 * visible loading state on cold starts while still skipping it entirely when
 * the cache was already warm (returning user, tab switching).
 *
 * Returns a [State] of `Boolean` whose value:
 *  - mirrors [isLoading] when it transitions to `true`,
 *  - lingers at `true` for at least [minDisplayMs] after [isLoading] flips
 *    to `false` (but only if a shimmer was actually showing),
 *  - stays `false` if [isLoading] starts as `false` (warm-cache case).
 */
@Composable
fun rememberMinDisplayShimmer(
    isLoading: Boolean,
    minDisplayMs: Long = 400L
): State<Boolean> {
    val state = remember { mutableStateOf(isLoading) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            state.value = true
        } else if (state.value) {
            // Was showing — hold for the minimum duration before hiding so the
            // user actually perceives the loading state.
            delay(minDisplayMs)
            state.value = false
        }
    }
    return state
}
