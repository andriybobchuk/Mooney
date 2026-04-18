package com.andriybobchuk.mooney.core.data.category

expect object RemoteConfig {
    suspend fun fetchAndActivate(): Boolean
    fun getString(key: String): String
}
