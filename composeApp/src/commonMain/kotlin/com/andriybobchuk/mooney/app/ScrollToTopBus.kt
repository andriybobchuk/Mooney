package com.andriybobchuk.mooney.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-screen channel that the bottom navigation uses to ask the currently
 * visible tab to scroll its primary list back to the top. Fired when the user
 * double-taps the already-selected nav item (mirrors the iOS tab-bar idiom).
 *
 * Single Koin singleton — tab screens collect [events] in a `LaunchedEffect`
 * and animate to item 0 when their own [Tab] value lands.
 */
class ScrollToTopBus {

    enum class Tab { TRANSACTIONS, ASSETS, ANALYTICS, SETTINGS }

    private val _events = MutableSharedFlow<Tab>(extraBufferCapacity = 1)
    val events: SharedFlow<Tab> = _events.asSharedFlow()

    fun fire(tab: Tab) {
        _events.tryEmit(tab)
    }
}
