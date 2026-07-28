import WidgetKit
import SwiftUI

/// Streak + Mooley — the retention-optimized widget. SwiftUI counterpart to
/// `StreakWidget.kt`. systemSmall (Mooley + count centered) and systemMedium
/// (Mooley on left, headline + subline on right).
struct StreakWidget: Widget {
    let kind: String = "MooneyStreakWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider()) { entry in
            StreakWidgetView(snapshot: entry.snapshot)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Streak · Mooley")
        .description("Say hi to Mooley. Log a spend every day to keep the fire alive.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct StreakWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let snapshot: WidgetSnapshot

    var body: some View {
        switch family {
        case .systemSmall: SmallStreak(snapshot: snapshot)
        default: MediumStreak(snapshot: snapshot)
        }
    }
}

private struct SmallStreak: View {
    let snapshot: WidgetSnapshot
    var body: some View {
        VStack(spacing: 4) {
            MooleyView(mood: snapshot.mood).frame(width: 56, height: 56)
            Text(snapshot.streakDays > 0
                 ? "🔥 \(snapshot.streakDays) day\(snapshot.streakDays == 1 ? "" : "s")"
                 : "🌱 Start today")
                .font(.system(size: 14, weight: .bold))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(12)
        .widgetURL(WidgetDeepLink.url(
            kind: "streak",
            action: snapshot.streakDays == 0 ? "add_transaction" : "home"
        ))
    }
}

private struct MediumStreak: View {
    let snapshot: WidgetSnapshot
    var body: some View {
        HStack(spacing: 12) {
            MooleyView(mood: snapshot.mood).frame(width: 68, height: 68)
            VStack(alignment: .leading, spacing: 2) {
                Text(headline)
                    .font(.system(size: 16, weight: .bold))
                    .lineLimit(1)
                Text(subline)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .widgetURL(WidgetDeepLink.url(
            kind: "streak",
            action: snapshot.streakDays == 0 ? "add_transaction" : "home"
        ))
    }

    private var headline: String {
        switch snapshot.streakDays {
        case 0: return "Start a new streak"
        case 1: return "🔥 1-day streak"
        default: return "🔥 \(snapshot.streakDays)-day streak"
        }
    }

    private var subline: String {
        switch snapshot.mood {
        case .happy: return "Under budget on every category — nice."
        case .neutral: return "Keep going, Mooley believes in you."
        case .worried: return "Getting close to a budget — check inside."
        case .overBudget: return "One category is over budget. Time to review."
        }
    }
}
