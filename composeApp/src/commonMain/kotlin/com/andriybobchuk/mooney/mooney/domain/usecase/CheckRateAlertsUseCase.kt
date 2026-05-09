package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RateWatchAlertDao
import com.andriybobchuk.mooney.mooney.domain.AlertDirection
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.RateWatchAlert
import com.andriybobchuk.mooney.mooney.domain.TriggeredAlert
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class CheckRateAlertsUseCase(
    private val rateWatchAlertDao: RateWatchAlertDao
) {
    suspend operator fun invoke(currentRates: Map<Currency, Double>, baseCurrency: Currency): List<TriggeredAlert> {
        val activeAlerts = rateWatchAlertDao.getAllActive().first()
        return activeAlerts.mapNotNull { entity ->
            val from = try { Currency.valueOf(entity.fromCurrency) } catch (_: Exception) { return@mapNotNull null }
            val to = try { Currency.valueOf(entity.toCurrency) } catch (_: Exception) { return@mapNotNull null }

            // Get the rate for this pair relative to the alert's base
            val currentRate = if (from == baseCurrency) {
                currentRates[to]
            } else {
                // Cross-rate: need to calculate from→to via baseCurrency
                val fromRate = currentRates[from] ?: return@mapNotNull null
                val toRate = currentRates[to] ?: return@mapNotNull null
                if (fromRate == 0.0) return@mapNotNull null
                toRate / fromRate
            } ?: return@mapNotNull null

            val direction = try { AlertDirection.valueOf(entity.direction) } catch (_: Exception) { return@mapNotNull null }
            val triggered = when (direction) {
                AlertDirection.ABOVE -> currentRate >= entity.targetRate
                AlertDirection.BELOW -> currentRate <= entity.targetRate
            }

            if (triggered) {
                val alert = RateWatchAlert(
                    id = entity.id,
                    fromCurrency = from,
                    toCurrency = to,
                    targetRate = entity.targetRate,
                    direction = direction,
                    isActive = entity.isActive,
                    createdDate = try { LocalDate.parse(entity.createdDate) } catch (_: Exception) { return@mapNotNull null }
                )
                TriggeredAlert(alert, currentRate)
            } else null
        }
    }
}
