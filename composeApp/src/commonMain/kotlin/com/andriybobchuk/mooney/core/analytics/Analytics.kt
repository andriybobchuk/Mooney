package com.andriybobchuk.mooney.core.analytics

expect object Analytics {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun setUserId(id: String?)
    fun setUserProperty(name: String, value: String)
    fun recordException(throwable: Throwable, context: Map<String, String> = emptyMap())
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
}
