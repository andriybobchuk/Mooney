import WidgetKit
import SwiftUI

/// Entry point for every widget the Mooney bundle ships.
///
/// WidgetKit uses `@main` on a `WidgetBundle` to discover the extension's
/// widgets — the launcher's picker enumerates whatever is listed here. Adding
/// a new widget = adding one more entry inside `body`.
@main
struct MooneyWidgetBundle: WidgetBundle {
    var body: some Widget {
        BalanceWidget()
        TodaySpendingWidget()
        BudgetProgressWidget()
        StreakWidget()
        QuickAddWidget()
    }
}
