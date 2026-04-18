package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.HistoricalRate

class CalculateRatePercentileUseCase {
    operator fun invoke(currentRate: Double, historicalRates: List<HistoricalRate>): Int {
        if (historicalRates.isEmpty()) return 50
        val belowOrEqual = historicalRates.count { it.rate <= currentRate }
        return ((belowOrEqual.toDouble() / historicalRates.size) * 100).toInt().coerceIn(0, 100)
    }
}
