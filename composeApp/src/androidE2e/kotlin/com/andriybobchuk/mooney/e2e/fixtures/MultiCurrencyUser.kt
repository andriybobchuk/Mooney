package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Multi-currency user for net-worth math flows. Balances chosen so the
 * StubExchangeRateProvider math produces recognizable round numbers:
 *
 *   Base USD:
 *     Checking (USD $5,000)  → $5,000
 *     Euro Savings (€2,300)  → $2,500 (rate 0.92 → /0.92)
 *     Polish Zloty (4,000 zł) → $1,000 (rate 4.00 → /4.00)
 *   Total net worth: $8,500
 */
val MultiCurrencyUser: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    account(title = "Checking", currency = Currency.USD, amount = 5_000.0, isPrimary = true)
    account(title = "Euro Savings", currency = Currency.EUR, amount = 2_300.0)
    account(title = "Zloty Cash", currency = Currency.PLN, amount = 4_000.0)
    userCurrency(Currency.USD, sortOrder = 0)
    userCurrency(Currency.EUR, sortOrder = 1)
    userCurrency(Currency.PLN, sortOrder = 2)
}
