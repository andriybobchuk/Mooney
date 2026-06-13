import ComposeApp
import Foundation
import GoogleMobileAds
import UIKit

// Google Mobile Ads bridge. Mirrors the pattern of FirebaseAnalyticsBridge:
// a Swift class implementing the Kotlin `IosAdsBridge` protocol, registered
// into `Ads.shared.setBridge(...)` from `iOSApp.swift`'s
// `didFinishLaunchingWithOptions`.
//
// SDK 13.x renamed every `GAD*` symbol to its un-prefixed form: GADBannerView
// → BannerView, GADRequest → Request, GADMobileAds → MobileAds, etc. This
// file uses the new names.
//
// Info.plist is configured with:
//   - GADApplicationIdentifier = ca-app-pub-7021633711522076~6326300426
//   - Full SKAdNetworkItems list
//
// Ad unit IDs come from Kotlin (AdUnitIds.kt) so debug/release switching is
// centralized. Real iOS IDs:
//   Banner       : ca-app-pub-7021633711522076/1005395834
//   Interstitial : ca-app-pub-7021633711522076/3061884393
//   Rewarded     : ca-app-pub-7021633711522076/1640949952
class AdMobBridge: NSObject, IosAdsBridge, FullScreenContentDelegate, BannerViewDelegate {

    private var interstitialAd: InterstitialAd?
    private var rewardedAd: RewardedAd?

    private var lastInterstitialAdUnitId: String?
    private var lastRewardedAdUnitId: String?

    private enum LastPresented { case none, interstitial, rewarded }
    private var lastPresented: LastPresented = .none

    private var pendingRewardedOnDismissed: (() -> Void)?

    func initialize() {
        MobileAds.shared.start(completionHandler: nil)
    }

    func preloadInterstitial(adUnitId: String) {
        lastInterstitialAdUnitId = adUnitId
        InterstitialAd.load(
            with: adUnitId,
            request: Request()
        ) { [weak self] ad, error in
            guard let self = self else { return }
            if let error = error {
                NSLog("[Ads] interstitial load failed: \(String(describing: error))")
                self.interstitialAd = nil
                return
            }
            ad?.fullScreenContentDelegate = self
            self.interstitialAd = ad
        }
    }

    func showInterstitialIfReady() -> Bool {
        guard let interstitial = self.interstitialAd,
              let root = topMostViewController() else {
            return false
        }
        lastPresented = .interstitial
        interstitial.present(from: root)
        self.interstitialAd = nil
        return true
    }

    func preloadRewarded(adUnitId: String) {
        lastRewardedAdUnitId = adUnitId
        RewardedAd.load(
            with: adUnitId,
            request: Request()
        ) { [weak self] ad, error in
            guard let self = self else { return }
            if let error = error {
                NSLog("[Ads] rewarded load failed: \(String(describing: error))")
                self.rewardedAd = nil
                return
            }
            ad?.fullScreenContentDelegate = self
            self.rewardedAd = ad
        }
    }

    func showRewarded(onReward: @escaping () -> Void, onDismissed: @escaping () -> Void) {
        guard let rewarded = self.rewardedAd,
              let root = topMostViewController() else {
            onDismissed()
            return
        }
        pendingRewardedOnDismissed = onDismissed
        lastPresented = .rewarded
        rewarded.present(from: root) {
            onReward()
        }
        self.rewardedAd = nil
    }

    func attachBanner(container: UIView, adUnitId: String) {
        NSLog("[Ads] attachBanner: adUnitId=\(adUnitId), container=\(container)")
        let bannerView = BannerView(adSize: AdSizeBanner)
        bannerView.adUnitID = adUnitId
        bannerView.rootViewController = topMostViewController()
        bannerView.delegate = self
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(bannerView)
        NSLayoutConstraint.activate([
            bannerView.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            bannerView.centerYAnchor.constraint(equalTo: container.centerYAnchor)
        ])
        NSLog("[Ads] banner rootVC=\(String(describing: bannerView.rootViewController))")
        bannerView.load(Request())
        NSLog("[Ads] banner load() requested")
    }

    // MARK: - FullScreenContentDelegate

    func ad(
        _ ad: FullScreenPresentingAd,
        didFailToPresentFullScreenContentWithError error: Error
    ) {
        NSLog("[Ads] present failed: \(String(describing: error))")
        if let onDismissed = pendingRewardedOnDismissed {
            pendingRewardedOnDismissed = nil
            onDismissed()
        }
        lastPresented = .none
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        if let onDismissed = pendingRewardedOnDismissed {
            pendingRewardedOnDismissed = nil
            onDismissed()
        }
        switch lastPresented {
        case .interstitial:
            if let id = lastInterstitialAdUnitId {
                preloadInterstitial(adUnitId: id)
            }
        case .rewarded:
            if let id = lastRewardedAdUnitId {
                preloadRewarded(adUnitId: id)
            }
        case .none:
            break
        }
        lastPresented = .none
    }

    // MARK: - BannerViewDelegate

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        NSLog("[Ads] banner DID receive ad: size=\(bannerView.adSize.size)")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        NSLog("[Ads] banner FAILED to receive ad: \(String(describing: error))")
    }

    func bannerViewWillPresentScreen(_ bannerView: BannerView) {
        NSLog("[Ads] banner click — will present screen")
    }

    private func topMostViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
