package com.andriybobchuk.mooney.core.testing

/**
 * Single source of truth for Maestro-visible testTag identifiers.
 *
 * Rules:
 *  - One const per Maestro-relevant interactive element.
 *  - Values are stable strings — a rename is a breaking change to the flow suite.
 *  - `TestTags.kt` const count must only ever grow (asserted in CI).
 */
object TestTags {
    // Bottom navigation
    const val NAV_TRANSACTIONS = "nav_transactions"
    const val NAV_ACCOUNTS = "nav_accounts"
    const val NAV_ANALYTICS = "nav_analytics"
    const val NAV_SETTINGS = "nav_settings"

    // Transactions screen
    const val FAB_ADD_TXN = "fab_add_txn"
    const val TXN_LIST = "txn_list"
    const val TXN_AMOUNT_FIELD = "txn_amount_field"
    const val TXN_CATEGORY_PICKER = "txn_category_picker"
    const val TXN_SAVE_BUTTON = "txn_save"
    const val TXN_ACTION_DELETE = "txn_action_delete"
    const val TXN_CONFIRM_DELETE = "txn_confirm_delete"
    fun txnRow(id: Long): String = "txn_row_$id"

    // Accounts / net worth
    const val ACCOUNT_LIST = "account_list"
    const val NET_WORTH_LABEL = "net_worth_label"
    const val FAB_ADD_ACCOUNT = "fab_add_account"
    const val ACCOUNT_TITLE_FIELD = "account_title_field"
    const val ACCOUNT_AMOUNT_FIELD = "account_amount_field"
    const val ACCOUNT_SAVE_BUTTON = "account_save"
    fun accountRow(id: Long): String = "account_row_$id"

    // Paywall
    const val PAYWALL_SHEET = "paywall_sheet"
    const val PAYWALL_DISMISS = "paywall_dismiss"

    // Onboarding
    const val ONBOARDING_GET_STARTED = "onboarding_get_started"
    fun onboardingCurrency(code: String): String = "onboarding_currency_$code"

    // Settings
    const val SETTINGS_THEME = "settings_theme"
    const val SETTINGS_DARK_MODE_TOGGLE = "settings_dark_mode"
    const val SETTINGS_BASE_CURRENCY = "settings_base_currency"
}
