package com.andriybobchuk.mooney.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Android banner ad — wraps a Google Mobile Ads [AdView] inside [AndroidView].
 *
 * Uses [AdSize.BANNER] (320x50) rather than `SMART_BANNER` / adaptive sizes so
 * the slot reserves a predictable Compose height up-front and avoids layout
 * jumps when the ad loads. Eligibility / placement gating is handled by
 * [AdBannerSlot] — this composable just renders whatever it's told to.
 */
@Composable
actual fun MooneyBannerAdView(adUnitId: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(STANDARD_BANNER_HEIGHT_DP.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

private const val STANDARD_BANNER_HEIGHT_DP = 50
