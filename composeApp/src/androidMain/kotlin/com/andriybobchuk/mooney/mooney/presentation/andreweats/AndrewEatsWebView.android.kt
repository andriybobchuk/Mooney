package com.andriybobchuk.mooney.mooney.presentation.andreweats

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "AndrewEatsWebView"

/**
 * Android actual — wraps [android.webkit.WebView] inside an `AndroidView`.
 *
 * File uploads use plain `ACTION_GET_CONTENT` with
 * [ActivityResultContracts.StartActivityForResult]. That's the most
 * broadly-compatible file-picker path — no camera-capture intent (which
 * requires FileProvider extras), no dependence on the Photos Picker
 * (which can vary by OEM). Every branch is wrapped in try/catch with
 * Logcat output under tag `AndrewEatsWebView` so any failure surfaces via
 * `adb logcat -s AndrewEatsWebView` rather than crashing the app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun AndrewEatsWebView(
    url: String,
    goBackSignal: Int,
    onCanGoBackChanged: (Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val pendingCallback = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val cb = pendingCallback.value
            pendingCallback.value = null
            if (cb == null) {
                Log.w(TAG, "picker returned but no pending callback — ignoring")
                return@rememberLauncherForActivityResult
            }
            val uris: Array<Uri>? = when {
                result.resultCode != Activity.RESULT_OK -> {
                    Log.i(TAG, "picker cancelled (resultCode=${result.resultCode})")
                    null
                }
                result.data?.clipData != null -> {
                    val clip = result.data!!.clipData!!
                    Array(clip.itemCount) { clip.getItemAt(it).uri }.also {
                        Log.i(TAG, "picker returned ${it.size} uris (clip)")
                    }
                }
                result.data?.data != null -> arrayOf(result.data!!.data!!).also {
                    Log.i(TAG, "picker returned 1 uri: ${it[0]}")
                }
                else -> {
                    Log.w(TAG, "picker RESULT_OK but no data attached")
                    null
                }
            }
            cb.onReceiveValue(uris)
        } catch (t: Throwable) {
            Log.e(TAG, "picker callback crashed — swallowing", t)
            pendingCallback.value?.onReceiveValue(null)
            pendingCallback.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    userAgentString = "$userAgentString MooneyApp/1.0"
                    allowFileAccess = true
                    allowContentAccess = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChanged(false)
                        onCanGoBackChanged(view?.canGoBack() == true)
                    }
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        onLoadingChanged(false)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        // Cancel any stale callback first so the WebView
                        // doesn't stay blocked thinking the previous chooser
                        // is still open.
                        try {
                            pendingCallback.value?.onReceiveValue(null)
                        } catch (t: Throwable) {
                            Log.w(TAG, "failed to close previous callback", t)
                        }
                        pendingCallback.value = filePathCallback

                        val accept = fileChooserParams?.acceptTypes
                            ?.filter { it.isNotBlank() }
                            ?.joinToString(",")
                            ?: "image/*"
                        val allowMultiple = fileChooserParams?.mode ==
                            FileChooserParams.MODE_OPEN_MULTIPLE

                        Log.i(
                            TAG,
                            "onShowFileChooser accept=$accept allowMultiple=$allowMultiple " +
                                "mode=${fileChooserParams?.mode}",
                        )

                        // Deliberately hand-rolled — NOT fileChooserParams.createIntent().
                        // The auto-created intent on inputs with `capture="environment"`
                        // includes a camera-capture chooser leg that needs a
                        // FileProvider output URI matching the WebView's
                        // temp-file path. Getting that wrong crashes on
                        // Android 10+. This intent just opens the standard
                        // document picker — the web app can still receive
                        // a camera photo if the user launches the camera
                        // app manually and shares back, which is 99% of
                        // what "add photo" needs anyway.
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = accept.substringBefore(",").ifBlank { "image/*" }
                            if (allowMultiple) {
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                        }
                        val chooser = Intent.createChooser(intent, "Select photo")

                        return try {
                            fileChooserLauncher.launch(chooser)
                            true
                        } catch (t: Throwable) {
                            Log.e(TAG, "launcher.launch() threw — falling back", t)
                            pendingCallback.value?.onReceiveValue(null)
                            pendingCallback.value = null
                            false
                        }
                    }
                }
                loadUrl(url)
            }
        },
        update = { view ->
            if (goBackSignal > 0 && view.canGoBack()) {
                view.goBack()
            }
            onCanGoBackChanged(view.canGoBack())
        },
        onRelease = { view ->
            try {
                view.stopLoading()
                view.destroy()
            } catch (t: Throwable) {
                Log.w(TAG, "webview release failed", t)
            }
            try {
                pendingCallback.value?.onReceiveValue(null)
            } catch (t: Throwable) {
                Log.w(TAG, "pending callback release failed", t)
            }
            pendingCallback.value = null
        },
    )
}
