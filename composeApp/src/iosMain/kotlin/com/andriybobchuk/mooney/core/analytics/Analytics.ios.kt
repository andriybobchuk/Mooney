package com.andriybobchuk.mooney.core.analytics

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Analytics {
    actual fun logEvent(name: String, params: Map<String, String>) {
        // iOS Firebase Analytics is initialized in Swift via FirebaseApp.configure()
        // Event logging from Kotlin/Native requires cocoapods or direct interop
        // For now, basic events are auto-collected by Firebase iOS SDK
    }

    actual fun setUserId(id: String?) {
        // Set via Swift side if needed
    }
}
