import Foundation

/// Compact money formatting used across every widget view. Matches the
/// Kotlin `formatMoney` in `WidgetSupport.kt` — thresholds, decimals, and
/// suffixes are intentionally identical so the two platforms display the
/// same string for the same amount.
func formatMoney(_ amount: Double, symbol: String) -> String {
    let abs = Swift.abs(amount)
    let display: String
    switch abs {
    case 1_000_000...:
        display = String(format: "%.1fM", amount / 1_000_000)
    case 10_000...:
        display = String(format: "%.1fk", amount / 1_000)
    case ..<100:
        display = String(format: "%.2f", amount)
    default:
        display = String(format: "%.0f", amount)
    }
    return "\(symbol)\(display)"
}
