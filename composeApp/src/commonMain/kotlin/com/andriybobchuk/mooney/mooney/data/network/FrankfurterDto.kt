package com.andriybobchuk.mooney.mooney.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FrankfurterLatestResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

@Serializable
data class FrankfurterHistoricalResponse(
    val base: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val rates: Map<String, Map<String, Double>>
)
