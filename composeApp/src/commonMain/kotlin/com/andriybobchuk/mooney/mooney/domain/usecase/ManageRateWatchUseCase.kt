package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RateWatchAlertDao
import com.andriybobchuk.mooney.core.data.database.RateWatchAlertEntity
import com.andriybobchuk.mooney.mooney.domain.AlertDirection
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.RateWatchAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ManageRateWatchUseCase(
    private val rateWatchAlertDao: RateWatchAlertDao
) {
    fun getAllAlerts(): Flow<List<RateWatchAlert>> {
        return rateWatchAlertDao.getAll().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    RateWatchAlert(
                        id = entity.id,
                        fromCurrency = Currency.valueOf(entity.fromCurrency),
                        toCurrency = Currency.valueOf(entity.toCurrency),
                        targetRate = entity.targetRate,
                        direction = AlertDirection.valueOf(entity.direction),
                        isActive = entity.isActive,
                        createdDate = LocalDate.parse(entity.createdDate)
                    )
                } catch (_: Exception) { null }
            }
        }
    }

    suspend fun saveAlert(
        fromCurrency: Currency,
        toCurrency: Currency,
        targetRate: Double,
        direction: AlertDirection
    ) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        rateWatchAlertDao.upsert(
            RateWatchAlertEntity(
                fromCurrency = fromCurrency.name,
                toCurrency = toCurrency.name,
                targetRate = targetRate,
                direction = direction.name,
                isActive = true,
                createdDate = today.toString()
            )
        )
    }

    suspend fun deleteAlert(id: Int) {
        rateWatchAlertDao.delete(id)
    }

    suspend fun deactivateAlert(id: Int) {
        rateWatchAlertDao.deactivate(id)
    }
}
