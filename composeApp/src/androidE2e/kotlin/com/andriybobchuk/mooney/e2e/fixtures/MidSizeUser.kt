package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * User with 3 accounts and 15 pre-existing transactions covering the
 * current month. Realistic shape for flows that need transactions
 * on-screen to interact with (edit, delete, list navigation).
 *
 * Category IDs need to match those seeded by CategoryImporter — using a
 * few well-known ones. Dates: last 15 days from a July 2026 baseline.
 */
val MidSizeUser: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    val checkingId = account(
        title = "Checking",
        currency = Currency.USD,
        amount = 3_500.0,
        isPrimary = true,
    )
    account(title = "Savings", currency = Currency.USD, amount = 15_000.0)
    account(title = "Cash", currency = Currency.USD, amount = 250.0)

    // Recent week — realistic spending pattern.
    txn(checkingId, "groceries", 45.20, "2026-07-02")
    txn(checkingId, "coffee", 4.75, "2026-07-03")
    txn(checkingId, "gas", 62.10, "2026-07-05")
    txn(checkingId, "coffee", 12.50, "2026-07-06")
    txn(checkingId, "groceries", 89.30, "2026-07-07")
    txn(checkingId, "salary", 3_500.0, "2026-07-09")
    txn(checkingId, "coffee", 5.25, "2026-07-10")
    txn(checkingId, "streaming", 34.99, "2026-07-11")
    txn(checkingId, "groceries", 78.40, "2026-07-13")
    txn(checkingId, "coffee", 6.00, "2026-07-14")
}
