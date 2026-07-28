package com.andriybobchuk.mooney.core.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android implementation of the widget snapshot bridge.
 *
 * Persistence path:
 *  1. Serialize `WidgetDataSnapshot` to JSON.
 *  2. Atomic-write to `<filesDir>/widget-snapshot.json` (tmp + rename so a
 *     process kill mid-write never leaves a truncated file behind).
 *  3. Broadcast a Glance update to every installed widget so the OS calls
 *     back into our `GlanceAppWidget.provideGlance()` on the next frame.
 *
 * Glance's `updateAll` walks every glanceId currently placed by the user and
 * re-runs its composition pass, which will then re-read our JSON. Cheap when
 * no widgets exist (early-returns internally).
 */
class AndroidWidgetSnapshotWriter(
    private val context: Context,
    private val json: Json = defaultJson
) : WidgetSnapshotWriter {

    override suspend fun write(snapshot: WidgetDataSnapshot) {
        withContext(Dispatchers.IO) {
            try {
                val serialized = json.encodeToString(snapshot)
                val target = File(context.filesDir, WidgetSnapshotStorage.ANDROID_FILENAME)
                val tmp = File(context.filesDir, WidgetSnapshotStorage.ANDROID_FILENAME + ".tmp")
                tmp.writeText(serialized)
                if (!tmp.renameTo(target)) {
                    // Fallback if rename fails on some FS — do a straight overwrite.
                    target.writeText(serialized)
                    tmp.delete()
                }
                notifyAllWidgets()
            } catch (t: Throwable) {
                // Best-effort — never crash the main app for a widget write.
                Log.w(TAG, "widget snapshot write failed", t)
            }
        }
    }

    /**
     * Fan-out to every installed Mooney widget so the Glance runtime redraws.
     * Wrapped in a per-widget try/catch so an unhealthy provider doesn't stop
     * the sibling widgets from getting the fresh state.
     */
    private suspend fun notifyAllWidgets() {
        val manager = GlanceAppWidgetManager(context)
        val providers = listOf(
            com.andriybobchuk.mooney.widgets.BalanceWidget(),
            com.andriybobchuk.mooney.widgets.TodaySpendingWidget(),
            com.andriybobchuk.mooney.widgets.BudgetProgressWidget(),
            com.andriybobchuk.mooney.widgets.StreakWidget(),
            com.andriybobchuk.mooney.widgets.QuickAddWidget()
        )
        providers.forEach { widget ->
            try {
                widget.updateAll(context)
            } catch (t: Throwable) {
                Log.w(TAG, "updateAll failed for ${widget::class.simpleName}", t)
            }
        }
    }

    companion object {
        private const val TAG = "WidgetWriter"

        /** JSON with defaults + ignore-unknown so schema bumps are forward-compat. */
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/**
 * Reader used by every Glance widget's `provideGlance` block. Falls back to
 * `WidgetDataSnapshot.Empty` if the file doesn't exist yet (first launch,
 * before the coordinator has run once) or if the JSON is unparseable
 * (post-uninstall, schema mismatch across an OTA before the app restarts).
 */
class AndroidWidgetSnapshotReader(
    private val context: Context,
    private val json: Json = AndroidWidgetSnapshotWriter.defaultJson
) {
    fun read(): WidgetDataSnapshot {
        val file = File(context.filesDir, WidgetSnapshotStorage.ANDROID_FILENAME)
        if (!file.exists()) return WidgetDataSnapshot.Empty
        return try {
            val serialized = file.readText()
            json.decodeFromString<WidgetDataSnapshot>(serialized)
        } catch (t: Throwable) {
            Log.w("WidgetReader", "snapshot decode failed, using Empty", t)
            WidgetDataSnapshot.Empty
        }
    }
}
