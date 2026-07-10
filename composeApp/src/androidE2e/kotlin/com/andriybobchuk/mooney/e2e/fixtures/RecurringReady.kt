package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * One account + one active monthly recurring on day 15 with
 * lastProcessedDate a full cycle back. Used by flow 07: on a cold-start
 * where wall-clock has crossed the next due date, the app should
 * auto-generate a PendingTransactionEntity that the user can accept.
 *
 * NB: this depends on the real system clock — no FixedClock injection
 * yet. If the CI runner clock happens to be pre-`lastProcessedDate + 1
 * month`, the flow will read as no-op instead of failing. Set the
 * `lastProcessedDate` far enough back to make that vanishingly unlikely.
 */
val RecurringReady: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = true
    val checkingId = account(
        title = "Checking",
        currency = Currency.USD,
        amount = 2_500.0,
        isPrimary = true,
    )

    recurring(
        title = "Rent",
        subcategoryId = "rent",
        amount = 1_500.0,
        accountId = checkingId,
        dayOfMonth = 15,
        frequency = "MONTHLY",
        createdDate = "2024-01-01",
        lastProcessedDate = "2024-06-15",
    )
}
