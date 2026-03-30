package com.andriybobchuk.mooney.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Analytics {
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private val firebaseCrashlytics by lazy { Firebase.crashlytics }

    actual fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    actual fun setUserId(id: String?) {
        firebaseAnalytics.setUserId(id)
        firebaseCrashlytics.setUserId(id.orEmpty())
    }

    actual fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    actual fun recordException(throwable: Throwable, context: Map<String, String>) {
        context.forEach { (key, value) ->
            firebaseCrashlytics.setCustomKey(key, value)
        }
        firebaseCrashlytics.recordException(throwable)
    }

    actual fun log(message: String) {
        firebaseCrashlytics.log(message)
    }

    actual fun setCustomKey(key: String, value: String) {
        firebaseCrashlytics.setCustomKey(key, value)
    }
}
