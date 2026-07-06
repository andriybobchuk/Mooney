package com.andriybobchuk.mooney.core.ads

import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Android banner ad — wraps a Google Mobile Ads [AdView] inside [AndroidView].
 *
 * Uses [AdSize.BANNER] (320x50) rather than `SMART_BANNER` / adaptive sizes so
 * the slot reserves a predictable Compose height up-front and avoids layout
 * jumps when the ad loads. Eligibility / placement gating is handled by
 * [AdBannerSlot].
 *
 * `AdView` defaults to a solid white background. In dark mode that white shows
 * as a full-width flash between when the slot appears and when the ad creative
 * paints. Setting the AdView background to transparent lets the Compose parent
 * (which paints the app's surface color) show through instead.
 *
 * Ad creatives themselves are HTML rendered inside a WebView the SDK owns. On
 * Android 10+ we ask WebViewCompat to force-dark that WebView so creatives
 * that don't explicitly opt into dark mode still render darker greys instead
 * of pure white. It's a best-effort hint — some publishers ignore it — but
 * it's the closest lever AdMob gives us today.
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
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                setBackgroundColor(Color.TRANSPARENT)
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { view ->
            view.setBackgroundColor(Color.TRANSPARENT)
            // The ad creative lives in a WebView inside the AdView. Walk the
            // hierarchy and force-dark it on Android 10+ where the API exists.
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
