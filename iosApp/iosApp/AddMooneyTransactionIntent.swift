import AppIntents
import ComposeApp
import Foundation
import UserNotifications

// MARK: - Intent parameters

@available(iOS 16.0, *)
enum MooneyTransactionType: String, AppEnum {
    case expense = "EXPENSE"
    case income = "INCOME"

    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Transaction type")
    static var caseDisplayRepresentations: [MooneyTransactionType: DisplayRepresentation] = [
        .expense: DisplayRepresentation(title: "Expense"),
        .income: DisplayRepresentation(title: "Income")
    ]
}

// MARK: - Errors

@available(iOS 16.0, *)
enum MooneyIntentError: Swift.Error, CustomLocalizedStringResourceConvertible {
    case failed(message: String)

    var localizedStringResource: LocalizedStringResource {
        switch self {
        case .failed(let message):
            return LocalizedStringResource(stringLiteral: message)
        }
    }
}

// MARK: - Add Transaction intent

/// AppIntent that logs a new Mooney transaction without opening the app.
///
/// The killer use case: chain from the built-in **Wallet card payment**
/// automation trigger. Wallet exposes the payment Amount, Merchant, and Card
/// as magic variables — wire Amount → the intent's Amount, and Merchant →
/// the intent's Description, and every card payment auto-logs in Mooney with
/// a meaningful label ("Automatically added 64.14 zł for Biedronka").
///
/// `openAppWhenRun = false` so automation doesn't yank the user out of what
/// they're doing. After success we fire a local notification confirming the
/// entry — otherwise silent auto-logs feel invisible.
@available(iOS 16.0, *)
struct AddMooneyTransactionIntent: AppIntent {
    static var title: LocalizedStringResource = "Add Transaction"

    // Single-string literal (no + concatenation) so the compiler can bridge
    // it to LocalizedStringResource. Concatenated Strings don't auto-bridge.
    static var description = IntentDescription(
        "Log a new expense or income in Mooney without opening the app. Best paired with the Wallet card-payment automation trigger.",
        categoryName: "Money",
        searchKeywords: ["transaction", "expense", "income", "spending", "budget", "wallet"]
    )

    static var openAppWhenRun: Bool = false

    @Parameter(title: "Amount")
    var amount: Double

    @Parameter(title: "Type", default: .expense)
    var type: MooneyTransactionType

    @Parameter(
        title: "Description",
        description: "Merchant or note — wire Wallet's Merchant magic variable here for auto-labelled logs."
    )
    var descriptionText: String?

    @Parameter(
        title: "Category",
        description: "Category ID (e.g. groceries, salary). Defaults to your Mooney default."
    )
    var categoryId: String?

    @Parameter(
        title: "Account",
        description: "Account name (e.g. Bank, Cash). Defaults to your primary account."
    )
    var accountTitle: String?

    @Parameter(title: "Date")
    var date: Date?

    // Description is hoisted into the visible summary so users pairing this
    // with Wallet automations can wire the Merchant variable without having
    // to expand the "additional parameters" section.
    static var parameterSummary: some ParameterSummary {
        Summary("Add \(\.$type) of \(\.$amount) for \(\.$descriptionText) to Mooney") {
            \.$categoryId
            \.$accountTitle
            \.$date
        }
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        // Zero-amount path. The iOS Wallet "card tap" trigger fires at NFC
        // handshake time (t≈0), BEFORE the terminal has sent back the final
        // amount. For PIN'd / network-authorized contactless txs the amount
        // may not populate for 1-5 seconds — the trigger wins the race and
        // Mooney gets Amount=0 for what was in fact a real purchase. That
        // was the "30-40% intermittent failure" users were seeing.
        //
        // Two sub-cases, distinguished by whether Merchant was wired:
        //   (a) Merchant present → real purchase racing settlement. Fire a
        //       push notification "Amount needed for [Merchant]" so the user
        //       can log it manually. ZERO data loss — the user still knows
        //       a tx happened. Return success so Shortcuts doesn't ALSO
        //       stack its red failure banner on top.
        //   (b) Merchant empty → trigger fired on a non-payment event
        //       (loyalty card tap, boarding pass, pre-auth with no merchant
        //       context). Silent skip is correct here.
        //
        // Best permanent cure is on the Shortcut side: insert a "Wait 3s"
        // action before Add Transaction. We tell users this in the
        // Automation onboarding screen. This Mooney-side logic is the
        // safety net for users who skip that step.
        guard amount > 0 else {
            let merchantHint = descriptionText?.trimmingCharacters(in: .whitespaces) ?? ""
            if !merchantHint.isEmpty {
                postAmountNeededNotification(merchant: merchantHint)
                return .result(dialog: "Payment at \(merchantHint) — Apple Wallet didn't report the amount in time. Tap the Mooney notification to add it manually.")
            }
            return .result(dialog: "No amount or merchant in this Wallet event — nothing to log.")
        }

        let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()

        let isoDate: String? = date.map { pickedDate in
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withFullDate]
            return formatter.string(from: pickedDate)
        }

        let result = try await handler.addTransaction(
            amount: amount,
            typeRaw: type.rawValue,
            categoryId: categoryId,
            accountTitle: accountTitle,
            description: descriptionText,
            isoDate: isoDate
        )

        if result.isSuccess {
            postAutoLogNotification(amountFormatted: result.amountFormatted, label: result.label)
            return .result(dialog: IntentDialog(stringLiteral: result.message))
        } else {
            throw MooneyIntentError.failed(message: result.message)
        }
    }

    /// Local push for the race-condition case: Wallet fired the trigger
    /// with Amount=0 but a real Merchant is present (shop payment that
    /// settled too slow for the trigger). Non-silent sound because this
    /// is action-required, not a passive confirmation.
    private func postAmountNeededNotification(merchant: String) {
        let content = UNMutableNotificationContent()
        content.title = "Mooney — amount needed"
        content.body = "Apple Wallet didn't send the amount for your \(merchant) payment in time. Open Mooney to add it."
        content.sound = .default
        content.userInfo = ["pendingMerchant": merchant]
        let request = UNNotificationRequest(
            identifier: "mooney.amountneeded.\(UUID().uuidString)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request) { _ in }
    }

    /// Local push confirming the auto-log — critical for background-run
    /// intents (Wallet automation) because otherwise the user has no signal
    /// that Mooney actually recorded the payment. Silently no-ops if the
    /// user hasn't granted notification permission; the intent still succeeds.
    private func postAutoLogNotification(amountFormatted: String, label: String) {
        let content = UNMutableNotificationContent()
        content.title = "Mooney"
        content.body = "Automatically added \(amountFormatted) for \(label)"
        content.sound = nil // Silent — Wallet already made the "cha-ching" sound
        let request = UNNotificationRequest(
            identifier: "mooney.autolog.\(UUID().uuidString)",
            content: content,
            trigger: nil // Fire immediately
        )
        UNUserNotificationCenter.current().add(request) { _ in
            // No-op — best-effort. If permission denied, notification is
            // dropped silently and the intent still returns success.
        }
    }
}
