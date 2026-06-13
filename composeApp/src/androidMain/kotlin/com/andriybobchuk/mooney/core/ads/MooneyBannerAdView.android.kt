package com.andriybobchuk.mooney.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun MooneyBannerAdView(adUnitId: String, modifier: Modifier) {
    // No-op until com.google.android.gms:play-services-ads is added. Returns
    // nothing rather than a placeholder Box — Android's launch is a couple
    // weeks after iOS, and showing a blank gray strip in the meantime is
    // worse UX than showing nothing at all.
}
