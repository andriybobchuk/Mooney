package com.andriybobchuk.mooney.core.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit
): () -> Unit {
    // iOS surfaces the system prompt automatically the first time we
    // schedule via UNUserNotificationCenter.requestAuthorizationWithOptions
    // (see ReminderScheduler.ios.kt). No-op here — just signal success so
    // the calling flow proceeds to scheduling, where the real prompt lives.
    return remember { { onResult(true) } }
}
