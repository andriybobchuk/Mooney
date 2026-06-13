package com.andriybobchuk.mooney.core.ads

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Per-process snapshot of the two counters [AdEligibilityUseCase] needs:
 *  - [sessionCount] — total cold-start opens. Increments once at app launch
 *    and stays fixed for the rest of the session. Drives the "no ads for
 *    first 3 sessions" grace window.
 *  - [tapCount] — meaningful taps this session. Increments only when the user
 *    clearly engages (open a screen, save a transaction). NOT every touch.
 *    Drives the "interstitial only after the user has clearly been using the
 *    app" rule.
 *
 * Provided at the top of the composition tree by `NavigationHost` so any
 * screen can pull it without prop drilling.
 */
data class AdSession(
    val sessionCount: Int,
    val tapCount: Int
)

val LocalAdSession = staticCompositionLocalOf {
    // Defaulting to 0/0 means [AdEligibilityUseCase] denies every placement
    // until the real values are wired in. Better than defaulting to "ads on".
    AdSession(sessionCount = 0, tapCount = 0)
}
