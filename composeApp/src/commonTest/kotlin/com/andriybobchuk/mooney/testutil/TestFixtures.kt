package com.andriybobchuk.mooney.testutil

import com.andriybobchuk.mooney.mooney.domain.*
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.datetime.LocalDate

object TestFixtures {

    val testRates = ExchangeRates(
        rates = mapOf(
            Currency.PLN to 1.0,
            Currency.USD to 0.25,
            Currency.EUR to 0.22,
            Currency.UAH to 10.0
        )
    )

    val fakeExchangeRateProvider = object : ExchangeRateProvider {
        override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
            return Result.Success(testRates)
        }
    }

    fun currencyManager(): CurrencyManagerUseCase = CurrencyManagerUseCase(fakeExchangeRateProvider)

    // Category hierarchy
    val expense = Category("expense", "Expense", CategoryType.EXPENSE, emoji = "☺️")
    val income = Category("income", "Income", CategoryType.INCOME, emoji = "🤲")
    val transfer = Category("transfer", "Transfer", CategoryType.TRANSFER, emoji = "↔️")

    val groceries = Category("groceries", "Groceries & Household", CategoryType.EXPENSE, emoji = "🛒", parent = expense)
    val housing = Category("housing", "Housing", CategoryType.EXPENSE, emoji = "🏠", parent = expense)
    val rent = Category("rent", "Rent", CategoryType.EXPENSE, parent = housing)
    val utilities = Category("utilities", "Utilities", CategoryType.EXPENSE, parent = housing)

    val tax = Category("tax", "Tax", CategoryType.EXPENSE, emoji = "🏦", parent = expense)
    val zus = Category("zus", "ZUS", CategoryType.EXPENSE, parent = tax)
    val pit = Category("pit", "PIT", CategoryType.EXPENSE, parent = tax)

    val salary = Category("salary", "Salary", CategoryType.INCOME, emoji = "💸", parent = income)
    val taxReturn = Category("tax_return", "Tax Return", CategoryType.INCOME, emoji = "💸", parent = income)
    val refund = Category("refund", "Refund", CategoryType.INCOME, emoji = "💸", parent = income)

    val internalTransfer = Category("internal_transfer", "Internal Transfer", CategoryType.TRANSFER, emoji = "🔄", parent = transfer)

    val reconciliation = Category("reconciliation", "Account Reconciliation", CategoryType.EXPENSE, emoji = "💱", parent = expense)
    val positiveReconciliation = Category("positive_reconciliation", "Account Reconciliation", CategoryType.INCOME, emoji = "💸", parent = income)

    val allCategories = listOf(
        expense, income, transfer, groceries, housing, rent, utilities,
        tax, zus, pit, salary, taxReturn, refund, internalTransfer,
        reconciliation, positiveReconciliation
    )

    fun account(
        id: Int = 1,
        title: String = "Test Account",
        amount: Double = 1000.0,
        currency: Currency = Currency.PLN,
        emoji: String = "💰",
        assetCategory: AssetCategory = AssetCategory.BANK_ACCOUNT,
        isPrimary: Boolean = false,
        isLiability: Boolean = false
    ) = Account(id, title, amount, currency, emoji, assetCategory, assetCategory.name, isPrimary, isLiability)

    fun transaction(
        id: Int = 0,
        subcategory: Category = groceries,
        amount: Double = 100.0,
        account: Account = account(),
        date: LocalDate = LocalDate(2024, 3, 15)
    ) = Transaction(id, subcategory, amount, account, date)

    fun goal(
        id: Int = 1,
        emoji: String = "🎯",
        title: String = "Test Goal",
        description: String = "Test description",
        targetAmount: Double = 10000.0,
        currency: Currency = Currency.PLN
    ) = Goal(id, emoji, title, description, targetAmount, currency)
}
