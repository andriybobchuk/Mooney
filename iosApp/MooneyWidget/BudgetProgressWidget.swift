import WidgetKit
import SwiftUI

/// Budget progress — SwiftUI counterpart to `BudgetProgressWidget.kt`. Medium
/// (top-1) and Large (top-3) sizes.
struct BudgetProgressWidget: Widget {
    let kind: String = "MooneyBudgetWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider()) { entry in
            BudgetWidgetView(snapshot: entry.snapshot)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Budgets")
        .description("Your top budgets and how close you are to hitting them.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

struct BudgetWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let snapshot: WidgetSnapshot

    private var maxLines: Int { family == .systemLarge ? 3 : 1 }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("BUDGET · THIS MONTH")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(.secondary)
            if snapshot.budgetLines.isEmpty {
                emptyState
            } else {
                ForEach(Array(snapshot.budgetLines.prefix(maxLines).enumerated()), id: \.offset) { _, line in
                    BudgetRow(line: line, symbol: snapshot.baseCurrencySymbol)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .widgetURL(WidgetDeepLink.url(kind: "budget", action: "analytics"))
    }

    private var emptyState: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("No budgets set yet")
                .font(.system(size: 16, weight: .bold))
            Text("Tap to set a category limit — Mooley will keep an eye on it.")
                .font(.system(size: 11))
                .foregroundStyle(.secondary)
        }
    }
}

private struct BudgetRow: View {
    let line: WidgetBudgetLine
    let symbol: String

    private var barColor: Color {
        if line.isOverBudget { return .red }
        if line.progress >= 0.80 { return .orange }
        return .accentColor
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Text(line.emoji.isEmpty ? "🎯" : line.emoji)
                    .font(.system(size: 13))
                Text(line.title)
                    .font(.system(size: 12, weight: .medium))
                Spacer()
                Text("\(formatMoney(line.spentBase, symbol: symbol)) / \(formatMoney(line.limitBase, symbol: symbol))")
                    .font(.system(size: 11))
                    .foregroundStyle(.secondary)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color.gray.opacity(0.25))
                    RoundedRectangle(cornerRadius: 3)
                        .fill(barColor)
                        .frame(width: geo.size.width * CGFloat(min(1.0, line.progress)))
                }
            }
            .frame(height: 6)
        }
    }
}
