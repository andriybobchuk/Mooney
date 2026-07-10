package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Post-spending USD user: Checking sits at $4,900 with one pre-existing
 * $100 "coffee" expense from yesterday. The account balance already
 * reflects that spend — this fixture is the DAO-level equivalent of
 * what add-transaction flow 02 should produce.
 *
 * Purpose: exercise the balance render + currency formatting + txn-row
 * rendering path *without* going through the Compose `BasicTextField`
 * text-input path that flow 02 keeps racing on the CI emulator.
 */
val PostSpendUsd: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    val checkingId = account(
        title = "Checking",
        currency = Currency.USD,
        amount = 4_900.0,
        isPrimary = true,
    )
    txn(checkingId, "coffee", 100.0, daysAgo(1))
}
