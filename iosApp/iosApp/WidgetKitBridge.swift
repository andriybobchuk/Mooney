import ComposeApp
import WidgetKit

/// Swift-side implementation of Kotlin's `IosWidgetKitBridge` protocol.
/// Registered from `iOSApp.swift`'s AppDelegate; Kotlin calls
/// `reloadAllTimelines()` whenever the widget snapshot changes, and this
/// bridge forwards the call to WidgetKit so every installed widget refreshes.
///
/// Fails silently if WidgetKit is unavailable (unlikely on iOS 14+, but the
/// bridge stays defensive so a rare init failure never crashes the main app).
class WidgetKitBridgeImpl: NSObject, IosWidgetKitBridge {
    func reloadAllTimelines() {
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}
