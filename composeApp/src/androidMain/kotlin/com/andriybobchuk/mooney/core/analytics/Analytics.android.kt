package com.andriybobchuk.mooney.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Analytics {
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }

    actual fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    actual fun setUserId(id: String?) {
        firebaseAnalytics.setUserId(id)
    }
}
