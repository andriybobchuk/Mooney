package com.andriybobchuk.mooney.core.ads

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
 * Delegates the actual UIView/View creation to [MooneyBannerAdView] (iOS uses
 * `UIKitView` hosting `GADBannerView`; Android is a no-op until we wire
 * play-services-ads).
 */
@Composable
fun AdBannerSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier
) {
    val eligibility: AdEligibilityUseCase = koinInject()
    val session = LocalAdSession.current
    var eligible by remember(placement, session) { mutableStateOf(false) }

    LaunchedEffect(placement, session) {
        eligible = eligibility.isEligible(
            placement = placement,
            sessionTapCount = session.tapCount,
            sessionCount = session.sessionCount
        )
        if (eligible) {
            // Stamp the cooldown immediately so the next visit to this
            // placement (within the cooldown window) won't show another ad,
            // even if the user backgrounds the app before the impression
            // finishes loading.
            eligibility.markShown(placement)
        }
        println("[Ads] AdBannerSlot $placement: eligible=$eligible (session=$session)")
    }

    if (!eligible) return

    println("[Ads] AdBannerSlot $placement: rendering banner with adUnitId=${AdUnitIds.banner}")
    MooneyBannerAdView(
        adUnitId = AdUnitIds.banner,
        modifier = modifier
    )
}
