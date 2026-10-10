package com.andriybobchuk.mooney.core.platform

/**
 * Platform-native bridge for the screenshot OCR import feature.
 *
 * iOS implementation (Swift) presents a SwiftUI PhotosPicker, runs Apple's
 * Vision framework for text recognition on each selected image, and returns
 * raw parsed strings per image. Kotlin side consumes the result and runs the
 * regex-based amount/merchant parsing + dedup pipeline.
 *
 * Android is a no-op placeholder for now; the Phase 3 ship is iOS-only.
 *
 * Design note: we deliberately keep the bridge dumb. All business logic
 * (amount regex, merchant heuristics, duplicate detection) lives in common
 * Kotlin so adding Android later via ML Kit means implementing only the
 * `pickAndExtractText` function — the parsing + dedup is already shared.
 */
interface ScreenshotOcrBridge {
    /**
     * Present the native image picker, run OCR on each selected image, and
     * return the raw text per image via [onResult].
     *
     * Callback-based rather than `suspend` because the Swift side (iOS 15+
     * SwiftUI PhotosPicker + Vision async-await) implements this via a
     * closure parameter — matches the proven [IosBillingBridge] pattern.
     * The common actual wraps this in `suspendCancellableCoroutine` so
     * domain code gets a clean suspend entry point.
     *
     * [onResult] is invoked exactly once:
     * - empty list if the user cancelled the picker,
     * - otherwise one entry per selected image (OCR failures land as
     *   entries with empty `textLines`, surfaced as parse-failures in UI).
     *
     * Images are capped at 20 by the UI layer, not here — if the bridge
     * receives more, it still processes them (defensive).
     */
    fun pickAndExtractText(onResult: (List<ScreenshotOcrResult>) -> Unit)
}

/** Raw per-image OCR result. */
data class ScreenshotOcrResult(
    /** Stable identifier for the source image — PHAsset local id on iOS. */
    val imageId: String,
    /**
     * All text strings Vision detected on this image, in roughly
     * top-to-bottom reading order. Kotlin regex pipeline treats this as
     * a flat corpus — bounding-box clustering was dropped from scope
     * because single-notification screenshots are the dominant case.
     */
    val textLines: List<String>,
    /**
     * Capture timestamp from EXIF, in epoch seconds. Used as a fallback
     * when the notification body itself doesn't contain a parseable time.
     * **Zero means "unknown"** (sentinel instead of nullable so the
     * ObjC-interop bridge stays on primitive `Int64` without the
     * KotlinLong-boxing fragility).
     */
    val captureEpochSeconds: Long
)

/**
 * Global holder for the Swift-provided implementation. Set from
 * iOSApp.swift during AppDelegate init, consumed by the iOS actual
 * [com.andriybobchuk.mooney.core.platform.ScreenshotOcrParser]. Mirrors
 * the pattern used for [IosBillingBridgeHolder].
 */
object ScreenshotOcrBridgeHolder {
    @kotlin.concurrent.Volatile
    var bridge: ScreenshotOcrBridge? = null
}

/** Setter callable from Swift. */
fun setScreenshotOcrBridge(bridge: ScreenshotOcrBridge) {
    ScreenshotOcrBridgeHolder.bridge = bridge
}
