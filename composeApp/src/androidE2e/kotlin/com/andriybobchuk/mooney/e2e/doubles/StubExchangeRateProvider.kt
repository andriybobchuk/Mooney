package com.andriybobchuk.mooney.e2e.doubles

import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

/**
 * Deterministic exchange rates for E2E flows. Baseline of 1.0 for the
 * requested base currency; other rates chosen for legible round math in
 * flows (a $1,000 USD balance shows as €920 or 4,000 zł).
 *
 * Never touches the network. If a flow references a currency not in the
 * table, it falls back to 1.0 — that's a bug in the flow, not the stub.
 */
class StubExchangeRateProvider : ExchangeRateProvider {
    override suspend fun getExchangeRates(
        baseCurrency: Currency,
    ): Result<ExchangeRates, DataError.Remote> {
        val ratesFromUsd = mapOf(
            Currency.USD to 1.00,
            Currency.EUR to 0.92,
            Currency.PLN to 4.00,
            Currency.GBP to 0.79,
            Currency.CHF to 0.90,
            Currency.UAH to 41.00,
        )
        val baseInUsd = ratesFromUsd[baseCurrency] ?: 1.0
        val rebased = ratesFromUsd.mapValues { (_, usdRate) -> usdRate / baseInUsd }
        return Result.Success(ExchangeRates(rebased))
    }
}
