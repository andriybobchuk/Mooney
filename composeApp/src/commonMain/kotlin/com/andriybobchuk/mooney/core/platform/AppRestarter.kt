package com.andriybobchuk.mooney.core.platform

/**
 * Force the app to fully terminate so the next launch reads the freshly-flipped
 * demo-mode preference before Room opens a database file. Room caches the file
 * handle in a process singleton, so a hot swap isn't safe; a clean process
 * restart is the only way to guarantee the new file is used.
 *
 * - **Android** kills the current process — the launcher icon is still there
 *   and the user reopens with one tap.
 * - **iOS** returns without doing anything (Apple doesn't allow programmatic
 *   quit). The caller must show a "please quit the app and reopen" dialog.
 *   [canRestart] surfaces this so the UI can choose the right copy.
 */
expect class AppRestarter {
    val canRestart: Boolean
    fun restart()
}
