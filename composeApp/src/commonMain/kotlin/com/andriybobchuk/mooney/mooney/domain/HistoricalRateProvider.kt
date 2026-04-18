package com.andriybobchuk.mooney.mooney.domain

import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.datetime.LocalDate

interface HistoricalRateProvider {
    suspend fun getRates(
        from: Currency,
        to: List<Currency>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<Currency, List<HistoricalRate>>, DataError.Remote>
}
