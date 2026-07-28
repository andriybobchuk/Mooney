import WidgetKit
import SwiftUI

/// TimelineProvider is WidgetKit's contract: given a placement, produce a
/// series of `TimelineEntry`s and tell the OS when to ask again. Our data is
/// event-driven (Kotlin nudges us via `WidgetCenter.reloadAllTimelines` on
/// every transaction change), so the timeline itself is trivial — just the
/// current snapshot and a "come back in an hour" refresh hint as a safety
/// net for the case where the main app is force-quit and can't nudge us.
struct SnapshotEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot
}

struct SnapshotProvider: TimelineProvider {
    /// Placeholder shown while the widget is still loading (first placement
    /// on the home screen, or when the OS wants a preview thumbnail).
    func placeholder(in context: Context) -> SnapshotEntry {
        SnapshotEntry(date: Date(), snapshot: .empty)
    }

    /// Snapshot for the widget picker + Smart Rotate. Load synchronously — no
    /// async work; UserDefaults reads on iOS are essentially free.
    func getSnapshot(in context: Context, completion: @escaping (SnapshotEntry) -> Void) {
        completion(SnapshotEntry(date: Date(), snapshot: WidgetSnapshot.loadFromAppGroup()))
    }

    /// Timeline: one entry (now) + a refresh policy of "check back in 1h" as
    /// a fallback for stale processes. The main app's WidgetCenter reload
    /// pings will normally re-run this method well before the hour is up.
    func getTimeline(in context: Context, completion: @escaping (Timeline<SnapshotEntry>) -> Void) {
        let entry = SnapshotEntry(date: Date(), snapshot: WidgetSnapshot.loadFromAppGroup())
        let nextRefresh = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
        completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
    }
}

// MARK: - Deep link URLs

/// Widget taps use SwiftUI's `.widgetURL(_:)` so WidgetKit hands the URL to
/// the main app via `onOpenURL`. We define one URL per kind + optional
/// action so the app can route + fire the analytics event.
enum WidgetDeepLink {
    static func url(kind: String, action: String) -> URL {
        URL(string: "mooney://widget/\(kind)/\(action)")!
    }
}
