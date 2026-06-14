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