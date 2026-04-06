package com.andriybobchuk.mooney.app

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object MooneyGraph : Route

    @Serializable
    data object Accounts : Route

    @Serializable
    data object Transactions : Route

    @Serializable
    data object Exchange : Route

    @Serializable
    data object Analytics : Route

    @Serializable
    data object Goals : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object RecurringTransactions : Route

}