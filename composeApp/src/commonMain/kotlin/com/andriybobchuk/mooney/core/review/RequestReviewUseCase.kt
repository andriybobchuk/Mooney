package com.andriybobchuk.mooney.core.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Decides whether NOW is the right moment to ask the user for an App Store
 * review, then fires the native prompt if so.
 *
 * Apple silently throttles after ~3 prompts/year. To maximize the chance the
 * user actually sees the prompt (and reviews positively), we self-gate so we
 * never burn a request on a low-satisfaction or low-engagement user.
 *
 * Call this from "positive moments" only — e.g., right after the user shared
 * their net worth, completed a goal, or hit a transaction milestone. Calling
 * it from a generic place (app open, settings tap, etc.) is exactly the
 * pattern that leads to 3-star averages.
 */
class RequestReviewUseCase(
    private val dataStore: DataStore<Preferences>,
    private val transactionDao: TransactionDao,
    private val reviewPromptManager: ReviewPromptManager
) {
    /**
     * Returns true if NOW is an appropriate moment to show the pre-prompt dialog.
     * Caller is responsible for actually rendering the dialog and then calling
     * [confirmReviewRequested] (positive path) or [markPromptShown] (negative
     * path) so we record the cooldown either way.
     */
    @Suppress("ReturnCount")
    suspend fun shouldShowPrePrompt(): Boolean {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val prefs = dataStore.data.first()

            // 1. Once per install — the moment we've shown the pre-prompt on
            //    any positive moment, we're done. Auto-prompting a second
            //    time (even months later) reads as pestering; the manual
            //    "Rate Mooney" row in Settings still bypasses this gate.
            val lastPrompt = prefs[PreferencesKeys.LAST_REVIEW_PROMPT_TIMESTAMP] ?: 0L
            if (lastPrompt != 0L) return false

            // 2. Installed at least a week ago — filters out impulsive
            //    churners and gives the user real time to form an opinion.
            val installTs = prefs[PreferencesKeys.INSTALL_TIMESTAMP] ?: now
            if ((now - installTs) < MIN_INSTALL_AGE_MS) return false

            // 3. App opened at least N sessions — filters out one-touch users.
            val opens = prefs[PreferencesKeys.APP_OPEN_COUNT] ?: 0
            if (opens < MIN_OPEN_COUNT) return false

            // 4. User has actually used the app — N transactions created.
            val txCount = transactionDao.getAll().first().size
            if (txCount < MIN_TRANSACTION_COUNT) return false

            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fire the native review prompt and remember the timestamp. Called from
     * the pre-prompt's "yes" path AND from the Settings → Rate Mooney row.
     */
    suspend fun confirmReviewRequested() {
        try {
            reviewPromptManager.requestReview()
            markPromptShown()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
    }

    /**
     * Record that we asked the user about reviewing, regardless of whether
     * they said yes or no. Prevents nagging on the next positive moment.
     */
    suspend fun markPromptShown() {
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            dataStore.edit { it[PreferencesKeys.LAST_REVIEW_PROMPT_TIMESTAMP] = now }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
    }

    /**
     * Call once per cold app start to track open count + install date.
     * Safe to call on every start — the first install timestamp is preserved.
     */
    suspend fun recordAppOpen() {
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            dataStore.edit { prefs ->
                if ((prefs[PreferencesKeys.INSTALL_TIMESTAMP] ?: 0L) == 0L) {
                    prefs[PreferencesKeys.INSTALL_TIMESTAMP] = now
                }
                prefs[PreferencesKeys.APP_OPEN_COUNT] = (prefs[PreferencesKeys.APP_OPEN_COUNT] ?: 0) + 1
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
    }

    private companion object {
        const val MIN_INSTALL_AGE_MS = 7L * 24 * 60 * 60 * 1000  // 7 days
        const val MIN_OPEN_COUNT = 3
        const val MIN_TRANSACTION_COUNT = 5
    }
}
