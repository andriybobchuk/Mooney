package com.andriybobchuk.mooney.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import platform.UIKit.UIView

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun MooneyBannerAdView(adUnitId: String, modifier: Modifier) {
    // Compose creates a plain container UIView, then the Swift bridge inserts
    // a GADBannerView inside it and kicks off the load. UIView only appears
    // as a parameter to the bridge method, never as a return — that signature
    // shape works cleanly with the patched ComposeApp framework header.
    val container = remember(adUnitId) {
        val v = UIView()
        Ads.bridge?.attachBanner(v, adUnitId)
        println("[Ads] MooneyBannerAdView: created container=$v, bridge=${Ads.bridge}, adUnitId=$adUnitId")
        v
    }
    UIKitView(
        factory = { container },
        modifier = modifier
            .fillMaxWidth()
            .height(STANDARD_BANNER_HEIGHT_DP.dp)
    )
}

private const val STANDARD_BANNER_HEIGHT_DP = 50
