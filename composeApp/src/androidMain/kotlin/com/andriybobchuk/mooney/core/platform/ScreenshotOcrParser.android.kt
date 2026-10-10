package com.andriybobchuk.mooney.core.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class ScreenshotOcrParser {
    // Android implementation is deferred. Phase 3 ships iOS-only; when
    // Android gets its turn, swap in ML Kit Text Recognition here. The UI
    // layer hides the "import screenshots" chip on Android (see
    // TransactionScreen) so this function is never called in practice.
    actual suspend fun pickAndExtractText(): List<ScreenshotOcrResult> = emptyList()
}
