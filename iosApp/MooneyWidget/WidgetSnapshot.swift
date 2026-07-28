import Foundation
import WidgetKit

/// Swift-side mirror of Kotlin's `WidgetDataSnapshot`. Field names and JSON
/// keys MUST stay in sync with `WidgetDataSnapshot.kt` — any drift silently
/// breaks the widget by decoding to zeros.
///
/// We hand-code the Codable representation rather than generate it because
/// the source of truth is the Kotlin side; a manual mirror is trivially small
/// (< 100 lines) and immune to build-order gotchas that would come from
/// piping the KMP framework into the widget extension.
struct WidgetSnapshot: Codable {
    let schemaVersion: Int
    let netWorthBase: Double
    let baseCurrencySymbol: String
    let baseCurrencyCode: String
    let todaySpendingBase: Double
    let todayTopCategory: WidgetCategoryLine?
    let monthSpendingBase: Double
    let monthIncomeBase: Double
    let budgetLines: [WidgetBudgetLine]
    let streakDays: Int
    let mooleyMood: String   // decoded from Kotlin's enum name — "HAPPY"/"WORRIED"/…
    let lastUpdatedMs: Int64
    let updateGeneration: Int64

    static let empty = WidgetSnapshot(
        schemaVersion: 1,
        netWorthBase: 0,
        baseCurrencySymbol: "$",
        baseCurrencyCode: "USD",
        todaySpendingBase: 0,
        todayTopCategory: nil,
        monthSpendingBase: 0,
        monthIncomeBase: 0,
        budgetLines: [],
        streakDays: 0,
        mooleyMood: "NEUTRAL",
        lastUpdatedMs: 0,
        updateGeneration: 0
    )

    /// Reads the JSON blob written by Kotlin's `IosWidgetSnapshotWriter`.
    /// Returns `.empty` on any failure so widgets never crash the host — a
    /// silent fallback is better than a fatal on the user's home screen.
    static func loadFromAppGroup() -> WidgetSnapshot {
        let defaults = UserDefaults(suiteName: WidgetGroup.suiteName)
            ?? UserDefaults.standard
        guard let raw = defaults.string(forKey: WidgetGroup.snapshotKey),
              let data = raw.data(using: .utf8) else { return .empty }
        return (try? JSONDecoder().decode(WidgetSnapshot.self, from: data)) ?? .empty
    }

    var mood: MooleyMood { MooleyMood(rawKotlin: mooleyMood) }
}

struct WidgetCategoryLine: Codable {
    let id: String
    let title: String
    let emoji: String
    let amountBase: Double
}

struct WidgetBudgetLine: Codable {
    let id: String
    let title: String
    let emoji: String
    let spentBase: Double
    let limitBase: Double

    var progress: Double { limitBase > 0 ? spentBase / limitBase : 0 }
    var isOverBudget: Bool { spentBase > limitBase }
}

/// SwiftUI-side enum mirroring `WidgetMooleyMood`. Decoded from the raw
/// Kotlin enum name (Kotlin serializes enums as `NAME` strings by default).
enum MooleyMood: String {
    case happy, neutral, worried, overBudget

    init(rawKotlin raw: String) {
        switch raw.uppercased() {
        case "HAPPY": self = .happy
        case "WORRIED": self = .worried
        case "OVER_BUDGET": self = .overBudget
        default: self = .neutral
        }
    }
}

/// Constants that also live on the Kotlin side in `WidgetSnapshotStorage.kt`.
/// If you change either constant, change both — the two processes can only
/// find each other via matching strings.
enum WidgetGroup {
    /// Must match `WidgetSnapshotStorage.IOS_APP_GROUP` in Kotlin.
    /// Must ALSO match the App Group configured in both the main app's and
    /// the widget extension's entitlements files, otherwise UserDefaults
    /// silently reads/writes the per-process container instead.
    static let suiteName = "group.com.andriybobchuk.mooney.widgets"

    /// Must match `WidgetSnapshotStorage.IOS_SNAPSHOT_KEY` in Kotlin.
    static let snapshotKey = "widget-snapshot-v1"
}
