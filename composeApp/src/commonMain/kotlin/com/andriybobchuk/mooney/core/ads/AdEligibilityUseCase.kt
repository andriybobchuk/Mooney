package com.andriybobchuk.mooney.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.premium.PremiumManager
import com.andriybobchuk.mooney.core.premium.isBillingEnabled
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
        val prefsForKillSwitch = dataStore.data.first()
        if (prefsForKillSwitch[PreferencesKeys.ADS_DISABLED_DEV] == true) {
            println("[Ads] $placement DENY: ADS_DISABLED_DEV is on")
            return false
        }

        // Premium gates ads — but ONLY when billing is actually enabled on
        // this platform. Android keeps `isBillingEnabled=false` and stubs
        // every user as Premium so the paywall stays hidden; without this
        // guard that stub would also silently hide every ad placement.
        val premiumGatesAds = isBillingEnabled && premiumManager.getIsPremium()

        // Test-everywhere bypass — flag in FeatureFlags OR runtime dev toggle
        // (ADS_FORCE_SHOW_DEV in Settings → Developer Options). Either one
        // skips grace + cooldown so we can verify every ad surface immediately.
        val forceShowDev = prefsForKillSwitch[PreferencesKeys.ADS_FORCE_SHOW_DEV] == true
        if (FeatureFlags.adsAlwaysShow || forceShowDev) {
            if (placement == AdPlacement.INTERSTITIAL_RETURN_TO_TRANSACTIONS &&
                interstitialsShownThisSession >= MAX_INTERSTITIALS_PER_SESSION
            ) {
                println("[Ads] $placement DENY: interstitial cap hit (even with force-show)")
                return false
            }
            println("[Ads] $placement ALLOW: force-show on (premium=$premiumGatesAds)")
            return !premiumGatesAds
        }

        if (premiumGatesAds) {
            println("[Ads] $placement DENY: premium (billing=$isBillingEnabled)")
            return false
        }

        if (sessionCount < NEW_USER_GRACE_SESSIONS) {
            println("[Ads] $placement DENY: new-user grace ($sessionCount<$NEW_USER_GRACE_SESSIONS)")
            return false
        }

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
            AdPlacement.ASSETS_BANNER -> bannerCooledDown(prefs, PreferencesKeys.ADS_LAST_BANNER_ASSETS, now)

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
            AdPlacement.ASSETS_BANNER ->
                dataStore.edit { it[PreferencesKeys.ADS_LAST_BANNER_ASSETS] = now }
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
        // Zero grace: banners are unobtrusive and pre-launch testers report
        // that "no ads at all" is more disorienting than an ad on session 1.
        // Interstitials still gated hard by INTERSTITIAL_MIN_TAPS.
        const val NEW_USER_GRACE_SESSIONS = 0
        const val INTERSTITIAL_MIN_TAPS = 20
        const val INTERSTITIAL_COOLDOWN_MS = 2L * 60L * 60L * 1000L
        const val MAX_INTERSTITIALS_PER_SESSION = 1
        // Was 12h — meant a screen showed a banner exactly once, then went
        // dark for the rest of the day. Users read that as "ads broken" and
        // reported it. 15 min is closer to "same session, keep it fresh".
        const val BANNER_COOLDOWN_MS = 15L * 60L * 1000L
    }
}
