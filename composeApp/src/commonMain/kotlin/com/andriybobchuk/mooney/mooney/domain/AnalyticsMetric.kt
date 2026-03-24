package com.andriybobchuk.mooney.mooney.domain

data class AnalyticsMetric(
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val color: Long,
    val trendPercentage: Double = 0.0,
    val isClickable: Boolean = false
)
