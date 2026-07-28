import WidgetKit
import SwiftUI

/// Net worth at a glance — SwiftUI counterpart to
/// `composeApp/src/androidMain/.../BalanceWidget.kt`. Two sizes: systemSmall
/// (2×2) and systemMedium (4×2).
struct BalanceWidget: Widget {
    let kind: String = "MooneyBalanceWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider()) { entry in
            BalanceWidgetView(snapshot: entry.snapshot)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Net Worth")
        .description("Your total net worth at a glance — updates the moment you log a transaction.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct BalanceWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let snapshot: WidgetSnapshot

    var body: some View {
        switch family {
        case .systemSmall: SmallBalance(snapshot: snapshot)
        default: MediumBalance(snapshot: snapshot)
        }
    }
}

private struct SmallBalance: View {
    let snapshot: WidgetSnapshot
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("NET WORTH")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(.secondary)
            Text(formatMoney(snapshot.netWorthBase, symbol: snapshot.baseCurrencySymbol))
                .font(.system(size: 22, weight: .bold))
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Spacer(minLength: 0)
            Text("Mooney")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(.tint)
        }
        .padding(12)
        .widgetURL(WidgetDeepLink.url(kind: "balance", action: "assets"))
    }
}

private struct MediumBalance: View {
    let snapshot: WidgetSnapshot
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text("NET WORTH")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.secondary)
                Text("Mooney")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(.tint)
            }
            Text(formatMoney(snapshot.netWorthBase, symbol: snapshot.baseCurrencySymbol))
                .font(.system(size: 30, weight: .bold))
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            HStack(spacing: 8) {
                MonthPill(label: "IN", amount: snapshot.monthIncomeBase,
                          symbol: snapshot.baseCurrencySymbol, tint: .green)
                MonthPill(label: "OUT", amount: snapshot.monthSpendingBase,
                          symbol: snapshot.baseCurrencySymbol, tint: .red)
                Spacer(minLength: 0)
            }
        }
        .padding(12)
        .widgetURL(WidgetDeepLink.url(kind: "balance", action: "assets"))
    }
}

private struct MonthPill: View {
    let label: String
    let amount: Double
    let symbol: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.system(size: 9, weight: .bold)).foregroundStyle(tint)
            Text(formatMoney(amount, symbol: symbol))
                .font(.system(size: 13, weight: .medium))
                .lineLimit(1).minimumScaleFactor(0.8)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
    }
}
