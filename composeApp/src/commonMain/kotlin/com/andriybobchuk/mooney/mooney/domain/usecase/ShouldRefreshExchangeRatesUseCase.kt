package com.andriybobchuk.mooney.mooney.domain.usecase

import kotlinx.datetime.Clock

class ShouldRefreshExchangeRatesUseCase {

    operator fun invoke(lastUpdatedTimestamp: Long): Boolean {
        val oneHourAgo = Clock.System.now().toEpochMilliseconds() - ONE_HOUR_MS
        return lastUpdatedTimestamp == 0L || lastUpdatedTimestamp < oneHourAgo
    }

    companion object {
        const val ONE_HOUR_MS = 3_600_000L
    }
}
