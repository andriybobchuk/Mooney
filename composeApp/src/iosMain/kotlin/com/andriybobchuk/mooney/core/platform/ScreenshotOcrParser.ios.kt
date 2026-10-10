package com.andriybobchuk.mooney.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class ScreenshotOcrParser {
    actual suspend fun pickAndExtractText(): List<ScreenshotOcrResult> {
        // Bridge is set by iOSApp.swift's AppDelegate before any import flow
        // kicks off. If missing, we return empty and let the UI render its
        // "no results" state — not throwing because the user already
        // interacted with a chip and expects a soft failure, not a crash.
        val bridge = ScreenshotOcrBridgeHolder.bridge ?: return emptyList()
        // Bridge is callback-based (matches the IosBillingBridge pattern);
        // wrap it here so domain code sees a clean suspend entry point.
        return suspendCancellableCoroutine { continuation ->
            bridge.pickAndExtractText { results ->
                if (continuation.isActive) {
                    continuation.resume(results)
                }
            }
        }
    }
}
