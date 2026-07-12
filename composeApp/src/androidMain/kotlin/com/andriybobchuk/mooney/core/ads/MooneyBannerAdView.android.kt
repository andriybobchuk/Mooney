package com.andriybobchuk.mooney.core.ads

import android.graphics.Color
import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Android banner ad — wraps a Google Mobile Ads [AdView] inside [AndroidView].
 * Uses [AdSize.BANNER] (320x50) so the slot reserves a predictable Compose
 * height up-front and avoids layout jumps when the ad loads. Container
 * background is transparent so the Compose parent (which paints the app's
 * surface color) shows through — no white flash before load.
 *
 * Every SDK event is logged with the `MooneyAds` tag so `adb logcat -s
 * MooneyAds` shows the full lifecycle (load requested → success/failure →
 * impression/click).
 */
@Composable
actual fun MooneyBannerAdView(
    adUnitId: String,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(STANDARD_BANNER_HEIGHT_DP.dp),
        factory = { context ->
            Log.i(TAG, "banner factory: creating AdView adUnitId=$adUnitId dark=$isDarkTheme")
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                setBackgroundColor(Color.TRANSPARENT)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.i(TAG, "banner LOADED adUnitId=$adUnitId")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(
                            TAG,
                            "banner FAILED code=${error.code} msg=${error.message} " +
                                "domain=${error.domain} responseInfo=${error.responseInfo}"
                        )
                    }
                    override fun onAdImpression() {
                        Log.i(TAG, "banner IMPRESSION adUnitId=$adUnitId")
                    }
                    override fun onAdClicked() {
                        Log.i(TAG, "banner CLICK adUnitId=$adUnitId")
                    }
                    override fun onAdOpened() {
                        Log.i(TAG, "banner OPENED adUnitId=$adUnitId")
                    }
                    override fun onAdClosed() {
                        Log.i(TAG, "banner CLOSED adUnitId=$adUnitId")
                    }
                }
                Log.i(TAG, "banner loadAd() called for $adUnitId")
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { view ->
            view.setBackgroundColor(Color.TRANSPARENT)
            applyWebViewDarkMode(view, isDarkTheme)
        }
    )
}

private fun applyWebViewDarkMode(root: android.view.View, isDarkTheme: Boolean) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) return
    findWebViews(root).forEach { webView ->
        runCatching {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
        }
    }
}

private fun findWebViews(view: android.view.View): List<WebView> {
    if (view is WebView) return listOf(view)
    if (view !is android.view.ViewGroup) return emptyList()
    val result = mutableListOf<WebView>()
    for (i in 0 until view.childCount) {
        result += findWebViews(view.getChildAt(i))
    }
    return result
}

private const val STANDARD_BANNER_HEIGHT_DP = 50
private const val TAG = "MooneyAds"
