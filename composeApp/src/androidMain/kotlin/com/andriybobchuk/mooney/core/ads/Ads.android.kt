package com.andriybobchuk.mooney.core.ads

import android.content.Context
import android.util.Log
import com.andriybobchuk.mooney.core.premium.ActivityProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.koin.core.context.GlobalContext

/**
 * Android implementation backed by the Google Mobile Ads SDK
 * (`com.google.android.gms:play-services-ads`).
 *
 * Matches the iOS `AdMobBridge.swift` feature set:
 * - Banner ads live in [MooneyBannerAdView].
 * - Interstitial ads pre-load into [interstitialAd] and are shown via
 *   [showInterstitialIfReady], then auto-reloaded after dismissal so the
 *   next placement doesn't wait for a full network round-trip.
 * - Rewarded ads follow the same load-show-reload lifecycle. `onReward`
 *   fires only when the user watches to completion; `onDismissed` always
 *   fires (whether they earned or bailed).
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Ads {

    private const val TAG = "Ads"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var initialized: Boolean = false

    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialAdUnitId: String? = null

    private var rewardedAd: RewardedAd? = null
    private var lastRewardedAdUnitId: String? = null

    private var pendingRewardedOnDismissed: (() -> Unit)? = null

    fun setApplication(context: Context) {
        appContext = context.applicationContext
    }

    actual fun initialize() {
        if (initialized) {
            Log.i(TAG, "initialize(): already done, skipping")
            return
        }
        val ctx = appContext
        if (ctx == null) {
            Log.w(TAG, "initialize() called before setApplication() — skipping")
            return
        }
        Log.i(TAG, "initialize(): calling MobileAds.initialize()")
        MobileAds.initialize(ctx) { status ->
            val summary = status.adapterStatusMap.entries.joinToString {
                "${it.key}=${it.value.initializationState}"
            }
            Log.i(TAG, "MobileAds initialized: $summary")
        }
        initialized = true
    }

    actual fun preloadInterstitial(adUnitId: String) {
        val ctx = appContext ?: return
        lastInterstitialAdUnitId = adUnitId
        InterstitialAd.load(
            ctx,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = interstitialCallback
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "interstitial load failed: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    actual fun showInterstitialIfReady(): Boolean {
        val ad = interstitialAd ?: return false
        val activity = activity() ?: return false
        ad.show(activity)
        interstitialAd = null
        return true
    }

    actual fun preloadRewarded(adUnitId: String) {
        val ctx = appContext ?: return
        lastRewardedAdUnitId = adUnitId
        RewardedAd.load(
            ctx,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "rewarded load failed: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    actual fun showRewarded(onReward: () -> Unit, onDismissed: () -> Unit) {
        val ad = rewardedAd
        val activity = activity()
        if (ad == null || activity == null) {
            onDismissed()
            return
        }
        pendingRewardedOnDismissed = onDismissed
        ad.fullScreenContentCallback = rewardedCallback
        ad.show(activity) {
            onReward()
        }
        rewardedAd = null
    }

    private val interstitialCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            lastInterstitialAdUnitId?.let { preloadInterstitial(it) }
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            Log.w(TAG, "interstitial show failed: ${error.message}")
        }
    }

    private val rewardedCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            pendingRewardedOnDismissed?.invoke()
            pendingRewardedOnDismissed = null
            lastRewardedAdUnitId?.let { preloadRewarded(it) }
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            Log.w(TAG, "rewarded show failed: ${error.message}")
            pendingRewardedOnDismissed?.invoke()
            pendingRewardedOnDismissed = null
        }
    }

    private fun activity() =
        GlobalContext.getOrNull()?.get<ActivityProvider>()?.getActivity()
}
