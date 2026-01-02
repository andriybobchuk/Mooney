package com.andriybobchuk.mooney.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.add
import mooney.composeapp.generated.resources.goals
import mooney.composeapp.generated.resources.stats
import mooney.composeapp.generated.resources.transactions
import mooney.composeapp.generated.resources.wallet
import org.jetbrains.compose.resources.painterResource

object Icons {
    @Composable
    fun AddIcon(): Painter = painterResource(Res.drawable.add)

    @Composable
    fun TransactionsIcon(): Painter = painterResource(Res.drawable.transactions)

    @Composable
    fun AccountsIcon(): Painter = painterResource(Res.drawable.wallet)

    @Composable
    fun StatsIcon(): Painter = painterResource(Res.drawable.stats)

    @Composable
    fun GoalsIcon(): Painter = painterResource(Res.drawable.goals)

    @Composable
    fun ExchangeIcon(): Painter = painterResource(Res.drawable.stats) // Temporarily use stats icon
}
