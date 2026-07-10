package com.andriybobchuk.mooney.core.platform

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Bridge between Android's [ActivityResultContracts.OpenDocument] (which must
 * be registered before the host activity reaches STARTED) and our suspending
 * common-code [FileHandler.pickAndReadTextFile] API. Registered once in
 * [com.andriybobchuk.mooney.MainActivity.onCreate]; FileHandler grabs it from
 * Koin and awaits the result on demand.
 */
class FilePickerLauncher {

    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<Uri?>? = null

    fun attach(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val deferred = pending
            pending = null
            deferred?.complete(uri)
        }
    }

    suspend fun pick(mimeTypes: Array<String>): Uri? {
        val launcher = launcher ?: return null
        // If a previous pick is still outstanding, drop it — only one
        // document picker can be on screen at a time anyway.
        pending?.complete(null)
        val deferred = CompletableDeferred<Uri?>()
        pending = deferred
        launcher.launch(mimeTypes)
        return deferred.await()
    }
}
