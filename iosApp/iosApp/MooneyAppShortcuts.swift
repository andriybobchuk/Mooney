import AppIntents

/// Discoverable Siri phrases + Shortcuts app entries for Mooney intents.
///
/// These are the phrases Siri will match when the user says something without
/// tapping — the `(.applicationName)` token expands to "Mooney" so the whole
/// phrase reads naturally in the Shortcuts UI too. Once installed, the
/// Shortcuts app will show a "Mooney" section with these tiles pre-populated.
@available(iOS 16.0, *)
struct MooneyAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: AddMooneyTransactionIntent(),
            phrases: [
                "Add expense in \(.applicationName)",
                "Log an expense in \(.applicationName)",
                "Add income in \(.applicationName)",
                "New transaction in \(.applicationName)",
                "Track spending in \(.applicationName)"
            ],
            shortTitle: "Add Transaction",
            systemImageName: "plus.circle.fill"
        )
        AppShortcut(
            intent: GetMooneySpentThisMonthIntent(),
            phrases: [
                "How much did I spend in \(.applicationName) this month",
                "What did I spend this month in \(.applicationName)",
                "\(.applicationName) monthly spending"
            ],
            shortTitle: "Spent This Month",
            systemImageName: "chart.line.downtrend.xyaxis"
        )
        AppShortcut(
            intent: GetMooneyIncomeThisMonthIntent(),
            phrases: [
                "How much did I earn in \(.applicationName) this month",
                "\(.applicationName) monthly income",
                "What's my income this month in \(.applicationName)"
            ],
            shortTitle: "Earned This Month",
            systemImageName: "chart.line.uptrend.xyaxis"
        )
        AppShortcut(
            intent: GetMooneyNetWorthIntent(),
            phrases: [
                "What's my net worth in \(.applicationName)",
                "\(.applicationName) net worth",
                "Check my net worth in \(.applicationName)"
            ],
            shortTitle: "Net Worth",
            systemImageName: "banknote"
        )
        AppShortcut(
            intent: GetMooneyAccountBalanceIntent(),
            phrases: [
                "What's my balance in \(.applicationName)",
                "\(.applicationName) balance",
                "Check my \(.applicationName) account"
            ],
            shortTitle: "Account Balance",
            systemImageName: "creditcard"
        )
    }
}
