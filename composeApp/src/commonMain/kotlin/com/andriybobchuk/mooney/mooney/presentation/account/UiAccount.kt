package com.andriybobchuk.mooney.mooney.presentation.account

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion

typealias UiAccount = AccountWithConversion

fun List<UiAccount>.toAccounts(): List<Account> = map { it.toAccount() }
