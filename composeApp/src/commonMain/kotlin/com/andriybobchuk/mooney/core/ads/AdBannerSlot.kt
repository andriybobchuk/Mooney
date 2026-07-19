package com.andriybobchuk.mooney.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

/**
 * Drop-in banner ad slot. Place at the bottom of a screen; renders nothing if
 * the user is Premium / new / otherwise ineligible.
 *
 * Paints the current theme's surface color behind the ad so the container
 * blends with the screen — otherwise the AdView's default white background
 * flashes during load (especially jarring in dark mode). Passes the current
 * dark-mode state to the platform banner so ad creatives that honor the trait
 * environment (iOS) or WebView algorithmic darkening (Android 10+) render in
 * the right mode.
 */
@Composable
fun AdBannerSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier
) {
    // Master kill switch — every ad slot bails out here before touching Koin
    // or the eligibility use case. Keeps first-launch release surfaces clean.
    if (!com.andriybobchuk.mooney.mooney.domain.FeatureFlags.adsEnabled) return
    val eligibility: AdEligibilityUseCase = koinInject()
    val session = LocalAdSession.current
    val isDarkTheme = isSystemInDarkTheme()
    var eligible by remember(placement, session) { mutableStateOf(false) }

    LaunchedEffect(placement, session) {
        eligible = eligibility.isEligible(
            placement = placement,
            sessionTapCount = session.tapCount,
            sessionCount = session.sessionCount
        )
        println(
            "[MooneyAds] AdBannerSlot $placement eligibility check: " +
                "eligible=$eligible session=$session"
        )
        if (eligible) {
            eligibility.markShown(placement)
        }
    }

    if (!eligible) return

    println(
        "[MooneyAds] AdBannerSlot $placement rendering: " +
            "adUnitId=${AdUnitIds.banner} dark=$isDarkTheme"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        MooneyBannerAdView(
            adUnitId = AdUnitIds.banner,
            isDarkTheme = isDarkTheme
        )
    }
}
