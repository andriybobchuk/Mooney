package com.andriybobchuk.mooney.core.data.category

interface IosRemoteConfigBridge {
    fun getString(key: String): String
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RemoteConfig {
    private var bridge: IosRemoteConfigBridge? = null

    fun setBridge(bridge: IosRemoteConfigBridge) {
        this.bridge = bridge
    }

    actual suspend fun fetchAndActivate(): Boolean {
        // Firebase Remote Config fetch is handled lazily — getString reads cached values
        return bridge != null
    }

    actual fun getString(key: String): String {
        return try {
            bridge?.getString(key) ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
