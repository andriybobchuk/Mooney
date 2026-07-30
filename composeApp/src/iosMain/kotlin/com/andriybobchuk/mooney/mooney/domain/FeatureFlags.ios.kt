package com.andriybobchuk.mooney.mooney.domain

// iOS binaries that reach the App Store are always release builds, and
// Kotlin/Native's `Platform.isDebugBinary` is @ExperimentalNativeApi with
// KMP-version-specific behavior. Hardcode false here — iOS devs test ad
// UI by flipping FeatureFlags.adsAlwaysShow, not by relying on build type.
actual val isDebugBuild: Boolean = false
