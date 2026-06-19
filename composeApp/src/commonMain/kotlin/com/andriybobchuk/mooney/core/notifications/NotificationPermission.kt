package com.andriybobchuk.mooney.core.notifications

import androidx.compose.runtime.Composable

/**
 * Returns a callable that requests the OS notification permission.
 *
 * On Android 13+ (API 33) this triggers the POST_NOTIFICATIONS runtime
 * prompt; on older Android versions the permission is granted at install
 * time so the call is a no-op. On iOS the OS prompt is surfaced by the
 * scheduler itself when the first notification is scheduled, so this is a
 * no-op too — see ReminderScheduler.ios.kt.
 *
 * The callback fires with the user's decision (true = granted). Callers
 * MAY proceed to schedule even on `false` because the OS will simply not
 * surface the notification — better than blocking the toggle flow.
 */
@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit
): () -> Unit
