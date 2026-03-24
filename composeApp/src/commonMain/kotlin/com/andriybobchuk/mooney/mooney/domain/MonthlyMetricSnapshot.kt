package com.andriybobchuk.mooney.mooney.domain

data class MonthlyMetricSnapshot(
    val month: MonthKey,
    val revenue: Double,
    val taxes: Double,
    val operatingCosts: Double,
    val netIncome: Double,
    val transactionCount: Int
)
