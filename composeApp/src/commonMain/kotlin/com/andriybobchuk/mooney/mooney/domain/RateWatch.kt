package com.andriybobchuk.mooney.mooney.domain

import kotlinx.datetime.LocalDate

data class RateWatchAlert(
    val id: Int = 0,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val targetRate: Double,
    val direction: AlertDirection,
    val isActive: Boolean = true,
    val createdDate: LocalDate
)

enum class AlertDirection { ABOVE, BELOW }

data class TriggeredAlert(
    val alert: RateWatchAlert,
    val currentRate: Double
)
