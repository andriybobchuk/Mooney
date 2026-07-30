package com.andriybobchuk.mooney.mooney.domain

data class AnalyticsMetric(
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val color: Long,
    val trendPercentage: Double = 0.0,
    val isClickable: Boolean = false,
    /**
     * Formatted last-month value for the small "Compared to $X last month"
     * subline under the trend pill. Null when there's no prior month to
     * compare against (fresh install or the metric was zero last month).
     * Pre-formatted with the base currency symbol so the composable stays
     * ignorant of currency state.
     */
    val previousValueFormatted: String? = null
)
