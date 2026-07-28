package com.andriybobchuk.mooney.core.widgets

/**
 * Platform-specific persistence + notification for the widget snapshot.
 *
 * Android impl writes JSON to app-private files, then broadcasts a Glance
 * state update so every installed widget re-renders on the next frame.
 * iOS impl writes to the shared App-Group `UserDefaults` and pings
 * `WidgetCenter.shared.reloadAllTimelines()` so WidgetKit rebuilds the
 * timeline entries.
 *
 * The `write` call is deliberately fire-and-forget from the caller's PoV: it
 * suspends only long enough to serialize + persist, then returns. The actual
 * widget host redraw happens asynchronously — Glance and WidgetKit both
 * throttle re-renders to avoid battery drain.
 */
interface WidgetSnapshotWriter {
    suspend fun write(snapshot: WidgetDataSnapshot)
}

/** Shared filename/key so Android and iOS agree on the payload location. */
object WidgetSnapshotStorage {
    /** Filename inside `context.filesDir` on Android. */
    const val ANDROID_FILENAME: String = "widget-snapshot.json"

    /** Shared App-Group suite name on iOS. Must match iosApp entitlement. */
    const val IOS_APP_GROUP: String = "group.com.andriybobchuk.mooney.widgets"

    /** Key under the App-Group UserDefaults where the JSON blob lives. */
    const val IOS_SNAPSHOT_KEY: String = "widget-snapshot-v1"
}
