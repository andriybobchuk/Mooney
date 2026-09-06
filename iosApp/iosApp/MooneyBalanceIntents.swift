import AppIntents
import ComposeApp
import Foundation

// MARK: - Get Spent This Month

@available(iOS 16.0, *)
struct GetMooneySpentThisMonthIntent: AppIntent {
    static var title: LocalizedStringResource = "Spent This Month"
    static var description = IntentDescription(
        "Return the total amount you've spent this calendar month, in your base currency.",
        categoryName: "Money"
    )
    static var openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult & ReturnsValue<Double> & ProvidesDialog {
        let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()
        let result = try await handler.getSpentThisMonth()
        if result.isSuccess {
            return .result(
                value: result.amount,
                dialog: IntentDialog(stringLiteral: "You've spent \(result.formatted) this month.")
            )
        } else {
            throw MooneyIntentError.failed(message: result.errorMessage)
        }
    }
}

// MARK: - Get Income This Month

@available(iOS 16.0, *)
struct GetMooneyIncomeThisMonthIntent: AppIntent {
    static var title: LocalizedStringResource = "Earned This Month"
    static var description = IntentDescription(
        "Return the total income you've received this calendar month.",
        categoryName: "Money"
    )
    static var openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult & ReturnsValue<Double> & ProvidesDialog {
        let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()
        let result = try await handler.getIncomeThisMonth()
        if result.isSuccess {
            return .result(
                value: result.amount,
                dialog: IntentDialog(stringLiteral: "You've earned \(result.formatted) this month.")
            )
        } else {
            throw MooneyIntentError.failed(message: result.errorMessage)
        }
    }
}

// MARK: - Get Net Worth

@available(iOS 16.0, *)
struct GetMooneyNetWorthIntent: AppIntent {
    static var title: LocalizedStringResource = "Net Worth"
    static var description = IntentDescription(
        "Return your current total net worth across all Mooney accounts.",
        categoryName: "Money"
    )
    static var openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult & ReturnsValue<Double> & ProvidesDialog {
        let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()
        let result = try await handler.getNetWorth()
        if result.isSuccess {
            return .result(
                value: result.amount,
                dialog: IntentDialog(stringLiteral: "Your net worth is \(result.formatted).")
            )
        } else {
            throw MooneyIntentError.failed(message: result.errorMessage)
        }
    }
}

// MARK: - Get Account Balance

@available(iOS 16.0, *)
struct GetMooneyAccountBalanceIntent: AppIntent {
    static var title: LocalizedStringResource = "Account Balance"
    static var description = IntentDescription(
        "Return the current balance of a specific Mooney account. Leaves the account blank to get your primary account.",
        categoryName: "Money"
    )
    static var openAppWhenRun: Bool = false

    @Parameter(
        title: "Account",
        description: "Account name (e.g. Bank, Cash). Leave empty for your primary account."
    )
    var accountTitle: String?

    static var parameterSummary: some ParameterSummary {
        Summary("Get balance of \(\.$accountTitle)")
    }

    func perform() async throws -> some IntentResult & ReturnsValue<Double> & ProvidesDialog {
        let handler = TransactionIntentHandlerKt.resolveTransactionIntentHandler()
        let result = try await handler.getAccountBalance(accountTitle: accountTitle)
        if result.isSuccess {
            return .result(
                value: result.amount,
                dialog: IntentDialog(stringLiteral: result.formatted)
            )
        } else {
            throw MooneyIntentError.failed(message: result.errorMessage)
        }
    }
}
