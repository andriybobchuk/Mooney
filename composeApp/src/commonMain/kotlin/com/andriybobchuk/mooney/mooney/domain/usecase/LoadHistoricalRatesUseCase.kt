package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.HistoricalRateDao
import com.andriybobchuk.mooney.core.data.database.HistoricalRateEntity
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.HistoricalRateProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class LoadHistoricalRatesUseCase(
    private val historicalRateDao: HistoricalRateDao,
    private val historicalRateProvider: HistoricalRateProvider
) {
    suspend operator fun invoke(
        baseCurrency: Currency,
        targetCurrencies: List<Currency>,
        months: Int = 6
    ): Map<Currency, List<HistoricalRate>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startDate = today.minus(months, DateTimeUnit.MONTH)

        // Check cache freshness
        val latestCached = historicalRateDao.getLatestDate(baseCurrency.name, targetCurrencies.firstOrNull()?.name ?: "")
        val needsFetch = latestCached == null || latestCached < today.toString()

        if (needsFetch) {
            val fetchStart = if (latestCached != null) {
                // Fetch only the gap
                LocalDate.parse(latestCached)
            } else {
                startDate
            }

            when (val result = historicalRateProvider.getRates(baseCurrency, targetCurrencies, fetchStart, today)) {
                is Result.Success -> {
                    val entities = result.data.flatMap { (currency, rates) ->
                        rates.map { rate ->
                            HistoricalRateEntity(
                                fromCurrency = baseCurrency.name,
                                toCurrency = currency.name,
                                date = rate.date.toString(),
                                rate = rate.rate
                            )
                        }
                    }
                    if (entities.isNotEmpty()) {
                        historicalRateDao.upsertAll(entities)
                    }
                }
                is Result.Error -> { /* Use cached data */ }
            }
        }

        // Clean up old data (older than 12 months)
        val cutoff = today.minus(12, DateTimeUnit.MONTH)
        historicalRateDao.deleteOlderThan(cutoff.toString())

        // Read from cache
        // Frankfurter "from=baseCurrency, to=target" returns "how many target per 1 baseCurrency"
        // e.g. from=USD, to=PLN → PLN: 3.58 = "3.58 PLN per 1 USD"
        return targetCurrencies.associateWith { currency ->
            historicalRateDao.getRates(baseCurrency.name, currency.name, startDate.toString())
                .map { entity -> HistoricalRate(LocalDate.parse(entity.date), entity.rate) }
        }
    }
}
