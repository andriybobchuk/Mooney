package com.andriybobchuk.mooney.core.presentation.locale

import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

@Composable
actual fun isCurrentLocaleRtl(): Boolean {
    val lang = NSLocale.currentLocale.languageCode
    return lang in RTL_LANGUAGES
}

private val RTL_LANGUAGES = setOf("ar", "fa", "he", "iw", "ur")
