package com.andriybobchuk.mooney.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun MooneyBannerAdView(
    adUnitId: String,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    val container = remember(adUnitId) {
        val v = UIView()
        v.backgroundColor = UIColor.clearColor
        val bridge = Ads.bridge
        println(
            "[MooneyAds] iOS banner factory: bridge=${bridge != null} " +
                "adUnitId=$adUnitId"
        )
        if (bridge == null) {
            println("[MooneyAds] iOS banner: bridge is NULL — no ad will render")
        }
        bridge?.attachBanner(v, adUnitId)
        v
    }
    UIKitView(
        factory = { container },
        modifier = modifier
            .fillMaxWidth()
            .height(STANDARD_BANNER_HEIGHT_DP.dp),
        update = { view ->
            // Trait-collection override propagates to the BannerView subview
            // the Swift bridge added; creatives that honor
            // UITraitCollection.userInterfaceStyle render dark when we're in
            // dark mode. Kept transparent so the Compose parent (which paints
            // MaterialTheme.colorScheme.surface) shows through before the
            // ad load completes.
            view.backgroundColor = UIColor.clearColor
            view.overrideUserInterfaceStyle = if (isDarkTheme) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark
            } else {
                UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }
        }
    )
}

private const val STANDARD_BANNER_HEIGHT_DP = 50
