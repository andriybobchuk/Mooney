package com.andriybobchuk.mooney.e2e.fixtures

import com.andriybobchuk.mooney.e2e.Fixture
import com.andriybobchuk.mooney.e2e.fixture
import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Fresh install, no accounts, onboarding NOT skipped — for tests that walk
 * the onboarding flow.
 */
val Empty: Fixture = fixture {
    baseCurrency = Currency.USD
    skipOnboarding = false
}
