package com.andriybobchuk.mooney.core.widgets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of the widget snapshot bridge.
 *
 * We write the serialized JSON into a shared App-Group `NSUserDefaults` so
 * the WidgetKit extension (a separate process) can read it. The main app
 * cannot call `WidgetCenter.reloadAllTimelines()` from Kotlin/Native, so the
 * Swift side registers a callback via [IosWidgetKitBridge] that we invoke
 * after every write. Bridge is set from `iOSApp.swift`; when it's null (dev
 * builds, unit tests) we still write the payload so the next natural WidgetKit
 * timeline refresh picks it up.
 */
class IosWidgetSnapshotWriter(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : WidgetSnapshotWriter {

    override suspend fun write(snapshot: WidgetDataSnapshot) {
        try {
            val serialized = json.encodeToString(snapshot)
            val defaults = NSUserDefaults(
                suiteName = WidgetSnapshotStorage.IOS_APP_GROUP
            ) ?: NSUserDefaults.standardUserDefaults
            defaults.setObject(serialized, WidgetSnapshotStorage.IOS_SNAPSHOT_KEY)
            WidgetKitBridgeRegistry.bridge?.reloadAllTimelines()
        } catch (_: Throwable) {
            // Widget writes are best-effort — never crash the app on failure.
        }
    }
}

/**
 * Callback surface implemented by Swift. Kept intentionally tiny — the only
 * reason we call across the framework boundary is to tell WidgetKit that
 * fresh data landed.
 */
interface IosWidgetKitBridge {
    fun reloadAllTimelines()
}

/**
 * Singleton registry that holds the Swift-side [IosWidgetKitBridge]. Exposed
 * as a Kotlin `object` (Swift sees `WidgetKitBridgeRegistry.shared`) so
 * `iOSApp.swift`'s AppDelegate can register the bridge with one line, exactly
 * like `Ads.shared.setBridge(...)` does today.
 */
object WidgetKitBridgeRegistry {
    // No volatile — the setter is only called once from iOSApp's main-thread
    // AppDelegate init, so cross-thread visibility isn't a concern here.
    internal var bridge: IosWidgetKitBridge? = null
        private set

    fun setBridge(bridge: IosWidgetKitBridge) {
        this.bridge = bridge
    }
}
