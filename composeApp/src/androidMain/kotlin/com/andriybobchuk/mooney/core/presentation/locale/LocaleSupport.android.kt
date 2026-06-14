package com.andriybobchuk.mooney.core.presentation.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import android.view.View
import java.util.Locale

@Composable
actual fun isCurrentLocaleRtl(): Boolean {
    // Android already exposes layoutDirection via Configuration once locale
    // changes. Honour both the system locale (the usual signal) AND the app
    // configuration's layoutDirection which Compose flips for us when
    // android:supportsRtl="true" — we rely on the latter as the more accurate
    // source.
    val config = LocalConfiguration.current
    return config.layoutDirection == View.LAYOUT_DIRECTION_RTL ||
        Locale.getDefault().language in RTL_LANGUAGES
}

private val RTL_LANGUAGES = setOf("ar", "fa", "he", "iw", "ur")
