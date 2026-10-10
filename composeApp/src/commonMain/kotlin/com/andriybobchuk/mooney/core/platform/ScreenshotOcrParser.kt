package com.andriybobchuk.mooney.core.platform

/**
 * Thin facade that domain code consumes. Hides the bridge-indirection
 * (Swift-set holder on iOS, no-op stub on Android) behind a single suspend
 * entry point. VMs inject this via Koin and call [pickAndExtractText]
 * without knowing which platform they're on.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class ScreenshotOcrParser() {
    suspend fun pickAndExtractText(): List<ScreenshotOcrResult>
}
