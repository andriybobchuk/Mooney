package com.andriybobchuk.mooney.core.data.category

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.tasks.await

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RemoteConfig {
    actual suspend fun fetchAndActivate(): Boolean {
        return try {
            Firebase.remoteConfig.fetchAndActivate().await()
        } catch (_: Exception) {
            false
        }
    }

    actual fun getString(key: String): String {
        return try {
            Firebase.remoteConfig.getString(key)
        } catch (_: Exception) {
            ""
        }
    }
}
