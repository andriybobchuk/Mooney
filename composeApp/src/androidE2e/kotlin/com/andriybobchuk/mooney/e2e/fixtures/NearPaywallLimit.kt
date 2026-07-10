package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * User at the free-tier account limit (5 accounts). The next "Add account"
 * tap should trigger the paywall. Fixture for flow 13.
 */
val NearPaywallLimit: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    account("Checking", Currency.USD, 5_000.0, isPrimary = true)
    account("Savings", Currency.USD, 12_000.0)
    account("Cash", Currency.USD, 300.0)
    account("Credit Card", Currency.USD, -450.0, isLiability = true, assetCategory = "CREDIT_CARD")
    account("Brokerage", Currency.USD, 25_000.0, assetCategory = "INVESTMENT")
}
