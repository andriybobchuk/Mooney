import AppIntents
import ComposeApp
import Foundation

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
/// Users can invoke this via:
///   * Siri: "Add expense in Mooney"
///   * Shortcuts app: pre-built "Add Transaction" tile
///   * Custom Shortcuts: chained after Apple Wallet notifications, NFC tag
///     scans, focus filters, or "when I arrive at Merchant X" automations
///
/// `openAppWhenRun = false` — the intent runs silently in the background so
/// automation doesn't yank the user out of whatever they're doing. The result
/// dialog shows the confirmation ("Added 12.50 zł — Coffee") right in the
/// Shortcuts / Siri overlay.
@available(iOS 16.0, *)
struct AddMooneyTransactionIntent: AppIntent {
    static var title: LocalizedStringResource = "Add Transaction"

    static var description = IntentDescription(
        "Log a new expense or income in Mooney without opening the app.",
        categoryName: "Money",
        searchKeywords: ["transaction", "expense", "income", "spending", "budget"]
    )

    static var openAppWhenRun: Bool = false

    @Parameter(title: "Amount")
    var amount: Double

    @Parameter(title: "Type", default: .expense)
    var type: MooneyTransactionType

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

    @Parameter(title: "Note")
    var note: String?

    @Parameter(title: "Date")
    var date: Date?

    static var parameterSummary: some ParameterSummary {
        Summary("Add \(\.$type) of \(\.$amount) to Mooney") {
            \.$categoryId
            \.$accountTitle
            \.$note
            \.$date
        }
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        // Resolve the Kotlin handler through the top-level bootstrap function.
        // Guaranteed idempotent w.r.t. Koin — safe to call whether the app is
        // in the foreground, background, or being launched cold by the intent.
        let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()

        // Convert the optional Swift Date to the yyyy-MM-dd string the Kotlin
        // handler expects. Passing nil lets the handler default to today.
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
            description: note,
            isoDate: isoDate
        )

        if result.isSuccess {
            return .result(dialog: IntentDialog(stringLiteral: result.message))
        } else {
            throw MooneyIntentError.failed(message: result.message)
        }
    }
}
