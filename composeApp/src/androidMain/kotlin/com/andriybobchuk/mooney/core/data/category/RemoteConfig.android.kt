package com.andriybobchuk.mooney.core.data.category

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RemoteConfig {

    // Firebase's default minimumFetchInterval is 12 HOURS, which meant every
    // console change we made was invisible to the app until half a day
    // passed. 15 minutes is well above the "5 fetches per 60-minute window"
    // server cap, and short enough that manual toggle-and-restart iteration
    // during release prep actually reflects the console.
    //
    // Debug builds use 0 so devs testing new keys see them immediately.
    private const val FETCH_INTERVAL_SECONDS_RELEASE = 900L
    private const val FETCH_INTERVAL_SECONDS_DEBUG = 0L

    private var configured = false

    private fun ensureConfigured() {
        if (configured) return
        try {
            val intervalSec = if (
                com.andriybobchuk.mooney.mooney.domain.FeatureFlags.isDebug
            ) {
                FETCH_INTERVAL_SECONDS_DEBUG
            } else {
                FETCH_INTERVAL_SECONDS_RELEASE
            }
            Firebase.remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = intervalSec }
            )
            // Live push updates — when a console value changes and Firebase
            // has a socket open to our device, the change gets pushed
            // immediately (no fetch interval delay). Best-effort; if the
            // listener registration fails we fall back to the interval fetch.
            Firebase.remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Firebase.remoteConfig.activate()
                }
                override fun onError(error: FirebaseRemoteConfigException) { /* ignore */ }
            })
            configured = true
        } catch (_: Exception) {
            /* leave configured = false so we'll try again on next call */
        }
    }

    actual suspend fun fetchAndActivate(): Boolean {
        ensureConfigured()
        return try {
            Firebase.remoteConfig.fetchAndActivate().await()
        } catch (_: Exception) {
            false
        }
    }

    actual fun getString(key: String): String {
        ensureConfigured()
        return try {
            Firebase.remoteConfig.getString(key)
        } catch (_: Exception) {
            ""
        }
    }
}
