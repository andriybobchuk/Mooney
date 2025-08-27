package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

class StaticExchangeRateProvider : ExchangeRateProvider {
    override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
        return Result.Success(GlobalConfig.testExchangeRates)
    }
}