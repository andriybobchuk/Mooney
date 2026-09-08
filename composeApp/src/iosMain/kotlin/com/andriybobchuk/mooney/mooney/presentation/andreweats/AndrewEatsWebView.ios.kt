package com.andriybobchuk.mooney.mooney.presentation.andreweats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIColor
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * iOS actual — hosts a `WKWebView` via Compose Multiplatform's newer
 * `androidx.compose.ui.viewinterop.UIKitView` (Compose 1.7+). Using the
 * new API is important here: with the deprecated
 * `androidx.compose.ui.interop.UIKitView`, Compose intercepts pan gestures
 * before they reach the WKWebView's scroll view, so the page just doesn't
 * scroll. Setting `interactionMode = NonCooperative` hands ALL touches to
 * UIKit, restoring native scroll/zoom/tap behavior inside the WebView.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun AndrewEatsWebView(
    url: String,
    goBackSignal: Int,
    onCanGoBackChanged: (Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val delegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
                onLoadingChanged(true)
            }
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                onLoadingChanged(false)
                onCanGoBackChanged(webView.canGoBack)
            }
            override fun webView(
                webView: WKWebView,
                didFailNavigation: WKNavigation?,
                withError: platform.Foundation.NSError,
            ) {
                onLoadingChanged(false)
            }
            override fun webView(
                webView: WKWebView,
                didFailProvisionalNavigation: WKNavigation?,
                withError: platform.Foundation.NSError,
            ) {
                onLoadingChanged(false)
            }
        }
    }

    val webView = remember {
        val config = WKWebViewConfiguration()
        WKWebView(
            frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 0.0, 0.0),
            configuration = config,
        ).apply {
            navigationDelegate = delegate
            backgroundColor = UIColor.whiteColor
            opaque = true
            NSURL.URLWithString(url)?.let { nsUrl ->
                loadRequest(NSURLRequest.requestWithURL(nsUrl))
            }
        }
    }

    UIKitView(
        factory = { webView },
        modifier = modifier,
        update = { view ->
            if (goBackSignal > 0 && view.canGoBack) {
                view.goBack()
            }
            onCanGoBackChanged(view.canGoBack)
        },
        // Hands every touch to WKWebView first. Without this, Compose eats
        // scroll pans before UIKit sees them and the page feels frozen.
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true,
        ),
    )
}
