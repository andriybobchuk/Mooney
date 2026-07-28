import WidgetKit
import SwiftUI

/// One-tap deep-link into the Add Transaction sheet. Home-screen equivalent
/// of the FAB. systemSmall only — a bigger surface wastes user grid space
/// for what is a single tap target.
struct QuickAddWidget: Widget {
    let kind: String = "MooneyQuickAddWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider()) { _ in
            QuickAddWidgetView()
                .containerBackground(.blue, for: .widget)
        }
        .configurationDisplayName("Quick Add")
        .description("One tap to log a spend, straight from your home screen.")
        .supportedFamilies([.systemSmall])
    }
}

struct QuickAddWidgetView: View {
    var body: some View {
        VStack(alignment: .center, spacing: 6) {
            Text("＋").font(.system(size: 44, weight: .bold)).foregroundStyle(.white)
            Text("Log a spend").font(.system(size: 14, weight: .bold)).foregroundStyle(.white)
            Text("Mooney → tap").font(.system(size: 11)).foregroundStyle(.white.opacity(0.8))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(12)
        .widgetURL(WidgetDeepLink.url(kind: "quick_add", action: "add_transaction"))
    }
}
