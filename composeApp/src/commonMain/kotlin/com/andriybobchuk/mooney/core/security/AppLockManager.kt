package com.andriybobchuk.mooney.core.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * App Lock — PIN-gated entry. Premium-only at the entry point (Settings),
 * but once configured, the gate runs for everyone regardless of subscription
 * state so a lapsed subscription doesn't expose data.
 *
 * Hashing uses FNV-1a 64-bit with a constant salt. A 4–6 digit PIN's threat
 * model is "device unlock theft + filesystem access" — at that point the
 * attacker has bigger primitives than reversing this hash; what we want is
 * to avoid storing the PIN in literal cleartext and to make brute-force
 * across a stolen DataStore snapshot non-trivial.
 */
class AppLockManager(
    private val dataStore: DataStore<Preferences>,
    private val startupPrefs: com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
) {
    val isLockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        !prefs[PreferencesKeys.APP_LOCK_PIN_HASH].isNullOrBlank()
    }

    suspend fun isLockEnabledNow(): Boolean {
        val stored = dataStore.data.first()[PreferencesKeys.APP_LOCK_PIN_HASH]
        return !stored.isNullOrBlank()
    }

    /** Synchronous read from the SharedPreferences/NSUserDefaults mirror. Used
     *  by `App()` on frame 1 to decide whether to render the lock screen
     *  without the blank-box stall that a DataStore suspend read caused. */
    fun isLockEnabledFast(): Boolean = startupPrefs.getAppLockEnabled() ?: false

    suspend fun setPin(pin: String) {
        dataStore.edit { it[PreferencesKeys.APP_LOCK_PIN_HASH] = hashPin(pin) }
        startupPrefs.setAppLockEnabled(true)
    }

    suspend fun disable() {
        dataStore.edit { it.remove(PreferencesKeys.APP_LOCK_PIN_HASH) }
        startupPrefs.setAppLockEnabled(false)
    }

    suspend fun verify(pin: String): Boolean {
        val stored = dataStore.data.first()[PreferencesKeys.APP_LOCK_PIN_HASH] ?: return false
        return hashPin(pin) == stored
    }

    private fun hashPin(pin: String): String {
        val salted = "$SALT::$pin"
        var hash = FNV_OFFSET_BASIS
        salted.forEach { c ->
            hash = hash xor c.code.toLong()
            hash *= FNV_PRIME
        }
        return hash.toString()
    }

    private companion object {
        const val SALT = "mooney_app_lock_salt_v1"
        const val FNV_OFFSET_BASIS = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
    }
}
