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
    @State private var showLiquidGlassPreview = false

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Deep-link fallback for pre-iOS-16 devices AND for Shortcut
                // authors who prefer a raw URL over the App Intent. Handles
                // mooney://add-tx?amount=12.5&type=expense&category=coffee&...
                // See AddMooneyTransactionIntent.swift for the parameter map.
                //
                // Also handles mooney://liquid-glass-preview — presents the
                // SwiftUI + Liquid Glass PoC as a full-screen cover. Kept
                // behind a URL so the PoC ships zero UI in the production
                // build path; only reachable if user (or dev) explicitly
                // navigates to the URL (Safari, Shortcut, or Notes link).
                .onOpenURL { url in
                    handleMooneyURL(url)
                }
                .fullScreenCover(isPresented: $showLiquidGlassPreview) {
                    if #available(iOS 26.0, *) {
                        MooneyLiquidGlassPreview()
                    } else {
                        FallbackUnavailableView()
                    }
                }
        }
    }

    private func handleMooneyURL(_ url: URL) {
        guard url.scheme == "mooney",
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        else { return }

        switch components.host {
        case "liquid-glass-preview":
            // Launch the SwiftUI Liquid Glass PoC. Deliberately gated behind
            // a URL rather than a Settings row so the production Compose UI
            // ships zero surface area for this — the PoC is opt-in only.
            showLiquidGlassPreview = true

        case "add-tx":
            handleAddTransactionURL(components)

        default:
            break
        }
    }

    private func handleAddTransactionURL(_ components: URLComponents) {
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

/// Shown when the PoC URL is opened on a device running iOS below 26 — the
/// Liquid Glass material and several API tokens require iOS 26. Keeps the
/// experience honest instead of silently no-oping.
private struct FallbackUnavailableView: View {
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundStyle(.tint)
            Text("Liquid Glass preview requires iOS 26")
                .font(.headline)
            Text("This device is on an older iOS. Open Mooney on an iOS 26 device to see the preview.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Button("Close") { dismiss() }
                .buttonStyle(.borderedProminent)
                .padding(.top, 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(.regularMaterial)
    }
}