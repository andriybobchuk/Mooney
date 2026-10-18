package com.andriybobchuk.mooney.app

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object MooneyGraph : Route

    @Serializable
    data object Accounts : Route

    @Serializable
    data object Transactions : Route

    @Serializable
    data object Exchange : Route

    @Serializable
    data object Analytics : Route

    @Serializable
    data object Goals : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object RecurringTransactions : Route

    @Serializable
    data object TransactionCategories : Route

    @Serializable
    data object AssetCategories : Route

    @Serializable
    data object Categories : Route

    @Serializable
    data object AnalyticsRevenue : Route

    @Serializable
    data object AnalyticsCosts : Route

    @Serializable
    data object AnalyticsTaxes : Route

    @Serializable
    data object AnalyticsNetIncome : Route

    /** Full-screen zoomable timeline chart — opens from a FAB on the main
     *  Analytics screen. Shows every available month end-to-end with
     *  pinch-zoom + horizontal scroll and an overlay card tied to the
     *  user's current month selection on the chart. */
    @Serializable
    data object AnalyticsTimeline : Route

    @Serializable
    data object NetWorthDetail : Route

    @Serializable
    data object ShortcutsOnboarding : Route

    /** Full-screen Upkeep review flow — card-at-a-time category picker for
     *  transactions auto-added by Shortcuts/Siri/Wallet. */
    @Serializable
    data object UpkeepReview : Route

    /** Screenshot-import preview — notification-centre layout.
     *  Picks screenshots of iOS notifications ("Nowa transakcja kartą ... w
     *  wysokości X PLN w <merchant>."), parses them with the notification
     *  regex, and shows the parsed candidates before commit. */
    @Serializable
    data object ScreenshotImportNotifications : Route

    /** Screenshot-import preview — bank-history list layout.
     *  Picks screenshots of the in-bank-app transaction history ("<merchant>
     *  Card operation -X,XX PLN" rows under date headers), parses with the
     *  bank-history regex (PLN-only, negative amounts only), and shows the
     *  parsed candidates before commit. */
    @Serializable
    data object ScreenshotImportBankHistory : Route

    /**
     * Full-screen WebView hosting the AndrewEats web app. Only reachable via
     * the "Eats" pill in Transactions quick-actions, which itself is only
     * visible when the "Display Eats" dev toggle is on.
     */
    @Serializable
    data object AndrewEats : Route

}