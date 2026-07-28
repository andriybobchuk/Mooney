import WidgetKit
import SwiftUI

/// "How much have I burned today?" — SwiftUI counterpart to
/// `TodaySpendingWidget.kt`. systemMedium only (needs the horizontal room
/// for the top-category row).
struct TodaySpendingWidget: Widget {
    let kind: String = "MooneyTodayWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider()) { entry in
            TodaySpendingWidgetView(snapshot: entry.snapshot)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Today's Spend")
        .description("What you've spent so far today, plus the top category.")
        .supportedFamilies([.systemMedium])
    }
}

struct TodaySpendingWidgetView: View {
    let snapshot: WidgetSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("SPENT TODAY")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.secondary)
            if snapshot.todaySpendingBase <= 0 && snapshot.todayTopCategory == nil {
                emptyState
            } else {
                Text(formatMoney(snapshot.todaySpendingBase, symbol: snapshot.baseCurrencySymbol))
                    .font(.system(size: 28, weight: .bold))
                    .lineLimit(1).minimumScaleFactor(0.6)
                if let top = snapshot.todayTopCategory {
                    HStack(spacing: 6) {
                        Text(top.emoji.isEmpty ? "💸" : top.emoji)
                            .font(.system(size: 14))
                        Text(top.title)
                            .font(.system(size: 12, weight: .medium))
                            .lineLimit(1)
                        Spacer()
                        Text(formatMoney(top.amountBase, symbol: snapshot.baseCurrencySymbol))
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(.red)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.primary.opacity(0.06),
                                in: RoundedRectangle(cornerRadius: 10))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .widgetURL(WidgetDeepLink.url(kind: "today", action: "home"))
    }

    private var emptyState: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(snapshot.baseCurrencySymbol)0 · a clean day")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(.green)
            Text("Log the first coffee to start your streak.")
                .font(.system(size: 11))
                .foregroundStyle(.secondary)
        }
    }
}
