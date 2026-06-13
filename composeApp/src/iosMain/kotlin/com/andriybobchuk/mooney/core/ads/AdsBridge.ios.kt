package com.andriybobchuk.mooney.core.ads

import platform.UIKit.UIView

/**
 * Implemented by `AdMobBridge.swift`. Lives in iosMain (not commonMain) so
 * `makeBannerView` can reference `UIView` directly — that triggers
 * Kotlin/Native to put `use UIKit` in the framework's modulemap, and Swift
 * can then bind the protocol's `UIView` to the real `UIKit.UIView` class.
 *
 * With the previous commonMain placement + `Any` return, Swift saw the
 * protocol returning a forward-declared opaque `UIView` from the framework
 * namespace, which didn't match the Swift impl's `UIKit.UIView` — protocol
 * conformance always failed.
 */
interface IosAdsBridge {
    fun initialize()
    fun preloadInterstitial(adUnitId: String)
    /** Synchronous: returns whether the SDK actually presented an ad. */
    fun showInterstitialIfReady(): Boolean
    fun preloadRewarded(adUnitId: String)
    fun showRewarded(onReward: () -> Unit, onDismissed: () -> Unit)
    /**
     * Attaches a fresh `GADBannerView` as a subview of [container] and kicks
     * off the ad load. Caller owns the container (Compose-created via
     * `UIKitView`); the bridge owns the banner inside it. Passes UIView as
     * a parameter rather than returning it because Swift's protocol
     * conformance check accepts `UIKit.UIView` parameters cleanly even when
     * a `UIKit.UIView` return type trips a phantom-forward-decl issue.
     */
    fun attachBanner(container: UIView, adUnitId: String)
}
