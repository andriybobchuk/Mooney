package com.andriybobchuk.mooney.core.review

/**
 * Android implementation is intentionally a no-op for now. We're not yet
 * distributing through Play Console (verification issues), and integrating the
 * Google Play In-App Review SDK would pull in a Play-Services dep that we
 * don't need until we're actually live on Play. Wire up when Android ships.
 */
actual class ReviewPromptManager {
    actual fun requestReview() {
        // no-op
    }
}
