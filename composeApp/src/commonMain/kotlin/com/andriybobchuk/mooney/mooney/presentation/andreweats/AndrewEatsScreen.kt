package com.andriybobchuk.mooney.mooney.presentation.andreweats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.Icons

/**
 * Where the WebView points. Netlify hostname for the AndrewEats deploy —
 * swap here if we ever move to a custom domain.
 */
private const val ANDREW_EATS_URL = "https://mooney-app.netlify.app"

/**
 * Full-bleed WebView host. No top bar / no chrome — the web app has its
 * own header and bottom nav, we just give it the whole screen. A small
 * translucent back arrow floats in the top-left corner (with status-bar
 * padding) so iOS users always have a way out; on Android the system Back
 * gesture also pops back to Transactions.
 *
 * The back button walks WebView history first via a signal counter and
 * only exits the composable when history is empty — bumping an `Int` on
 * each press so repeated taps register as distinct events (a Boolean
 * toggle wouldn't).
 */
@Composable
fun AndrewEatsScreen(onBackClick: () -> Unit) {
    var canGoBack by remember { mutableStateOf(false) }
    var goBackSignal by remember { mutableIntStateOf(0) }

    val handleBack: () -> Unit = {
        if (canGoBack) goBackSignal += 1 else onBackClick()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AndrewEatsWebView(
            url = ANDREW_EATS_URL,
            goBackSignal = goBackSignal,
            onCanGoBackChanged = { canGoBack = it },
            onLoadingChanged = { /* no-op — no spinner chrome */ },
            modifier = Modifier.fillMaxSize(),
        )

        // Floating exit affordance — kept inside the status-bar-safe area so
        // it doesn't sit under the notch/pill. Semi-opaque so it reads on any
        // page background but doesn't dominate the visual.
        IconButton(
            onClick = handleBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 8.dp, top = 4.dp)
                .size(40.dp)
                .clip(CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
            ),
        ) {
            Icon(
                painter = Icons.BackIcon(),
                contentDescription = null,
            )
        }
    }
}
