import SwiftUI
import FirebaseCore
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Analytics.shared.setBridge(bridge: FirebaseAnalyticsBridge())
        RemoteConfig.shared.setBridge(bridge: RemoteConfigBridge())
        // Ads — currently a no-op bridge. When the Google Mobile Ads SwiftPM
        // dependency is added (see AdMobBridge.swift header), this single
        // setBridge call is all that activates the SDK from app launch.
        Ads.shared.setBridge(bridge: AdMobBridge())
        Ads.shared.initialize()
        // Billing — wire the Swift StoreKit 2 bridge into the Koin-managed
        // IosBillingManager so purchase() uses native async/await instead of
        // the Kotlin-side SKPaymentQueue path. Eliminates the stuck-spinner
        // failure mode we hit on iPad. iOS 15+ required for StoreKit 2.
        if #available(iOS 15.0, *) {
            IosBillingBridgeKt.setIosBillingBridge(bridge: IosBillingBridgeImpl())
        }
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}