package com.andriybobchuk.mooney.app

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object MooneyGraph : Route

    @Serializable
    data object Accounts : Route

    @Serializable
    data object Transactions : Route

    @Serializable
    data object Analytics : Route

    @Serializable
    data object Goals : Route


    @Serializable
    data object BookGraph : Route

    @Serializable
    data object BookList : Route

    @Serializable
    data class BookDetail(val id: String) : Route
}