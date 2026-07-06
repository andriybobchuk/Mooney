package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * User with 3 accounts and 10 realistic transactions spread across the
 * last two weeks (relative to wall-clock). Uses the DSL's `daysAgo`
 * helper so flows using this fixture stay stable regardless of when CI
 * happens to run.
 *
 * Transaction insert order (governs row IDs referenced from flows):
 *   1: groceries -14 days
 *   2: coffee -13 days
 *   3: gas -11 days
 *   4: coffee -10 days
 *   5: groceries -9 days
 *   6: salary -7 days      ← id 6 in flows that need income
 *   7: coffee -6 days
 *   8: streaming -5 days   ← id 8 in flow 03 (edit)
 *   9: gas -4 days           ← id 9 in flow 04 (delete)
 *   10: groceries -3 days
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

    txn(checkingId, "groceries", 45.20, daysAgo(14))
    txn(checkingId, "coffee", 4.75, daysAgo(13))
    txn(checkingId, "gas", 62.10, daysAgo(11))
    txn(checkingId, "coffee", 12.50, daysAgo(10))
    txn(checkingId, "groceries", 89.30, daysAgo(9))
    txn(checkingId, "salary", 3_500.0, daysAgo(7))
    txn(checkingId, "coffee", 5.25, daysAgo(6))
    txn(checkingId, "streaming", 34.99, daysAgo(5))
    // A round-number expense — flow 04 deletes this row and asserts the
    // Checking balance rolls forward by exactly $50 (the "gas" row's amount).
    txn(checkingId, "gas", 50.0, daysAgo(4))
    txn(checkingId, "groceries", 78.40, daysAgo(3))
}
