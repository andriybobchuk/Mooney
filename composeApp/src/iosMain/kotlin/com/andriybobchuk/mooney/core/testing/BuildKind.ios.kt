package com.andriybobchuk.mooney.core.testing

import platform.Foundation.NSProcessInfo

/**
 * iOS runtime-only detection. `true` iff the process was launched with a
 * `--e2e` argument (Maestro sets this via `launchApp: { arguments: {…} }`).
 *
 * The threat model: App Store IPAs ship without debug symbols, so a
 * runtime boolean gate has a much smaller information leak than the
 * Kotlin/Native framework symbol table. If compile-time exclusion is
 * ever needed on iOS, add an `iosE2eMain` source set gated by a Gradle
 * property — deferred until proven necessary.
 */
@Suppress("MayBeConstant")
actual val isE2eBuild: Boolean
    get() = NSProcessInfo.processInfo.arguments.any { it == "--e2e" }
