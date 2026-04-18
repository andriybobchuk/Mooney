package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.data.safeCall
import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.core.domain.map
import com.andriybobchuk.mooney.mooney.data.network.FrankfurterHistoricalResponse
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.HistoricalRateProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.datetime.LocalDate

class FrankfurterHistoricalRateProvider(
    private val httpClient: HttpClient
) : HistoricalRateProvider {

    companion object {
        private const val BASE_URL = "https://api.frankfurter.dev/v1"
    }

    override suspend fun getRates(
        from: Currency,
        to: List<Currency>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<Currency, List<HistoricalRate>>, DataError.Remote> {
        val toCsv = to.joinToString(",") { it.name }
        return safeCall<FrankfurterHistoricalResponse> {
            httpClient.get("$BASE_URL/$startDate..$endDate?from=${from.name}&to=$toCsv")
        }.map { response ->
            mapToHistoricalRates(response)
        }
    }

    private fun mapToHistoricalRates(response: FrankfurterHistoricalResponse): Map<Currency, List<HistoricalRate>> {
        val result = mutableMapOf<Currency, MutableList<HistoricalRate>>()

        for ((dateStr, currencyRates) in response.rates) {
            val date = LocalDate.parse(dateStr)
            for ((currencyName, rate) in currencyRates) {
                val currency = try { Currency.valueOf(currencyName) } catch (_: Exception) { continue }
                result.getOrPut(currency) { mutableListOf() }.add(HistoricalRate(date, rate))
            }
        }

        // Sort each list by date
        return result.mapValues { (_, rates) -> rates.sortedBy { it.date } }
    }
}
