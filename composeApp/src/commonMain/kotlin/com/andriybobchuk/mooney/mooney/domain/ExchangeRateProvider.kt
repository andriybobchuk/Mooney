package com.andriybobchuk.mooney.mooney.domain

import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result

interface ExchangeRateProvider {
    suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote>
}