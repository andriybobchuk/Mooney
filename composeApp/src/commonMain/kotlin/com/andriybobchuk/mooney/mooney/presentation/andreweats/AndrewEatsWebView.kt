package com.andriybobchuk.mooney.mooney.presentation.andreweats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-bleed WebView hosting the AndrewEats web app.
 *
 * - **Android** wraps [android.webkit.WebView] via `AndroidView`, with JS,
 *   DOM storage and IndexedDB enabled (Firestore relies on all three).
 *   `onCanGoBackChanged` reports whether the WebView has back-history so
 *   the hosting screen can route the system Back gesture into it before
 *   popping the composable.
 * - **iOS** wraps `WKWebView` via `UIKitView`. Same idea for back nav.
 *
 * Pass in a stable [url] — the WebView is not recreated when the parent
 * recomposes, so changes to `url` after first render are ignored (fine for
 * our use case; the URL is a compile-time const).
 */
@Composable
expect fun AndrewEatsWebView(
    url: String,
    goBackSignal: Int,
    onCanGoBackChanged: (Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
