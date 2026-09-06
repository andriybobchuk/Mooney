import SwiftUI
import FirebaseCore
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
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
        // Register as UNUserNotificationCenter delegate so we can fire
        // `notification_opened` when a user taps the reminder. Without
        // setting a delegate, UN silently drops the tap callback.
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    // Foreground presentation — show the reminder banner even when the app
    // is already open. Without this, iOS suppresses the notification until
    // background, which breaks parity with Android where the reminder is
    // always visible when it fires.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    // Fires when the user taps the notification (foreground OR background
    // tap — iOS routes both paths here). Delegates to the shared Kotlin
    // NotificationTelemetry so the Android + iOS attribution events land
    // in Firebase under the same name.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        NotificationTelemetry().markOpened()
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Deep-link fallback for pre-iOS-16 devices AND for Shortcut
                // authors who prefer a raw URL over the App Intent. Handles
                // mooney://add-tx?amount=12.5&type=expense&category=coffee&...
                // See AddMooneyTransactionIntent.swift for the parameter map.
                .onOpenURL { url in
                    handleMooneyURL(url)
                }
        }
    }

    private func handleMooneyURL(_ url: URL) {
        guard url.scheme == "mooney",
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              components.host == "add-tx" else { return }

        let items = components.queryItems ?? []
        func value(_ key: String) -> String? {
            items.first(where: { $0.name == key })?.value
        }

        guard let amountStr = value("amount"), let amount = Double(amountStr), amount > 0 else {
            return
        }
        let typeRaw = value("type")?.uppercased() ?? "EXPENSE"

        Task {
            let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()
            _ = try? await handler.addTransaction(
                amount: amount,
                typeRaw: typeRaw,
                categoryId: value("category"),
                accountTitle: value("account"),
                description: value("note"),
                isoDate: value("date")
            )
        }
    }
}