package com.andriybobchuk.mooney.e2e

import com.andriybobchuk.mooney.core.data.database.AccountEntity
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.core.data.database.TransactionEntity
import com.andriybobchuk.mooney.core.data.database.UserCurrencyEntity
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Kotlin DSL for E2E fixtures.
 *
 * Compiled against real entity classes so a required-column addition to
 * any Room entity fails the `e2e` build at compile time — no JSON, no
 * runtime schema drift.
 */
class FixtureBuilder internal constructor() {
    private var nextAccountId = 1
    val accounts: MutableList<AccountEntity> = mutableListOf()
    val transactions: MutableList<TransactionEntity> = mutableListOf()
    val recurring: MutableList<RecurringTransactionEntity> = mutableListOf()
    val userCurrencies: MutableList<UserCurrencyEntity> = mutableListOf()

    var baseCurrency: Currency = Currency.USD
    var skipOnboarding: Boolean = true

    fun account(
        title: String,
        currency: Currency,
        amount: Double,
        emoji: String = "💰",
        assetCategory: String = "BANK_ACCOUNT",
        isPrimary: Boolean = false,
        isLiability: Boolean = false,
    ): Int {
        val id = nextAccountId++
        accounts += AccountEntity(
            id = id,
            title = title,
            amount = amount,
            currency = currency.name,
            emoji = emoji,
            assetCategory = assetCategory,
            isPrimary = isPrimary,
            isLiability = isLiability,
        )
        return id
    }

    fun txn(
        accountId: Int,
        subcategoryId: String,
        amount: Double,
        date: String,
        description: String? = null,
        destinationAmount: Double? = null,
    ) {
        transactions += TransactionEntity(
            id = 0,
            subcategoryId = subcategoryId,
            amount = amount,
            accountId = accountId,
            date = date,
            destinationAmount = destinationAmount,
            description = description,
        )
    }

    fun recurring(
        title: String,
        subcategoryId: String,
        amount: Double,
        accountId: Int,
        dayOfMonth: Int,
        frequency: String = "MONTHLY",
        createdDate: String,
        lastProcessedDate: String? = null,
        isActive: Boolean = true,
    ) {
        recurring += RecurringTransactionEntity(
            id = 0,
            title = title,
            subcategoryId = subcategoryId,
            amount = amount,
            accountId = accountId,
            dayOfMonth = dayOfMonth,
            frequency = frequency,
            isActive = isActive,
            createdDate = createdDate,
            lastProcessedDate = lastProcessedDate,
        )
    }

    fun userCurrency(currency: Currency, sortOrder: Int) {
        userCurrencies += UserCurrencyEntity(code = currency.name, sortOrder = sortOrder)
    }
}

class Fixture internal constructor(
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val recurring: List<RecurringTransactionEntity>,
    val userCurrencies: List<UserCurrencyEntity>,
    val baseCurrency: Currency,
    val skipOnboarding: Boolean,
)

fun fixture(block: FixtureBuilder.() -> Unit): Fixture {
    val builder = FixtureBuilder()
    builder.block()
    // Ensure the base currency is in the user-currencies list. Mirrors what
    // CompleteOnboardingUseCase does for real users.
    if (builder.userCurrencies.none { it.code == builder.baseCurrency.name }) {
        builder.userCurrency(builder.baseCurrency, sortOrder = 0)
    }
    return Fixture(
        accounts = builder.accounts.toList(),
        transactions = builder.transactions.toList(),
        recurring = builder.recurring.toList(),
        userCurrencies = builder.userCurrencies.toList(),
        baseCurrency = builder.baseCurrency,
        skipOnboarding = builder.skipOnboarding,
    )
}
