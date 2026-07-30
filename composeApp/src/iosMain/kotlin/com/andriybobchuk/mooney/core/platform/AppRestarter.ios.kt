package com.andriybobchuk.mooney.core.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppRestarter {
    // Apple explicitly disallows programmatic app termination (HIG "Don't
    // Programmatically Quit iOS Apps") and shipping exit(0) is a review
    // rejection risk. Return false so the UI can render a "please quit and
    // reopen" instruction instead of pretending to restart.
    actual val canRestart: Boolean = false

    actual fun restart() {
        // No-op — see [canRestart] docstring.
    }
}
