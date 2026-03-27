package com.andriybobchuk.mooney.core.analytics

expect object Analytics {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun setUserId(id: String?)
}
