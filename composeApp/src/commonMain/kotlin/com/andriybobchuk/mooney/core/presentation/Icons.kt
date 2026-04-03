package com.andriybobchuk.mooney.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.goals
import mooney.composeapp.generated.resources.ic_back
import mooney.composeapp.generated.resources.ic_calendar
import mooney.composeapp.generated.resources.ic_chevron_left
import mooney.composeapp.generated.resources.ic_chevron_right
import mooney.composeapp.generated.resources.ic_recurring
import mooney.composeapp.generated.resources.ic_refresh
import mooney.composeapp.generated.resources.ic_settings
import mooney.composeapp.generated.resources.ic_stats
import mooney.composeapp.generated.resources.ic_stats_filled
import mooney.composeapp.generated.resources.ic_transactions
import mooney.composeapp.generated.resources.ic_transactions_filled
import mooney.composeapp.generated.resources.ic_wallet
import mooney.composeapp.generated.resources.ic_wallet_filled
import org.jetbrains.compose.resources.painterResource

@Suppress("TooManyFunctions")
object Icons {
    // Bottom nav — outlined
    @Composable
    fun TransactionsIcon(): Painter = painterResource(Res.drawable.ic_transactions)

    @Composable
    fun AccountsIcon(): Painter = painterResource(Res.drawable.ic_wallet)

    @Composable
    fun StatsIcon(): Painter = painterResource(Res.drawable.ic_stats)

    // Bottom nav — filled (selected state)
    @Composable
    fun TransactionsFilledIcon(): Painter = painterResource(Res.drawable.ic_transactions_filled)

    @Composable
    fun AccountsFilledIcon(): Painter = painterResource(Res.drawable.ic_wallet_filled)

    @Composable
    fun StatsFilledIcon(): Painter = painterResource(Res.drawable.ic_stats_filled)

    // Toolbar / general
    @Composable
    fun SettingsIcon(): Painter = painterResource(Res.drawable.ic_settings)

    @Composable
    fun RefreshIcon(): Painter = painterResource(Res.drawable.ic_refresh)

    @Composable
    fun RecurringIcon(): Painter = painterResource(Res.drawable.ic_recurring)

    @Composable
    fun BackIcon(): Painter = painterResource(Res.drawable.ic_back)

    @Composable
    fun ChevronRightIcon(): Painter = painterResource(Res.drawable.ic_chevron_right)

    @Composable
    fun ChevronLeftIcon(): Painter = painterResource(Res.drawable.ic_chevron_left)

    @Composable
    fun CalendarIcon(): Painter = painterResource(Res.drawable.ic_calendar)

    @Composable
    fun GoalsIcon(): Painter = painterResource(Res.drawable.goals)
}
