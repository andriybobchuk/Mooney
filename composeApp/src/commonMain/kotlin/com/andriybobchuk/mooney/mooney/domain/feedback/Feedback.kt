package com.andriybobchuk.mooney.mooney.domain.feedback

/**
 * Type of in-app feedback the user is submitting. Drives the kind chip on
 * the feedback sheet and lets us triage in the Firestore console.
 */
enum class FeedbackKind(val label: String, val emoji: String) {
    GENERAL("General", "💬"),
    BUG("Bug", "🐛"),
    FEATURE("Feature idea", "✨"),
    WIDGET("Widget idea", "📊")
}

interface FeedbackRepository {
    /**
     * Persists feedback to the backing store. Returns true on success.
     * Implementations must NOT throw — network/permission errors must be
     * caught and reported as `false` so the UI can show a retry hint.
     */
    suspend fun submit(kind: FeedbackKind, body: String): Boolean
}
