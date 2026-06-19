package com.andriybobchuk.mooney.core.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds

/**
 * Android implementation backed by the Google Mobile Ads SDK
 * (`com.google.android.gms:play-services-ads`).
 *
 * ## Wiring
 *
 * The common [Ads.initialize] signature takes no parameters, but the Google
 * SDK needs an `applicationContext`. To bridge that without leaking changes
 * into the expect-fun shape, `MyApp.onCreate()` calls [setApplication] right
 * after Koin is initialised — this stashes the Application reference (not an
 * Activity, so it's safe), which [initialize] then reads. Mirrors the
 * `setBridge(...)` pattern the iOS actual already uses.
 *
 * Banner ads live in [MooneyBannerAdView]; this object only handles SDK init
 * and (later) full-screen formats. Interstitial and rewarded support are
 * stubbed for now — they're not on the immediate Android critical path and
 * iOS already has the production pipeline.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Ads {

    private const val TAG = "Ads"

    // Application context (never an Activity) — set once from MyApp.onCreate
    // before initialize() runs. Volatile because callers may sit on different
    // threads (warm-startup vs main).
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var initialized: Boolean = false

    /** Called from `MyApp.onCreate()` before [initialize]. Idempotent. */
    fun setApplication(context: Context) {
        appContext = context.applicationContext
    }

    actual fun initialize() {
        if (initialized) return
        val ctx = appContext
        if (ctx == null) {
            Log.w(TAG, "initialize() called before setApplication() — skipping")
            return
        }
        MobileAds.initialize(ctx) { status ->
            Log.i(TAG, "MobileAds initialized: ${status.adapterStatusMap}")
        }
        initialized = true
    }

    actual fun preloadInterstitial(adUnitId: String) {
        // Not yet implemented on Android — iOS handles full-screen ads today.
        // Banner ads are the immediate Android need; interstitial preloading
        // will land in a follow-up.
    }

    actual fun showInterstitialIfReady(): Boolean = false

    actual fun preloadRewarded(adUnitId: String) {
        // Not yet implemented on Android — see [preloadInterstitial].
    }

    actual fun showRewarded(onReward: () -> Unit, onDismissed: () -> Unit) {
        // No rewarded pipeline on Android yet — treat as "user dismissed
        // without reward" so callers' UI doesn't hang waiting for a callback.
        onDismissed()
    }
}
