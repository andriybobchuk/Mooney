package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Two USD accounts — Checking $3,000 and Savings $10,000. Used by the
 * same-currency transfer flow (05) where we assert both balances change
 * atomically and net worth stays flat.
 */
val TwoAccountsUsd: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    account(title = "Checking", currency = Currency.USD, amount = 3_000.0, isPrimary = true)
    account(title = "Savings", currency = Currency.USD, amount = 10_000.0)
}
