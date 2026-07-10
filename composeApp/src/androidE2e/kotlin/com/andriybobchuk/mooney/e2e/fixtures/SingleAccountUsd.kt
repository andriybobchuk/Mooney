package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Post-onboarding user with one USD checking account at $5,000.
 * The canonical fixture for the "add transaction, verify balance" smoke flow.
 */
val SingleAccountUsd: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    account(
        title = "Checking",
        currency = Currency.USD,
        amount = 5_000.0,
        isPrimary = true,
    )
}
