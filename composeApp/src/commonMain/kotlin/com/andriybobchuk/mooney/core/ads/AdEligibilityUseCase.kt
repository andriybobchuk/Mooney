package com.andriybobchuk.mooney.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.premium.PremiumManager
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

/**
 * Single source of truth for "should we show this ad right now".
 *
 * Centralises every "don't be that app" rule so a UI surface never has to
 * second-guess. Returns simple booleans for grep-ability.
 *
 * Gating layers (applied in order):
 *   1. Premium → always denied
 *   2. New-user grace (first 3 sessions) → denied
 *   3. Per-placement rules:
 *      - Banners: always allowed once past grace
 *      - Interstitial: needs 20+ taps THIS session, max 1/session, AND 2h
 *        cooldown across sessions (was 5/session + 30min — way too noisy)
 *      - Rewarded: always allowed (user-initiated)
 *
 * The [FeatureFlags.adsAlwaysShow] bypass skips 2 and 3, leaving only the
 * Premium check. Useful for verifying ad UX without burning sessions.
 */
class AdEligibilityUseCase(
    private val dataStore: DataStore<Preferences>,
    private val premiumManager: PremiumManager
) {
    // Session-scoped counter — survives across ViewModels but resets on cold
    // start (this class is a Koin `single`). Max one interstitial per cold-
    // start session is the new hard ceiling.
    private var interstitialsShownThisSession = 0

    @Suppress("ReturnCount")
    suspend fun isEligible(
        placement: AdPlacement,
        sessionTapCount: Int,
        sessionCount: Int
    ): Boolean {
        // Highest-priority gate: developer kill-switch from Settings → Developer
        // Options. Wins over everything else (including FeatureFlags.adsAlwaysShow)
        // so a dev can preview the ad-free UX inside the same build that
        // normally serves ads. Read directly from DataStore — there's no
        // separate observer because eligibility checks are async already.
        val prefsForKillSwitch = dataStore.data.first()
        if (prefsForKillSwitch[PreferencesKeys.ADS_DISABLED_DEV] == true) return false

        // Test-everywhere bypass: skips grace + cooldown so dev/QA can verify
        // every ad surface. Premium still denies. See FeatureFlags.adsAlwaysShow.
        if (FeatureFlags.adsAlwaysShow) {
            val isPremium = premiumManager.getIsPremium()
            // Even in "always-show" mode, cap interstitials at 1 per session
            // so you can verify the cap logic doesn't break — not for UX
            // (this flag exists for verification, not production).
            if (placement == AdPlacement.INTERSTITIAL_RETURN_TO_TRANSACTIONS &&
                interstitialsShownThisSession >= MAX_INTERSTITIALS_PER_SESSION
            ) {
                return false
            }
            return !isPremium
        }

        if (premiumManager.getIsPremium()) return false

        if (sessionCount < NEW_USER_GRACE_SESSIONS) return false

        val now = Clock.System.now().toEpochMilliseconds()
        val prefs = dataStore.data.first()

        return when (placement) {
            AdPlacement.TRANSACTIONS_NATIVE_ROW -> true

            // Banners run on a per-placement time cooldown so a user who
            // keeps re-opening Analytics doesn't get an ad every time. The
            // cooldown is enforced at eligibility time; the call site is
            // expected to invoke [markShown] right after rendering.
            AdPlacement.SETTINGS_BANNER -> bannerCooledDown(prefs, PreferencesKeys.ADS_LAST_BANNER_SETTINGS, now)
            AdPlacement.ANALYTICS_BREAKDOWN_BANNER -> bannerCooledDown(prefs, PreferencesKeys.ADS_LAST_BANNER_ANALYTICS, now)
            AdPlacement.CATEGORIES_BANNER -> bannerCooledDown(prefs, PreferencesKeys.ADS_LAST_BANNER_CATEGORIES, now)

            AdPlacement.INTERSTITIAL_RETURN_TO_TRANSACTIONS -> {
                // Hard cap: at most one interstitial per cold-start session.
                if (interstitialsShownThisSession >= MAX_INTERSTITIALS_PER_SESSION) return false
                // Engagement threshold: user has actually been using the app
                // for a while (20+ nav transitions) before we interrupt them.
                if (sessionTapCount < INTERSTITIAL_MIN_TAPS) return false
                // Cross-session cooldown — 2 hours between any two interstitials
                // regardless of session boundaries.
                val last = prefs[PreferencesKeys.ADS_LAST_INTERSTITIAL_TIMESTAMP] ?: 0L
                (now - last) >= INTERSTITIAL_COOLDOWN_MS
            }

            AdPlacement.REWARDED_FEATURE_UNLOCK -> true
        }
    }

    /** Call this after a placement actually showed, so counters update. */
    suspend fun markShown(placement: AdPlacement) {
        val now = Clock.System.now().toEpochMilliseconds()
        when (placement) {
            AdPlacement.INTERSTITIAL_RETURN_TO_TRANSACTIONS -> {
                interstitialsShownThisSession += 1
                dataStore.edit { it[PreferencesKeys.ADS_LAST_INTERSTITIAL_TIMESTAMP] = now }
            }
            AdPlacement.SETTINGS_BANNER ->
                dataStore.edit { it[PreferencesKeys.ADS_LAST_BANNER_SETTINGS] = now }
            AdPlacement.ANALYTICS_BREAKDOWN_BANNER ->
                dataStore.edit { it[PreferencesKeys.ADS_LAST_BANNER_ANALYTICS] = now }
            AdPlacement.CATEGORIES_BANNER ->
                dataStore.edit { it[PreferencesKeys.ADS_LAST_BANNER_CATEGORIES] = now }
            else -> Unit
        }
    }

    private fun bannerCooledDown(
        prefs: Preferences,
        key: androidx.datastore.preferences.core.Preferences.Key<Long>,
        nowMs: Long
    ): Boolean {
        val last = prefs[key] ?: 0L
        return (nowMs - last) >= BANNER_COOLDOWN_MS
    }

    private companion object {
        const val NEW_USER_GRACE_SESSIONS = 3
        // Was 5 — too low, fired after barely opening the app. Bumped so the
        // user has to be meaningfully engaged (~scroll through Transactions,
        // open Settings, tap a category) before an interstitial fires.
        const val INTERSTITIAL_MIN_TAPS = 20
        // Was 30 min — felt aggressive on heavy usage sessions. 2 hours is
        // closer to "once or twice per day for an engaged user".
        const val INTERSTITIAL_COOLDOWN_MS = 2L * 60L * 60L * 1000L
        const val MAX_INTERSTITIALS_PER_SESSION = 1
        // Same-placement banner cooldown — keeps Analytics/Settings/Categories
        // banners from feeling like every-screen interrupts. 12h means a heavy
        // user sees a banner at most twice a day per surface.
        const val BANNER_COOLDOWN_MS = 12L * 60L * 60L * 1000L
    }
}
