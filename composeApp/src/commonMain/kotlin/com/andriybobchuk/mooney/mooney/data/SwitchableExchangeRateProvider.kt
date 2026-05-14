package com.andriybobchuk.mooney.mooney.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.settings.ExchangeRateSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Routes exchange rate requests to the provider the user picked in Settings.
 * Reads the preference fresh on each call so changes take effect immediately
 * without restart.
 */
class SwitchableExchangeRateProvider(
    private val extended: ExtendedExchangeRateProvider,
    private val historical: LiveExchangeRateProvider,
    private val dataStore: DataStore<Preferences>
) : ExchangeRateProvider {

    override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
        val source = currentSource()
        return when (source) {
            ExchangeRateSource.EXTENDED -> extended.getExchangeRates(baseCurrency)
            ExchangeRateSource.HISTORICAL -> historical.getExchangeRates(baseCurrency)
        }
    }

    private suspend fun currentSource(): ExchangeRateSource {
        val stored = dataStore.data.map { it[PreferencesKeys.EXCHANGE_RATE_SOURCE] }.first()
        return try {
            stored?.let { ExchangeRateSource.valueOf(it) } ?: ExchangeRateSource.EXTENDED
        } catch (e: IllegalArgumentException) {
            ExchangeRateSource.EXTENDED
        }
    }
}
