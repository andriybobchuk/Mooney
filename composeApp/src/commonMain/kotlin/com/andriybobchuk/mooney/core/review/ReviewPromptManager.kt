package com.andriybobchuk.mooney.core.review

/**
 * Platform-specific bridge to the native App Store / Play Store review prompt.
 *
 * Apple's review-prompt API silently throttles after ~3 prompts per user per
 * year, and outright forbids any pre-prompt UI ("Are you enjoying X?") in 2026.
 * The only lever we control is WHEN to call this — see [RequestReviewUseCase]
 * for the gating logic that decides whether to fire.
 */
expect class ReviewPromptManager() {
    /**
     * Fire-and-forget native prompt. No-op when the platform has no native
     * review API (e.g. desktop, or when called too frequently per platform rules).
     */
    fun requestReview()
}
