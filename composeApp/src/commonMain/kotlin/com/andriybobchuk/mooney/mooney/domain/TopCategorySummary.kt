package com.andriybobchuk.mooney.mooney.domain

data class TopCategorySummary(
    val category: Category,
    val amount: Double,
    val formatted: String,
    val percentOfRevenue: String,
    val trendPercentage: Double = 0.0
)
