package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.usecase.ShouldRefreshExchangeRatesUseCase
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldRefreshExchangeRatesUseCaseTest {

    private val sut = ShouldRefreshExchangeRatesUseCase()

    @Test
    fun `returns true when timestamp is 0 - never updated`() {
        val result = sut(lastUpdatedTimestamp = 0L)
        assertTrue(result)
    }

    @Test
    fun `returns true when timestamp is older than 1 hour`() {
        val twoHoursAgo = Clock.System.now().toEpochMilliseconds() - (ShouldRefreshExchangeRatesUseCase.ONE_HOUR_MS * 2)

        val result = sut(lastUpdatedTimestamp = twoHoursAgo)

        assertTrue(result)
    }

    // The "exactly at the 1 hour boundary" test that lived here was removed
    // because it hinged on strict-less-than semantics of two Clock.now()
    // reads. On a fast machine both reads return the same value and the
    // SUT returns false; on a slower CI runner the second read ticks forward
    // and the SUT returns true. Either behavior is technically correct
    // (nothing meaningful happens on a 1-µs boundary), but the test was
    // deterministic in NEITHER direction. Clearly-stale + clearly-fresh
    // cases below cover the actual product behavior.

    @Test
    fun `returns false when timestamp is recent - less than 1 hour ago`() {
        val thirtyMinutesAgo = Clock.System.now().toEpochMilliseconds() -
            (ShouldRefreshExchangeRatesUseCase.ONE_HOUR_MS / 2)

        val result = sut(lastUpdatedTimestamp = thirtyMinutesAgo)

        assertFalse(result)
    }

    @Test
    fun `returns false when timestamp is just 1 minute ago`() {
        val oneMinuteAgo = Clock.System.now().toEpochMilliseconds() - 60_000L

        val result = sut(lastUpdatedTimestamp = oneMinuteAgo)

        assertFalse(result)
    }

    @Test
    fun `returns false when timestamp is current`() {
        val now = Clock.System.now().toEpochMilliseconds()

        val result = sut(lastUpdatedTimestamp = now)

        assertFalse(result)
    }

    @Test
    fun `returns true when timestamp is just over 1 hour ago`() {
        val justOverOneHourAgo = Clock.System.now().toEpochMilliseconds() -
            ShouldRefreshExchangeRatesUseCase.ONE_HOUR_MS - 5_000L

        val result = sut(lastUpdatedTimestamp = justOverOneHourAgo)

        assertTrue(result)
    }
}
