package com.andriybobchuk.mooney.core.presentation.locale

import androidx.compose.runtime.Composable

/**
 * Detects whether the active system locale uses right-to-left layout
 * direction. Wrap your composition root with the resulting
 * `LocalLayoutDirection` override so RTL languages flip the UI:
 *
 * ```
 * CompositionLocalProvider(
 *     LocalLayoutDirection provides if (isCurrentLocaleRtl()) Rtl else Ltr
 * ) { App() }
 * ```
 *
 * Compose Multiplatform's CMP host typically passes through the OS layout
 * direction automatically, but explicit detection lets us ALSO swap layout
 * direction when the user picks a language in-app that differs from the
 * system (a feature shipping in a later release).
 */
@Composable
expect fun isCurrentLocaleRtl(): Boolean
